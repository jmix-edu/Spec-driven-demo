package com.company.booking.service;

import com.company.booking.entity.Booking;
import com.company.booking.entity.BookingStatus;
import com.company.booking.entity.Desk;
import com.company.booking.entity.Room;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.core.FetchPlan;
import io.jmix.core.Messages;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Booking rules checked before a booking is saved:
 * R1 - end is after start; R2 - exactly one of room / desk;
 * R3 - no other non-cancelled booking of the same place overlaps [start, end).
 */
@Service
public class BookingValidationService {

    private static final String MESSAGE_GROUP = "com.company.booking.service";

    private final DataManager dataManager;
    private final EntityStates entityStates;
    private final Messages messages;
    private final DatatypeFormatter datatypeFormatter;

    public BookingValidationService(final DataManager dataManager,
                                    final EntityStates entityStates,
                                    final Messages messages,
                                    final DatatypeFormatter datatypeFormatter) {
        this.dataManager = dataManager;
        this.entityStates = entityStates;
        this.messages = messages;
        this.datatypeFormatter = datatypeFormatter;
    }

    /**
     * Returns the violated rules as localized messages; an empty list means the booking is valid.
     */
    public List<String> validate(final Booking booking) {
        List<String> errors = new ArrayList<>();

        OffsetDateTime start = booking.getStartTime();
        OffsetDateTime end = booking.getEndTime();
        boolean intervalValid = start != null && end != null && end.isAfter(start);
        if (start != null && end != null && !intervalValid) {
            errors.add(messages.getMessage(MESSAGE_GROUP, "bookingValidation.endBeforeStart"));
        }

        Booking place = withPlace(booking);
        Room room = place.getRoom();
        Desk desk = place.getDesk();
        if (room == null && desk == null) {
            errors.add(messages.getMessage(MESSAGE_GROUP, "bookingValidation.placeMissing"));
            return errors;
        }
        if (room != null && desk != null) {
            errors.add(messages.getMessage(MESSAGE_GROUP, "bookingValidation.placeBoth"));
            return errors;
        }

        if (intervalValid && booking.getStatus() != BookingStatus.CANCELLED) {
            if (room != null) {
                findConflict(booking, "room", room).ifPresent(conflict ->
                        errors.add(conflictMessage("bookingValidation.roomTaken", room.getName(), conflict)));
            } else {
                findConflict(booking, "desk", desk).ifPresent(conflict ->
                        errors.add(conflictMessage("bookingValidation.deskTaken", desk.getCode(), conflict)));
            }
        }
        return errors;
    }

    /**
     * Returns a booking whose room and desk can be read: the given one, or - when a reference is not
     * loaded (e.g. inside EntitySavingEvent) - a copy loaded from the database. Only the references
     * are read from the copy.
     */
    private Booking withPlace(final Booking booking) {
        if (entityStates.isLoaded(booking, "room") && entityStates.isLoaded(booking, "desk")) {
            return booking;
        }
        return dataManager.load(Booking.class)
                .id(booking.getId())
                .fetchPlan(fp -> fp.add("room", FetchPlan.INSTANCE_NAME)
                        .add("desk", FetchPlan.INSTANCE_NAME))
                .optional()
                .orElse(booking);
    }

    private Optional<Booking> findConflict(final Booking booking, final String placeProperty, final Object place) {
        // unconstrained: a row-level role ("own bookings") must not hide colleagues' bookings from the check
        return dataManager.unconstrained().load(Booking.class)
                .query("select e from Booking e where e." + placeProperty + " = :place"
                        + " and e.status <> :cancelled"
                        + " and e.startTime < :end and e.endTime > :start"
                        + " and e.id <> :id"
                        + " order by e.startTime")
                .parameter("place", place)
                .parameter("cancelled", BookingStatus.CANCELLED.getId())
                .parameter("start", booking.getStartTime())
                .parameter("end", booking.getEndTime())
                .parameter("id", booking.getId())
                .maxResults(1)
                .list()
                .stream()
                .findFirst();
    }

    private String conflictMessage(final String key, final String placeName, final Booking conflict) {
        return messages.formatMessage(MESSAGE_GROUP, key, placeName,
                datatypeFormatter.formatOffsetDateTime(conflict.getStartTime()),
                datatypeFormatter.formatOffsetDateTime(conflict.getEndTime()));
    }
}

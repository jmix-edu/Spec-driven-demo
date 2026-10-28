package com.company.booking.service;

import com.company.booking.entity.Booking;
import com.company.booking.entity.BookingStatus;
import com.company.booking.entity.Building;
import com.company.booking.entity.Desk;
import com.company.booking.entity.Floor;
import com.company.booking.entity.Room;
import com.company.booking.entity.User;
import com.company.booking.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Booking rules R1-R3 (spec 01_foundations/03), checked through DataManager.save(),
 * i.e. through BookingSavingListener.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
@ActiveProfiles("test")
public class BookingValidationTest {

    private static final OffsetDateTime DAY = OffsetDateTime.of(2030, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    DataManager dataManager;

    @Autowired
    BookingValidationService bookingValidationService;

    private final List<UUID> bookingIds = new ArrayList<>();
    private Building building;
    private Room room;
    private Room otherRoom;
    private Desk desk;
    private User user;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        user = dataManager.load(User.class)
                .query("select u from User u where u.username = :username")
                .parameter("username", "admin")
                .one();

        building = dataManager.create(Building.class);
        building.setName("Test building " + suffix);
        building = dataManager.save(building);

        Floor floor = dataManager.create(Floor.class);
        floor.setBuilding(building);
        floor.setNumber(1);
        floor.setName("First");
        floor = dataManager.save(floor);

        room = saveRoom(floor, "Room A");
        otherRoom = saveRoom(floor, "Room B");

        desk = dataManager.create(Desk.class);
        desk.setFloor(floor);
        desk.setCode("D-1");
        desk = dataManager.save(desk);
    }

    @AfterEach
    void tearDown() {
        bookingIds.forEach(id -> dataManager.load(Id.of(id, Booking.class)).optional().ifPresent(dataManager::remove));
        bookingIds.clear();
        // floors, rooms and desks go with the building (FK on delete cascade)
        dataManager.load(Id.of(building)).optional().ifPresent(dataManager::remove);
    }

    @Test
    void endBeforeStart_rejected() {
        assertRejected(roomBooking(room, 11, 10), "End must be after start.");
    }

    @Test
    void endEqualToStart_rejected() {
        assertRejected(roomBooking(room, 10, 10), "End must be after start.");
    }

    @Test
    void neitherRoomNorDesk_rejected() {
        assertRejected(booking(null, null, 10, 11), "Choose a room or a desk.");
    }

    @Test
    void bothRoomAndDesk_rejected() {
        assertRejected(booking(room, desk, 10, 11), "Choose either a room or a desk, not both.");
    }

    @Test
    void overlappingSameRoom_rejected() {
        save(roomBooking(room, 10, 11));

        assertRejected(roomBooking(room, 10, 12), "Room A is already booked");
    }

    @Test
    void backToBack_saved() {
        save(roomBooking(room, 10, 11));

        save(roomBooking(room, 11, 12));
    }

    @Test
    void overlappingCancelled_saved() {
        Booking cancelled = roomBooking(room, 10, 11);
        cancelled.setStatus(BookingStatus.CANCELLED);
        save(cancelled);

        save(roomBooking(room, 10, 11));
    }

    @Test
    void overlappingCompleted_rejected() {
        Booking completed = roomBooking(room, 10, 11);
        completed.setStatus(BookingStatus.COMPLETED);
        save(completed);

        assertRejected(roomBooking(room, 10, 11), "Room A is already booked");
    }

    @Test
    void sameTimeOtherPlace_saved() {
        save(roomBooking(room, 10, 11));

        save(roomBooking(otherRoom, 10, 11));
        save(booking(null, desk, 10, 11));
    }

    @Test
    void overlappingSameDesk_rejected() {
        save(booking(null, desk, 10, 11));

        assertRejected(booking(null, desk, 9, 12), "Desk D-1 is already booked");
    }

    @Test
    void resaveExisting_saved() {
        Booking saved = save(roomBooking(room, 10, 11));

        Booking loaded = dataManager.load(Booking.class).id(saved.getId()).one();
        loaded.setPurpose("Planning");
        dataManager.save(loaded);

        assertThat(dataManager.load(Booking.class).id(saved.getId()).one().getPurpose()).isEqualTo("Planning");
    }

    @Test
    void cancellingOverlappingBooking_saved() {
        save(roomBooking(room, 10, 11));
        Booking cancelled = roomBooking(room, 10, 11);
        cancelled.setStatus(BookingStatus.CANCELLED);

        save(cancelled);
    }

    @Test
    void validate_reportsAllViolationsAndSavesNothing() {
        Booking booking = booking(room, desk, 11, 10);

        List<String> errors = bookingValidationService.validate(booking);

        assertThat(errors).containsExactly(
                "End must be after start.",
                "Choose either a room or a desk, not both.");
        assertThat(dataManager.load(Id.of(booking)).optional()).isEmpty();
    }

    private Room saveRoom(final Floor floor, final String name) {
        Room newRoom = dataManager.create(Room.class);
        newRoom.setFloor(floor);
        newRoom.setName(name);
        newRoom.setCapacity(4);
        return dataManager.save(newRoom);
    }

    private Booking roomBooking(final Room bookedRoom, final int startHour, final int endHour) {
        return booking(bookedRoom, null, startHour, endHour);
    }

    private Booking booking(final Room bookedRoom, final Desk bookedDesk, final int startHour, final int endHour) {
        Booking booking = dataManager.create(Booking.class);
        booking.setUser(user);
        booking.setRoom(bookedRoom);
        booking.setDesk(bookedDesk);
        booking.setStartTime(DAY.plusHours(startHour));
        booking.setEndTime(DAY.plusHours(endHour));
        bookingIds.add(booking.getId());
        return booking;
    }

    private Booking save(final Booking booking) {
        Booking saved = dataManager.save(booking);
        assertThat(dataManager.load(Id.of(saved)).optional()).isPresent();
        return saved;
    }

    private void assertRejected(final Booking booking, final String expectedMessage) {
        Throwable thrown = catchThrowable(() -> dataManager.save(booking));

        BookingValidationException validationException = findValidationException(thrown);
        assertThat(validationException)
                .as("save must be rejected with BookingValidationException, got: %s", thrown)
                .isNotNull();
        assertThat(validationException.getMessages()).anySatisfy(message ->
                assertThat(message).contains(expectedMessage));
        assertThat(dataManager.load(Id.of(booking)).optional()).isEmpty();
    }

    private BookingValidationException findValidationException(final Throwable thrown) {
        for (Throwable t = thrown; t != null; t = t.getCause()) {
            if (t instanceof BookingValidationException validationException) {
                return validationException;
            }
        }
        return null;
    }
}

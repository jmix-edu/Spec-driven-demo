package com.company.booking.view.booking;

import com.company.booking.entity.Booking;
import com.company.booking.entity.Floor;
import com.company.booking.entity.User;
import com.company.booking.service.BookingValidationService;
import com.company.booking.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;


@Route(value = "bookings/:id", layout = MainView.class)
@ViewController(id = "Booking.detail")
@ViewDescriptor(path = "booking-detail-view.xml")
@EditedEntityContainer("bookingDc")
public class BookingDetailView extends StandardDetailView<Booking> {

    @Autowired
    private BookingValidationService bookingValidationService;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private Downloader downloader;

    @ViewComponent
    private InstanceContainer<Floor> floorPlanDc;
    @ViewComponent
    private JmixImage<?> floorPlanImage;
    @ViewComponent
    private Span noFloorPlanText;
    @ViewComponent
    private JmixButton downloadPlanButton;

    @Subscribe
    public void onInitEntity(final InitEntityEvent<Booking> event) {
        Booking booking = event.getEntity();
        if (booking.getUser() == null && currentAuthentication.getUser() instanceof User user) {
            booking.setUser(user);
        }
        if (booking.getStartTime() == null && booking.getEndTime() == null) {
            // next full hour in the user's time zone, one hour long
            OffsetDateTime start = ZonedDateTime.now(currentAuthentication.getTimeZone().toZoneId())
                    .truncatedTo(ChronoUnit.HOURS)
                    .plusHours(1)
                    .toOffsetDateTime();
            booking.setStartTime(start);
            booking.setEndTime(start.plusHours(1));
        }
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        updateFloorPlan();
    }

    @Subscribe(id = "bookingDc", target = Target.DATA_CONTAINER)
    public void onBookingDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<Booking> event) {
        if ("room".equals(event.getProperty()) || "desk".equals(event.getProperty())) {
            updateFloorPlan();
        }
    }

    @Subscribe("downloadPlanButton")
    public void onDownloadPlanButtonClick(final ClickEvent<JmixButton> event) {
        Floor floor = floorPlanDc.getItemOrNull();
        if (floor != null && floor.getPlan() != null) {
            downloader.download(floor.getPlan());
        }
    }

    @Subscribe
    public void onValidation(final ValidationEvent event) {
        bookingValidationService.validate(getEditedEntity()).forEach(event.getErrors()::add);
    }

    /**
     * Shows the plan of the selected room's or desk's floor. A place picked in the lookup carries only its
     * instance name, so the floor and its plan are always loaded by a query.
     */
    private void updateFloorPlan() {
        Booking booking = getEditedEntity();
        Floor floor = null;
        if (booking.getRoom() != null) {
            floor = findFloor("Room", booking.getRoom().getId());
        } else if (booking.getDesk() != null) {
            floor = findFloor("Desk", booking.getDesk().getId());
        }
        floorPlanDc.setItem(floor);

        boolean hasPlan = floor != null && floor.getPlan() != null;
        floorPlanImage.setVisible(hasPlan);
        noFloorPlanText.setVisible(!hasPlan);
        downloadPlanButton.setEnabled(hasPlan);
    }

    @Nullable
    private Floor findFloor(final String placeEntityName, final UUID placeId) {
        return dataManager.load(Floor.class)
                .query("select e from Floor e where e.id = "
                        + "(select p.floor.id from " + placeEntityName + " p where p.id = :placeId)")
                .parameter("placeId", placeId)
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.INSTANCE_NAME).add("plan"))
                .optional()
                .orElse(null);
    }
}

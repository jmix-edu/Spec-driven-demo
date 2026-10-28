package com.company.booking.security;

import com.company.booking.entity.Booking;
import com.company.booking.entity.Building;
import com.company.booking.entity.Desk;
import com.company.booking.entity.Floor;
import com.company.booking.entity.Room;
import com.company.booking.entity.User;
import com.company.booking.service.BookingValidationException;
import com.company.booking.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Roles from spec 02_security/01: each operation runs as a real user holding real role assignments.
 * Setup and assertions on the persisted state run as admin.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
@ActiveProfiles("test")
public class SecurityRolesTest {

    private static final OffsetDateTime DAY = OffsetDateTime.of(2031, 3, 10, 0, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    DataManager dataManager;

    @Autowired
    SystemAuthenticator systemAuthenticator;

    @Autowired
    PasswordEncoder passwordEncoder;

    private final List<UUID> bookingIds = new ArrayList<>();
    private final List<UUID> buildingIds = new ArrayList<>();
    private final List<UUID> roleAssignmentIds = new ArrayList<>();
    private final List<UUID> userIds = new ArrayList<>();

    private User employee;
    private User colleague;
    private User manager;
    private Building building;
    private Room room;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        employee = user("emp-" + suffix, EmployeeRole.CODE, OwnBookingsRole.CODE);
        colleague = user("col-" + suffix, EmployeeRole.CODE, OwnBookingsRole.CODE);
        manager = user("mgr-" + suffix, OfficeManagerRole.CODE, null);

        building = dataManager.create(Building.class);
        building.setName("Security test " + suffix);
        building = dataManager.save(building);
        buildingIds.add(building.getId());

        Floor floor = dataManager.create(Floor.class);
        floor.setBuilding(building);
        floor.setNumber(1);
        floor.setName("First");
        floor = dataManager.save(floor);

        room = dataManager.create(Room.class);
        room.setFloor(floor);
        room.setName("Room S");
        room.setCapacity(6);
        room = dataManager.save(room);
    }

    @AfterEach
    void tearDown() {
        // FK-safe order: bookings, then buildings (floors/rooms/desks cascade), then role assignments and users
        bookingIds.forEach(id -> dataManager.load(Id.of(id, Booking.class)).optional().ifPresent(dataManager::remove));
        buildingIds.forEach(id -> dataManager.load(Id.of(id, Building.class)).optional().ifPresent(dataManager::remove));
        roleAssignmentIds.forEach(id ->
                dataManager.load(Id.of(id, RoleAssignmentEntity.class)).optional().ifPresent(dataManager::remove));
        userIds.forEach(id -> dataManager.load(Id.of(id, User.class)).optional().ifPresent(dataManager::remove));
        bookingIds.clear();
        buildingIds.clear();
        roleAssignmentIds.clear();
        userIds.clear();
    }

    // --- Employee ---

    @Test
    void employee_seesOnlyOwnBookings() {
        Booking own = dataManager.save(booking(employee, 9, 10));
        dataManager.save(booking(colleague, 10, 11));

        List<Booking> visible = as(employee, () -> loadBookingsOfTestBuilding());

        assertThat(visible).extracting(Booking::getId).containsExactly(own.getId());
    }

    @Test
    void employee_createsOwnBooking() {
        Booking booking = booking(employee, 9, 10);

        as(employee, () -> dataManager.save(booking));

        assertThat(dataManager.load(Id.of(booking)).optional()).isPresent();
    }

    @Test
    void employee_cannotCreateBookingForColleague() {
        Booking booking = booking(colleague, 9, 10);

        Throwable thrown = catchThrowable(() -> as(employee, () -> dataManager.save(booking)));

        assertThat(thrown).isNotNull();
        assertThat(dataManager.load(Id.of(booking)).optional()).isEmpty();
    }

    @Test
    void employee_cannotUpdateOrDeleteColleaguesBooking() {
        Booking colleagues = dataManager.save(booking(colleague, 9, 10));
        Booking loadedByAdmin = dataManager.load(Booking.class).id(colleagues.getId()).one();
        loadedByAdmin.setPurpose("Hijacked");

        Throwable updateThrown = catchThrowable(() -> as(employee, () -> dataManager.save(loadedByAdmin)));
        Throwable deleteThrown = catchThrowable(() -> as(employee, () -> {
            dataManager.remove(dataManager.load(Booking.class).id(colleagues.getId()).one());
            return null;
        }));

        assertThat(updateThrown).isNotNull();
        assertThat(deleteThrown).isNotNull();
        Booking reloaded = dataManager.load(Booking.class).id(colleagues.getId()).one();
        assertThat(reloaded.getPurpose()).isNull();
    }

    @Test
    void employee_updatesAndDeletesOwnBooking() {
        Booking own = dataManager.save(booking(employee, 9, 10));

        as(employee, () -> {
            Booking loaded = dataManager.load(Booking.class).id(own.getId()).one();
            loaded.setPurpose("Standup");
            return dataManager.save(loaded);
        });
        assertThat(dataManager.load(Booking.class).id(own.getId()).one().getPurpose()).isEqualTo("Standup");

        as(employee, () -> {
            dataManager.remove(dataManager.load(Booking.class).id(own.getId()).one());
            return null;
        });
        assertThat(dataManager.load(Id.of(own)).optional()).isEmpty();
    }

    @Test
    void employee_cannotCreateBuilding() {
        Building newBuilding = dataManager.create(Building.class);
        newBuilding.setName("Not allowed " + UUID.randomUUID());
        buildingIds.add(newBuilding.getId());

        Throwable thrown = catchThrowable(() -> as(employee, () -> dataManager.save(newBuilding)));

        assertThat(thrown).isNotNull();
        assertThat(dataManager.load(Id.of(newBuilding)).optional()).isEmpty();
    }

    @Test
    void employee_collisionWithColleaguesBookingIsDetected() {
        dataManager.save(booking(colleague, 10, 11));
        Booking overlapping = booking(employee, 10, 12);

        Throwable thrown = catchThrowable(() -> as(employee, () -> dataManager.save(overlapping)));

        BookingValidationException validationException = findValidationException(thrown);
        assertThat(validationException)
                .as("expected the slot-conflict rejection, got: %s", thrown)
                .isNotNull();
        assertThat(validationException.getMessages()).anySatisfy(message ->
                assertThat(message).contains("Room S is already booked"));
        assertThat(dataManager.load(Id.of(overlapping)).optional()).isEmpty();
    }

    // --- Office Manager ---

    @Test
    void manager_managesReferenceData() {
        Building created = as(manager, () -> {
            Building newBuilding = dataManager.create(Building.class);
            newBuilding.setName("Managed " + UUID.randomUUID());
            Floor floor = dataManager.create(Floor.class);
            floor.setBuilding(newBuilding);
            floor.setNumber(3);
            floor.setName("Third");
            Room newRoom = dataManager.create(Room.class);
            newRoom.setFloor(floor);
            newRoom.setName("Room M");
            newRoom.setCapacity(10);
            Desk desk = dataManager.create(Desk.class);
            desk.setFloor(floor);
            desk.setCode("3-01");
            dataManager.saveWithoutReload(newBuilding, floor, newRoom, desk);
            return newBuilding;
        });
        buildingIds.add(created.getId());
        assertThat(dataManager.loadValue("select count(e) from Desk e where e.floor.building.id = :id", Long.class)
                .parameter("id", created.getId()).one()).isEqualTo(1L);

        as(manager, () -> {
            dataManager.remove(dataManager.load(Building.class).id(created.getId()).one());
            return null;
        });
        assertThat(dataManager.load(Id.of(created)).optional()).isEmpty();
    }

    @Test
    void manager_seesAllBookings() {
        Booking first = dataManager.save(booking(employee, 9, 10));
        Booking second = dataManager.save(booking(colleague, 10, 11));

        List<Booking> visible = as(manager, () -> loadBookingsOfTestBuilding());

        assertThat(visible).extracting(Booking::getId).containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    void manager_cannotCreateOrUpdateBookings() {
        Booking newBooking = booking(manager, 9, 10);
        Booking existing = dataManager.save(booking(employee, 11, 12));

        Throwable createThrown = catchThrowable(() -> as(manager, () -> dataManager.save(newBooking)));
        Throwable updateThrown = catchThrowable(() -> as(manager, () -> {
            Booking loaded = dataManager.load(Booking.class).id(existing.getId()).one();
            loaded.setPurpose("Changed by manager");
            return dataManager.save(loaded);
        }));

        assertThat(createThrown).isNotNull();
        assertThat(updateThrown).isNotNull();
        assertThat(dataManager.load(Id.of(newBooking)).optional()).isEmpty();
        assertThat(dataManager.load(Booking.class).id(existing.getId()).one().getPurpose()).isNull();
    }

    // --- helpers ---

    private User user(final String username, final String resourceRole, final String rowLevelRole) {
        User user = dataManager.create(User.class);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(username));
        user = dataManager.save(user);
        userIds.add(user.getId());
        roleAssignment(username, resourceRole, RoleAssignmentRoleType.RESOURCE);
        if (rowLevelRole != null) {
            roleAssignment(username, rowLevelRole, RoleAssignmentRoleType.ROW_LEVEL);
        }
        return user;
    }

    private void roleAssignment(final String username, final String roleCode, final String roleType) {
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(username);
        assignment.setRoleCode(roleCode);
        assignment.setRoleType(roleType);
        roleAssignmentIds.add(dataManager.save(assignment).getId());
    }

    private Booking booking(final User bookingUser, final int startHour, final int endHour) {
        Booking booking = dataManager.create(Booking.class);
        booking.setUser(bookingUser);
        booking.setRoom(room);
        booking.setStartTime(DAY.plusHours(startHour));
        booking.setEndTime(DAY.plusHours(endHour));
        bookingIds.add(booking.getId());
        return booking;
    }

    private List<Booking> loadBookingsOfTestBuilding() {
        return dataManager.load(Booking.class)
                .query("select e from Booking e where e.room.floor.building.id = :buildingId")
                .parameter("buildingId", building.getId())
                .list();
    }

    private <T> T as(final User user, final SystemAuthenticator.AuthenticatedOperation<T> operation) {
        return systemAuthenticator.withUser(user.getUsername(), operation);
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

package com.company.booking.security;

import com.company.booking.entity.Booking;
import com.company.booking.entity.Building;
import com.company.booking.entity.Floor;
import com.company.booking.entity.Room;
import com.company.booking.entity.User;
import com.company.booking.test_support.AuthenticatedAsAdmin;
import io.jmix.aitools.dataload.execution.JpqlExecutionRequest;
import io.jmix.aitools.dataload.execution.JpqlExecutionResult;
import io.jmix.aitools.dataload.execution.JpqlExecutionService;
import io.jmix.aitools.dataload.validation.JpqlValidationIssue;
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

/**
 * Spec 05_addons/01: the AI chat answers from data with the chatting user's rights.
 * Calls the add-on's query executor - the step that runs the model's generated JPQL - directly, as a real user;
 * no model is involved.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
@ActiveProfiles("test")
public class AiChatDataAccessTest {

    private static final OffsetDateTime DAY = OffsetDateTime.of(2032, 5, 20, 0, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    DataManager dataManager;

    @Autowired
    SystemAuthenticator systemAuthenticator;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JpqlExecutionService jpqlExecutionService;

    private final List<UUID> bookingIds = new ArrayList<>();
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
        employee = user("ai-emp-" + suffix, EmployeeRole.CODE, OwnBookingsRole.CODE);
        colleague = user("ai-col-" + suffix, EmployeeRole.CODE, OwnBookingsRole.CODE);
        manager = user("ai-mgr-" + suffix, OfficeManagerRole.CODE, null);

        building = dataManager.create(Building.class);
        building.setName("AI test " + suffix);
        building = dataManager.save(building);

        Floor floor = dataManager.create(Floor.class);
        floor.setBuilding(building);
        floor.setNumber(1);
        floor.setName("First");
        floor = dataManager.save(floor);

        room = dataManager.create(Room.class);
        room.setFloor(floor);
        room.setName("Room AI");
        room.setCapacity(4);
        room = dataManager.save(room);

        booking(employee, 9, 10);
        booking(colleague, 10, 11);
    }

    @AfterEach
    void tearDown() {
        // FK-safe order: bookings, building (floors/rooms cascade), role assignments, users
        bookingIds.forEach(id -> dataManager.load(Id.of(id, Booking.class)).optional().ifPresent(dataManager::remove));
        dataManager.load(Id.of(building)).optional().ifPresent(dataManager::remove);
        roleAssignmentIds.forEach(id ->
                dataManager.load(Id.of(id, RoleAssignmentEntity.class)).optional().ifPresent(dataManager::remove));
        userIds.forEach(id -> dataManager.load(Id.of(id, User.class)).optional().ifPresent(dataManager::remove));
        bookingIds.clear();
        roleAssignmentIds.clear();
        userIds.clear();
    }

    @Test
    void employee_getsOnlyOwnBookings() {
        JpqlExecutionResult result = as(employee, () -> jpqlExecutionService.execute(bookingsOfTestBuilding()));

        assertThat(result.isExecuted())
                .as("validation: %s, execution error: %s, generated: %s", result.getValidationResult(),
                        result.getExecutionError(), result.getGeneratedJpqlResult())
                .isTrue();
        assertThat(result.getRows()).hasSize(1);
    }

    @Test
    void manager_getsAllBookings() {
        JpqlExecutionResult result = as(manager, () -> jpqlExecutionService.execute(bookingsOfTestBuilding()));

        assertThat(result.isExecuted())
                .as("validation: %s, execution error: %s, generated: %s", result.getValidationResult(),
                        result.getExecutionError(), result.getGeneratedJpqlResult())
                .isTrue();
        assertThat(result.getRows()).hasSize(2);
    }

    @Test
    void users_areNotAvailable() {
        // same shape as the booking queries, which do run - so only the excluded entity can reject it
        JpqlExecutionRequest request = new JpqlExecutionRequest("List all users",
                "select u.username from User u", List.of(), List.of("username"), 20, 0);

        JpqlExecutionResult result = as(manager, () -> jpqlExecutionService.execute(request));

        assertThat(result.isExecuted()).isFalse();
        assertThat(result.getRows()).isNullOrEmpty();
        assertThat(result.getValidationResult().getIssues())
                .as("rejected for the excluded entity, not for a malformed request")
                .extracting(JpqlValidationIssue::getCode)
                .isNotEmpty()
                .doesNotContain("resultProperties.empty");
    }

    private JpqlExecutionRequest bookingsOfTestBuilding() {
        return new JpqlExecutionRequest("Show all bookings",
                // an attribute both personas may view: the add-on drops result columns the user has no attribute access to
                "select e.startTime from Booking e where e.room.floor.building.name = '" + building.getName() + "'",
                List.of(), List.of("startTime"), 20, 0);
    }

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

    private void booking(final User bookingUser, final int startHour, final int endHour) {
        Booking booking = dataManager.create(Booking.class);
        booking.setUser(bookingUser);
        booking.setRoom(room);
        booking.setStartTime(DAY.plusHours(startHour));
        booking.setEndTime(DAY.plusHours(endHour));
        bookingIds.add(dataManager.save(booking).getId());
    }

    private <T> T as(final User user, final SystemAuthenticator.AuthenticatedOperation<T> operation) {
        return systemAuthenticator.withUser(user.getUsername(), operation);
    }
}

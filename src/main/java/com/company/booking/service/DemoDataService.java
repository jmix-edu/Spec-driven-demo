package com.company.booking.service;

import com.company.booking.entity.Building;
import com.company.booking.entity.Desk;
import com.company.booking.entity.Floor;
import com.company.booking.entity.Room;
import com.company.booking.entity.User;
import com.company.booking.security.EmployeeRole;
import com.company.booking.security.OfficeManagerRole;
import com.company.booking.security.OwnBookingsRole;
import io.jmix.core.DataManager;
import io.jmix.core.security.Authenticated;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds demo data into an empty database: one building with floors, rooms and desks,
 * and demo users for the Employee and Office Manager personas.
 */
@Service
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    private static final List<String> APPLICATION_ENTITIES =
            List.of("Building", "Floor", "Room", "Desk", "Amenity", "Booking");

    private final DataManager dataManager;
    private final PasswordEncoder passwordEncoder;

    public DemoDataService(final DataManager dataManager, final PasswordEncoder passwordEncoder) {
        this.dataManager = dataManager;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates the demo data if the database is empty, i.e. holds only the admin user and nothing else.
     * Runs under the system user, so it works on the startup thread, which has no authenticated user.
     *
     * @return true if the data was created, false if the database was not empty
     */
    @Authenticated
    public boolean seedIfEmpty() {
        if (!isEmpty()) {
            log.info("Demo data skipped: the database is not empty");
            return false;
        }

        List<Object> entities = new ArrayList<>();
        Building building = dataManager.create(Building.class);
        building.setName("Head Office");
        building.setAddress("1 Main Street");
        entities.add(building);

        entities.addAll(floor(building, 1, "1st floor",
                new String[]{"Aurora", "Borealis"}, new int[]{8, 4}));
        entities.addAll(floor(building, 2, "2nd floor",
                new String[]{"Cosmos", "Delta"}, new int[]{12, 6}));

        // demo credentials: password = username, only ever created on an empty database
        entities.add(user("emma", "Emma", "Brown"));
        entities.add(roleAssignment("emma", EmployeeRole.CODE, RoleAssignmentRoleType.RESOURCE));
        entities.add(roleAssignment("emma", OwnBookingsRole.CODE, RoleAssignmentRoleType.ROW_LEVEL));
        entities.add(user("oliver", "Oliver", "Smith"));
        entities.add(roleAssignment("oliver", EmployeeRole.CODE, RoleAssignmentRoleType.RESOURCE));
        entities.add(roleAssignment("oliver", OwnBookingsRole.CODE, RoleAssignmentRoleType.ROW_LEVEL));
        entities.add(user("maria", "Maria", "Garcia"));
        entities.add(roleAssignment("maria", OfficeManagerRole.CODE, RoleAssignmentRoleType.RESOURCE));

        // one save context: all or nothing
        dataManager.saveWithoutReload(entities.toArray());
        log.info("Demo data created: building '{}' with 2 floors, 4 rooms and 8 desks; users emma, oliver, maria",
                building.getName());
        return true;
    }

    private boolean isEmpty() {
        if (count("select count(e) from User e") != 1
                || count("select count(e) from User e where e.username = 'admin'") != 1) {
            return false;
        }
        for (String entityName : APPLICATION_ENTITIES) {
            if (count("select count(e) from " + entityName + " e") > 0) {
                return false;
            }
        }
        return true;
    }

    private long count(final String query) {
        return dataManager.loadValue(query, Long.class).one();
    }

    private User user(final String username, final String firstName, final String lastName) {
        User user = dataManager.create(User.class);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(username));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }

    private RoleAssignmentEntity roleAssignment(final String username, final String roleCode, final String roleType) {
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(username);
        assignment.setRoleCode(roleCode);
        assignment.setRoleType(roleType);
        return assignment;
    }

    private List<Object> floor(final Building building, final int number, final String name,
                               final String[] roomNames, final int[] capacities) {
        List<Object> entities = new ArrayList<>();
        Floor floor = dataManager.create(Floor.class);
        floor.setBuilding(building);
        floor.setNumber(number);
        floor.setName(name);
        entities.add(floor);

        for (int i = 0; i < roomNames.length; i++) {
            Room room = dataManager.create(Room.class);
            room.setFloor(floor);
            room.setName(roomNames[i]);
            room.setCapacity(capacities[i]);
            entities.add(room);
        }
        for (int i = 1; i <= 4; i++) {
            Desk desk = dataManager.create(Desk.class);
            desk.setFloor(floor);
            desk.setCode("%d-%02d".formatted(number, i));
            entities.add(desk);
        }
        return entities;
    }
}

package com.company.booking.security;

import com.company.booking.entity.Amenity;
import com.company.booking.entity.Booking;
import com.company.booking.entity.Building;
import com.company.booking.entity.Desk;
import com.company.booking.entity.Floor;
import com.company.booking.entity.Room;
import com.company.booking.entity.User;
import io.jmix.aitoolsflowui.security.role.AiToolsChatUserRole;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * Office Manager: manages the space (buildings, floors, rooms, desks, amenities), sees all bookings read-only.
 */
@ResourceRole(name = "Office Manager", code = OfficeManagerRole.CODE)
public interface OfficeManagerRole extends UiMinimalRole, AiToolsChatUserRole {

    String CODE = "booking-office-manager";

    @EntityAttributePolicy(entityClass = Building.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Building.class, actions = EntityPolicyAction.ALL)
    @EntityAttributePolicy(entityClass = Floor.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Floor.class, actions = EntityPolicyAction.ALL)
    @EntityAttributePolicy(entityClass = Room.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Room.class, actions = EntityPolicyAction.ALL)
    @EntityAttributePolicy(entityClass = Desk.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Desk.class, actions = EntityPolicyAction.ALL)
    @EntityAttributePolicy(entityClass = Amenity.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Amenity.class, actions = EntityPolicyAction.ALL)
    void referenceData();

    @EntityAttributePolicy(entityClass = Booking.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Booking.class, actions = EntityPolicyAction.READ)
    void booking();

    // what the user's instance name shows
    @EntityAttributePolicy(entityClass = User.class,
            attributes = {"username", "firstName", "lastName"},
            action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = User.class, actions = EntityPolicyAction.READ)
    void user();

    // Floor/Room/Desk detail views are composition dialogs: view policy, no menu item
    @ViewPolicy(viewIds = {
            "Building.list", "Building.detail", "Floor.detail", "Room.detail", "Desk.detail",
            "Amenity.list", "Amenity.detail",
            "Booking.list", "Booking.detail"})
    @MenuPolicy(menuIds = {"Building.list", "Amenity.list", "Booking.list"})
    void screens();
}

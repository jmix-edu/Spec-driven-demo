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
 * Employee: picks rooms and desks from the reference data and manages bookings.
 * Which bookings - only their own - is restricted by {@link OwnBookingsRole}, assigned together with this role.
 */
@ResourceRole(name = "Employee", code = EmployeeRole.CODE)
public interface EmployeeRole extends UiMinimalRole, AiToolsChatUserRole {

    String CODE = "booking-employee";

    // user is VIEW: prefilled with the current user in the booking form, not editable
    @EntityAttributePolicy(entityClass = Booking.class,
            attributes = {"room", "desk", "startTime", "endTime", "status", "purpose"},
            action = EntityAttributePolicyAction.MODIFY)
    @EntityAttributePolicy(entityClass = Booking.class,
            attributes = "user",
            action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Booking.class, actions = {
            EntityPolicyAction.READ, EntityPolicyAction.CREATE,
            EntityPolicyAction.UPDATE, EntityPolicyAction.DELETE})
    void booking();

    @EntityAttributePolicy(entityClass = Building.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Building.class, actions = EntityPolicyAction.READ)
    @EntityAttributePolicy(entityClass = Floor.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Floor.class, actions = EntityPolicyAction.READ)
    @EntityAttributePolicy(entityClass = Room.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Room.class, actions = EntityPolicyAction.READ)
    @EntityAttributePolicy(entityClass = Desk.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Desk.class, actions = EntityPolicyAction.READ)
    @EntityAttributePolicy(entityClass = Amenity.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Amenity.class, actions = EntityPolicyAction.READ)
    void referenceData();

    // what the user's instance name shows
    @EntityAttributePolicy(entityClass = User.class,
            attributes = {"username", "firstName", "lastName"},
            action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = User.class, actions = EntityPolicyAction.READ)
    void user();

    @ViewPolicy(viewIds = {"Booking.list", "Booking.detail", "Room.lookup", "Desk.lookup"})
    @MenuPolicy(menuIds = "Booking.list")
    void screens();
}

package com.company.booking.security;

import com.company.booking.entity.Booking;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.security.model.RowLevelBiPredicate;
import io.jmix.security.model.RowLevelPolicyAction;
import io.jmix.security.role.annotation.JpqlRowLevelPolicy;
import io.jmix.security.role.annotation.PredicateRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;
import org.springframework.context.ApplicationContext;

/**
 * Restricts bookings to the current user's own: reads are filtered in the database,
 * create/update/delete are allowed only for a booking whose user is the current user.
 * Assign to employees only - row-level policies are joined with AND.
 */
@RowLevelRole(name = "Own bookings only", code = OwnBookingsRole.CODE)
public interface OwnBookingsRole {

    String CODE = "booking-own-bookings";

    @JpqlRowLevelPolicy(entityClass = Booking.class, where = "{E}.user.username = :current_user_username")
    void readOwn();

    // ownership only: it does not change during a save, so the check is stable for UPDATE
    @PredicateRowLevelPolicy(entityClass = Booking.class, actions = {
            RowLevelPolicyAction.CREATE, RowLevelPolicyAction.UPDATE, RowLevelPolicyAction.DELETE})
    default RowLevelBiPredicate<Booking, ApplicationContext> modifyOwn() {
        return (booking, applicationContext) -> booking.getUser() != null
                && booking.getUser().getUsername().equals(
                applicationContext.getBean(CurrentAuthentication.class).getUser().getUsername());
    }
}

package com.company.booking.listener;

import com.company.booking.entity.Floor;
import io.jmix.core.FileRef;
import io.jmix.core.Messages;
import io.jmix.core.event.EntitySavingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Accepts only PNG and JPEG floor plans, whatever path saves the floor. The upload field limits the type in the
 * browser; this is the data-level check. It checks the declared file name, not the file content.
 */
@Component
public class FloorPlanSavingListener {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    private final Messages messages;

    public FloorPlanSavingListener(final Messages messages) {
        this.messages = messages;
    }

    @EventListener
    public void onFloorSaving(final EntitySavingEvent<Floor> event) {
        FileRef plan = event.getEntity().getPlan();
        if (plan != null && !ALLOWED_EXTENSIONS.contains(extension(plan.getFileName()))) {
            throw new InvalidFloorPlanException(
                    messages.getMessage("com.company.booking.listener", "floorPlan.invalidType"));
        }
    }

    private String extension(final String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}

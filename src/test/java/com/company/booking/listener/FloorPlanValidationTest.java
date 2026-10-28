package com.company.booking.listener;

import com.company.booking.entity.Building;
import com.company.booking.entity.Floor;
import com.company.booking.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.FileRef;
import io.jmix.core.Id;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Spec 03_files/01: a floor plan must be a PNG or JPEG, checked on save by FloorPlanSavingListener.
 * The FileRefs are created in memory - no file is stored.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
@ActiveProfiles("test")
public class FloorPlanValidationTest {

    @Autowired
    DataManager dataManager;

    private Building building;
    private int floorNumber;

    @BeforeEach
    void setUp() {
        building = dataManager.create(Building.class);
        building.setName("Plan test " + UUID.randomUUID());
        building = dataManager.save(building);
    }

    @AfterEach
    void tearDown() {
        // floors go with the building (FK on delete cascade)
        dataManager.load(Id.of(building)).optional().ifPresent(dataManager::remove);
    }

    @ParameterizedTest
    @ValueSource(strings = {"plan.png", "PLAN.JPG", "plan.jpeg"})
    void pngAndJpeg_saved(final String fileName) {
        Floor floor = floor(plan(fileName));

        dataManager.save(floor);

        assertThat(dataManager.load(Floor.class).id(floor.getId()).one().getPlan().getFileName()).isEqualTo(fileName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"plan.pdf", "plan", "plan.png.exe"})
    void otherTypes_rejected(final String fileName) {
        Floor floor = floor(plan(fileName));

        Throwable thrown = catchThrowable(() -> dataManager.save(floor));

        assertThat(findCause(thrown, InvalidFloorPlanException.class))
                .as("expected InvalidFloorPlanException, got: %s", thrown)
                .isNotNull()
                .extracting(Throwable::getMessage)
                .isEqualTo("The floor plan must be a PNG or JPEG image.");
        assertThat(dataManager.load(Id.of(floor)).optional()).isEmpty();
    }

    @Test
    void noPlan_saved() {
        Floor floor = floor(null);

        dataManager.save(floor);

        assertThat(dataManager.load(Id.of(floor)).optional()).isPresent();
    }

    private Floor floor(final FileRef plan) {
        Floor floor = dataManager.create(Floor.class);
        floor.setBuilding(building);
        floor.setNumber(++floorNumber);
        floor.setName("Floor " + floorNumber);
        floor.setPlan(plan);
        return floor;
    }

    private FileRef plan(final String fileName) {
        return new FileRef("fs", "2026/10/27/" + UUID.randomUUID(), fileName);
    }

    private <T extends Throwable> T findCause(final Throwable thrown, final Class<T> type) {
        for (Throwable t = thrown; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
        }
        return null;
    }
}

package com.company.booking.entity;

import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

@JmixEntity
@Entity
@Table(name = "BOOKING", indexes = {
        @Index(name = "IDX_BOOKING_USER", columnList = "USER_ID"),
        @Index(name = "IDX_BOOKING_ROOM", columnList = "ROOM_ID"),
        @Index(name = "IDX_BOOKING_DESK", columnList = "DESK_ID"),
        @Index(name = "IDX_BOOKING_START_TIME", columnList = "START_TIME")
})
public class Booking {

    @Id
    @Column(name = "ID", nullable = false)
    @JmixGeneratedValue
    private UUID id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    @OnDeleteInverse(DeletePolicy.DENY)
    @NotNull
    @JoinColumn(name = "USER_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @OnDeleteInverse(DeletePolicy.DENY)
    @JoinColumn(name = "ROOM_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private Room room;

    @OnDeleteInverse(DeletePolicy.DENY)
    @JoinColumn(name = "DESK_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private Desk desk;

    @NotNull
    @Column(name = "START_TIME", nullable = false)
    private OffsetDateTime startTime;

    @NotNull
    @Column(name = "END_TIME", nullable = false)
    private OffsetDateTime endTime;

    @NotNull
    @Column(name = "STATUS", nullable = false, length = 50)
    private String status = BookingStatus.CONFIRMED.getId();

    @Column(name = "PURPOSE")
    private String purpose;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public User getUser() {
        return user;
    }

    public void setUser(final User user) {
        this.user = user;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(final Room room) {
        this.room = room;
    }

    public Desk getDesk() {
        return desk;
    }

    public void setDesk(final Desk desk) {
        this.desk = desk;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(final OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public BookingStatus getStatus() {
        return status == null ? null : BookingStatus.fromId(status);
    }

    public void setStatus(final BookingStatus status) {
        this.status = status == null ? null : status.getId();
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(final String purpose) {
        this.purpose = purpose;
    }

    @InstanceName
    @DependsOnProperties({"room", "desk", "startTime"})
    public String getDisplayName() {
        String target = room != null ? room.getName() : (desk != null ? desk.getCode() : "");
        return String.format("%s %s", target, startTime != null ? startTime : "").trim();
    }
}

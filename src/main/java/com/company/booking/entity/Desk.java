package com.company.booking.entity;

import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@JmixEntity
@Entity
@Table(name = "DESK", indexes = {
        @Index(name = "IDX_DESK_FLOOR", columnList = "FLOOR_ID")
}, uniqueConstraints = {
        @UniqueConstraint(name = "UK_DESK_FLOOR_CODE", columnNames = {"FLOOR_ID", "CODE"})
})
public class Desk {

    @Id
    @Column(name = "ID", nullable = false)
    @JmixGeneratedValue
    private UUID id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @NotNull
    @JoinColumn(name = "FLOOR_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Floor floor;

    @InstanceName
    @NotNull
    @Column(name = "CODE", nullable = false, length = 50)
    private String code;

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

    public Floor getFloor() {
        return floor;
    }

    public void setFloor(final Floor floor) {
        this.floor = floor;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }
}

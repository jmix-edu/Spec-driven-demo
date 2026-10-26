package com.company.booking.entity;

import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@JmixEntity
@Entity
@Table(name = "FLOOR", indexes = {
        @Index(name = "IDX_FLOOR_BUILDING", columnList = "BUILDING_ID")
}, uniqueConstraints = {
        @UniqueConstraint(name = "UK_FLOOR_BUILDING_NUMBER", columnNames = {"BUILDING_ID", "FLOOR_NUMBER"})
})
public class Floor {

    @Id
    @Column(name = "ID", nullable = false)
    @JmixGeneratedValue
    private UUID id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @NotNull
    @JoinColumn(name = "BUILDING_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Building building;

    @NotNull
    @Column(name = "FLOOR_NUMBER", nullable = false)
    private Integer number;

    @InstanceName
    @NotNull
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Composition
    @OneToMany(mappedBy = "floor")
    private List<Room> rooms;

    @Composition
    @OneToMany(mappedBy = "floor")
    private List<Desk> desks;

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

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(final Building building) {
        this.building = building;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(final Integer number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(final List<Room> rooms) {
        this.rooms = rooms;
    }

    public List<Desk> getDesks() {
        return desks;
    }

    public void setDesks(final List<Desk> desks) {
        this.desks = desks;
    }
}

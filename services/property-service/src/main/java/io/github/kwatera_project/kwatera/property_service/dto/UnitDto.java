package io.github.kwatera_project.kwatera.property_service.dto;

import jakarta.persistence.criteria.CriteriaBuilder;

import java.math.BigDecimal;
import java.util.UUID;

public class UnitDto {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer capacity;

    public UnitDto(UUID id, String name, String description, BigDecimal price, Integer capacity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.capacity = capacity;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getCapacity() {
        return capacity;
    }
}

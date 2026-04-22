package io.github.kwatera_project.kwatera.reservation_service.model;

import io.github.kwatera_project.kwatera.reservation_service.dto.PropertyDto;

public class Property {
    private Long id;
    private String name;
    private String location;
    private int capacity;
    private double basePrice;
    private String imageUrl;

    public Property(Long id, String name, String location, int capacity, double basePrice, String imageUrl) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.basePrice = basePrice;
        this.imageUrl = imageUrl;

    }


    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public PropertyDto toDto() {
        return new PropertyDto(id, name, location, basePrice, imageUrl);
    }

}


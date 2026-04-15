package io.github.kwatera_project.kwatera.reservation_service.dto;

public class PropertyDto {

    private Long id;
    private String name;
    private String location;
    private double basePrice;
    private String imageUrl;

    public PropertyDto(Long id, String name, String location, double basePrice, String imageUrl) {
        this.id = id;
        this.name = name;
        this.location = location;
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

    public double getBasePrice() {
        return basePrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}

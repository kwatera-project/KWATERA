package io.github.kwatera_project.kwatera.property_service.dto;

import java.util.UUID;

public class PropertyDto {

  private UUID id;
  private String title;
  private String description;
  private String location;
  private String imageUrl;

  public PropertyDto(UUID id, String title, String description, String location, String imageUrl) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.location = location;
    this.imageUrl = imageUrl;
  }

  public UUID getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getLocation() {
    return location;
  }

  public String getImageUrl() {
    return imageUrl;
  }
}

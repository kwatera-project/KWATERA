package io.github.kwatera_project.kwatera.property_service.dto;

import java.util.UUID;

public class PropertyDto {

  private UUID id;
  private String title;
  private String description;
  private String location;

  public PropertyDto(UUID id, String title, String description, String location) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.location = location;
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
}

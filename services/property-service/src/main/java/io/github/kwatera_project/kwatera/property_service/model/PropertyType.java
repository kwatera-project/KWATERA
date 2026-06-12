package io.github.kwatera_project.kwatera.property_service.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PropertyType {
  APARTMENT("Apartment"),
  HOUSE("House"),
  VILLA("Villa"),
  STUDIO("Studio"),
  ROOM("Room");

  private final String value;

  PropertyType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  public static PropertyType fromValue(String value) {
    for (PropertyType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown PropertyType: " + value);
  }
}

package io.github.kwatera_project.kwatera.property_service.dto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnitImageDto {
  private UUID id;
  private String url;
  private Boolean isMain;

  public UnitImageDto(UUID id, String url, Boolean isMain) {
    this.id = id;
    this.url = url;
    this.isMain = isMain;
  }
}

package io.github.kwatera_project.kwatera.ai_pricing_service.model;

public enum UnitType {
  ENTIRE_RENTAL_UNIT("Entire rental unit"),

  PRIVATE_ROOM_IN_RENTAL_UNIT("Private room in rental unit"),

  PRIVATE_ROOM_IN_HOME("Private room in home"),

  ENTIRE_TOWNHOUSE("Entire townhouse"),

  ENTIRE_HOME("Entire home"),

  ENTIRE_CONDO("Entire condo"),

  ENTIRE_GUEST_SUITE("Entire guest suite"),

  PRIVATE_ROOM_IN_GUEST_SUITE("Private room in guest suite"),

  SHARED_ROOM_IN_HOME("Shared room in home"),

  SHARED_ROOM_IN_HOSTEL("Shared room in hostel"),

  ENTIRE_SERVICED_APARTMENT("Entire serviced apartment"),

  ENTIRE_VILLA("Entire villa"),

  PRIVATE_ROOM_IN_TOWNHOUSE("Private room in townhouse"),

  PRIVATE_ROOM_IN_HOSTEL("Private room in hostel"),

  ROOM_IN_APARTHOTEL("Room in aparthotel"),

  ENTIRE_LOFT("Entire loft"),

  PRIVATE_ROOM("Private room"),

  ROOM_IN_SERVICED_APARTMENT("Room in serviced apartment"),

  PRIVATE_ROOM_IN_CONDO("Private room in condo"),

  SHARED_ROOM_IN_RENTAL_UNIT("Shared room in rental unit"),

  ENTIRE_PLACE("Entire place"),

  ROOM_IN_HOTEL("Room in hotel"),

  ENTIRE_APARTMENT("Entire apartment"),

  ENTIRE_GUESTHOUSE("Entire guesthouse"),

  ROOM_IN_BOUTIQUE_HOTEL("Room in boutique hotel"),

  PRIVATE_ROOM_IN_VILLA("Private room in villa"),

  ENTIRE_CHALET("Entire chalet"),

  ROOM_IN_HOSTEL("Room in hostel"),

  PRIVATE_ROOM_IN_BED_AND_BREAKFAST("Private room in bed and breakfast"),

  ENTIRE_COTTAGE("Entire cottage"),

  TINY_HOME("Tiny home"),

  PRIVATE_ROOM_IN_SERVICED_APARTMENT("Private room in serviced apartment"),

  ENTIRE_BUNGALOW("Entire bungalow"),

  ENTIRE_HOME_APT("Entire home/apt");

  private final String value;

  UnitType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static UnitType fromValue(String value) {
    for (UnitType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown UnitType: " + value);
  }
}

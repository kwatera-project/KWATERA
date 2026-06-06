package io.github.kwatera_project.kwatera.property_service.service;

import io.github.kwatera_project.kwatera.property_service.client.ReservationClient;
import io.github.kwatera_project.kwatera.property_service.dto.*;
import io.github.kwatera_project.kwatera.property_service.model.*;
import io.github.kwatera_project.kwatera.property_service.repository.*;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PropertyService {

  private final PropertyRepository propertyRepository;
  private final UnitRepository unitRepository;
  private final PropertyImageRepository propertyImageRepository;
  private final UnitImageRepository unitImageRepository;
  private final UnitSettlementItemRepository unitSettlementItemRepository;
  private final io.github.kwatera_project.kwatera.property_service.client.NbpExchangeRateClient
      nbpExchangeRateClient;
  private final ReservationClient reservationClient;
  private final GeocodingService geocodingService;

  public List<UnitDto> getUnits(UUID propertyId, String currency) {
    if (!propertyRepository.existsById(propertyId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
    }
    validateCurrency(currency);
    return unitRepository.findByPropertyId(propertyId).stream()
        .map(unit -> mapToDto(unit, currency))
        .toList();
  }

  public List<PropertyDto> getAll() {
    return propertyRepository.findAll().stream().map(this::mapToDto).toList();
  }

  public PropertyDto getById(UUID id) {
    Property property =
        propertyRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));

    return mapToDto(property);
  }

  public UnitDto getUnitById(UUID id, String currency) {
    Unit unit =
        unitRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));

    validateCurrency(currency);
    return mapToDto(unit, currency);
  }

  private void validateCurrency(String currency) {
    if (currency != null && !List.of("PLN", "EUR", "USD").contains(currency.toUpperCase())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported currency");
    }
  }

  public List<UUID> getUnitIdsByOwnerId(UUID ownerId) {
    List<UUID> propertyIds =
        propertyRepository.findByOwnerId(ownerId).stream().map(Property::getId).toList();

    if (propertyIds.isEmpty()) {
      return List.of();
    }

    return unitRepository.findByPropertyIdIn(propertyIds).stream().map(Unit::getId).toList();
  }

  public List<PropertyDto> getPropertiesByOwner(UUID ownerId) {
    return propertyRepository.findByOwnerId(ownerId).stream().map(this::mapToDto).toList();
  }

  public List<UnitDto> getUnitsForOwnerProperty(UUID ownerId, UUID propertyId, String currency) {

    Property property =
        propertyRepository
            .findById(propertyId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));

    if (!property.getOwnerId().equals(ownerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    return unitRepository.findByPropertyId(propertyId).stream()
        .map(unit -> mapToDto(unit, currency))
        .toList();
  }

  public List<UUID> getAllUnitIds() {
    return unitRepository.findAll().stream().map(Unit::getId).toList();
  }

  public List<String> getPropertyImages(UUID propertyId) {
    return propertyImageRepository.findByPropertyId(propertyId).stream()
        .map(PropertyImage::getUrl)
        .toList();
  }

  public List<UnitSettlementItemDto> getUnitSettlementItems(UUID unitId) {

    List<UnitSettlementItem> items = unitSettlementItemRepository.findByUnitId(unitId);

    return items.stream().map(this::mapToDto).toList();
  }

  private PropertyDto mapToDto(Property property) {
    String imageUrl =
        propertyImageRepository
            .findByPropertyIdAndIsMainTrue(property.getId())
            .map(PropertyImage::getUrl)
            .orElse(null);

    return new PropertyDto(
        property.getId(),
        property.getOwnerId(),
        property.getTitle(),
        property.getDescription(),
        property.getCity(),
        imageUrl,
        property.getLatitude(),
        property.getLongitude(),
        property.getCountry(),
        property.getPostalCode(),
        property.getStreet(),
        property.getStreetNumber());
  }

  private UnitDto mapToDto(Unit unit, String currency) {

    String imageUrl =
        unitImageRepository
            .findByUnitIdAndIsMainTrue(unit.getId())
            .map(UnitImage::getUrl)
            .orElse(null);

    io.github.kwatera_project.kwatera.property_service.dto.CurrencyMetadataDto currencyInfo =
        new io.github.kwatera_project.kwatera.property_service.dto.CurrencyMetadataDto(
            "PLN", "PLN", java.math.BigDecimal.ONE, java.time.LocalDate.now());
    java.math.BigDecimal convertedPricePerNight = unit.getPricePerNight();

    if (currency != null && !"PLN".equalsIgnoreCase(currency)) {
      String requestedCurrency = currency.toUpperCase(java.util.Locale.ROOT);

      try {
        io.github.kwatera_project.kwatera.property_service.dto.NbpResponseDto nbpResponse =
            "EUR".equals(requestedCurrency)
                ? nbpExchangeRateClient.getEurExchangeRate()
                : nbpExchangeRateClient.getUsdExchangeRate();

        if (nbpResponse != null && nbpResponse.rates() != null && !nbpResponse.rates().isEmpty()) {
          io.github.kwatera_project.kwatera.property_service.dto.NbpRateDto rateDto =
              nbpResponse.rates().get(0);
          java.math.BigDecimal rate = rateDto.mid();

          currencyInfo =
              new io.github.kwatera_project.kwatera.property_service.dto.CurrencyMetadataDto(
                  "PLN", requestedCurrency, rate, rateDto.effectiveDate());

          if (convertedPricePerNight != null) {
            convertedPricePerNight =
                convertedPricePerNight.divide(rate, 2, java.math.RoundingMode.HALF_UP);
          }
        }
      } catch (Exception e) {
        // Ignore exception, fallback to PLN
      }
    }

    return new UnitDto(
        unit.getId(),
        unit.getName(),
        unit.getDescription(),
        unit.getPricePerNight(),
        unit.getCapacity(),
        imageUrl,
        unit.getPropertyId(),
        unit.getUnitType(),
        unit.getUnitNumber(),
        unit.getFloor(),
        convertedPricePerNight,
        currencyInfo);
  }

  private UnitSettlementItemDto mapToDto(UnitSettlementItem item) {
    return new UnitSettlementItemDto(
        item.getId(),
        item.getUnitId(),
        item.getSettlementItemType(),
        item.getPricePerUnit(),
        item.getMeasurementUnit(),
        item.getBillingType());
  }

  private <T> void applyIfPresent(Optional<T> optionalValue, Consumer<T> setter) {
    optionalValue.ifPresent(setter);
  }

  @Transactional
  public UnitDto updateUnit(
      UUID ownerId, UUID propertyId, UUID unitId, String currency, UnitUpdateRequest request) {

    Property property =
        propertyRepository
            .findById(propertyId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));

    if (!property.getOwnerId().equals(ownerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    Unit unit =
        unitRepository
            .findByIdAndPropertyId(unitId, propertyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));

    applyIfPresent(request.name(), unit::setName);
    applyIfPresent(request.description(), unit::setDescription);
    applyIfPresent(request.pricePerNight(), unit::setPricePerNight);
    applyIfPresent(request.capacity(), unit::setCapacity);
    applyIfPresent(request.unitType(), unit::setUnitType);
    applyIfPresent(request.unitNumber(), unit::setUnitNumber);
    applyIfPresent(request.floor(), unit::setFloor);

    unitRepository.save(unit);

    return mapToDto(unit, currency);
  }

  @Transactional
  public PropertyDto updateProperty(UUID ownerId, UUID propertyId, PropertyUpdateRequest request) {
    Property property =
        propertyRepository
            .findById(propertyId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));

    if (!property.getOwnerId().equals(ownerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    boolean addressChanged =
        request.city().isPresent()
            || request.country().isPresent()
            || request.postalCode().isPresent()
            || request.street().isPresent()
            || request.streetNumber().isPresent();

    applyIfPresent(request.title(), property::setTitle);
    applyIfPresent(request.description(), property::setDescription);
    applyIfPresent(request.city(), property::setCity);
    applyIfPresent(request.country(), property::setCountry);
    applyIfPresent(request.postalCode(), property::setPostalCode);
    applyIfPresent(request.street(), property::setStreet);
    applyIfPresent(request.streetNumber(), property::setStreetNumber);

    if (addressChanged) {
      Coordinates coordinates =
          geocodingService.getCoordinates(
              property.getStreet(),
              property.getStreetNumber(),
              property.getPostalCode(),
              property.getCity(),
              property.getCountry());

      property.setLatitude(coordinates.latitude());
      property.setLongitude(coordinates.longitude());
    }

    propertyRepository.save(property);

    return mapToDto(property);
  }

  @Transactional
  public void deleteUnit(UUID ownerId, UUID propertyId, UUID unitId, String token) {

    Property property =
        propertyRepository
            .findById(propertyId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));

    if (!property.getOwnerId().equals(ownerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    Unit unit =
        unitRepository
            .findByIdAndPropertyId(unitId, propertyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));

    if (reservationClient.hasReservationsForUnit(unitId, token)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Unit has reservations");
    }

    unitRepository.delete(unit);
  }

  @Transactional
  public void deleteProperty(UUID ownerId, UUID propertyId, String token) {
    Property property =
        propertyRepository
            .findById(propertyId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));

    if (!property.getOwnerId().equals(ownerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    List<Unit> units = unitRepository.findByPropertyId(propertyId);

    for (Unit unit : units) {
      if (reservationClient.hasReservationsForUnit(unit.getId(), token)) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Property contains units with reservations");
      }
    }

    unitRepository.deleteByPropertyId(propertyId);
    propertyRepository.delete(property);
  }

  @Transactional
  public PropertyDto createProperty(UUID ownerId, PropertyCreateRequest request) {
    Property newProperty = new Property();

    newProperty.setOwnerId(ownerId);
    newProperty.setTitle(request.title());
    newProperty.setDescription(request.description());
    newProperty.setCity(request.city());
    newProperty.setCountry(request.country());
    newProperty.setPostalCode(request.postalCode());
    newProperty.setStreet(request.street());
    newProperty.setStreetNumber(request.streetNumber());

    Coordinates coordinates =
        geocodingService.getCoordinates(
            request.street(),
            request.streetNumber(),
            request.postalCode(),
            request.city(),
            request.country());

    newProperty.setLatitude(coordinates.latitude());
    newProperty.setLongitude(coordinates.longitude());

    Property savedProperty = propertyRepository.save(newProperty);

    return mapToDto(savedProperty);
  }

  @Transactional
  public UnitDto createUnit(UUID ownerId, UUID propertyId, UnitCreateRequest request) {
    Property property =
        propertyRepository
            .findById(propertyId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));

    if (!property.getOwnerId().equals(ownerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    Unit newUnit = new Unit();
    newUnit.setPropertyId(propertyId);
    newUnit.setName(request.name());
    newUnit.setDescription(request.description());
    newUnit.setPricePerNight(request.pricePerNight());
    newUnit.setCapacity(request.capacity());
    newUnit.setUnitType(request.unitType());
    newUnit.setUnitNumber(request.unitNumber());
    newUnit.setFloor(request.floor());

    Unit saved = unitRepository.save(newUnit);

    return mapToDto(saved, "PLN");
  }
}

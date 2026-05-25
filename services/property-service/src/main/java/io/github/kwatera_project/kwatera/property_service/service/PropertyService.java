package io.github.kwatera_project.kwatera.property_service.service;

import io.github.kwatera_project.kwatera.property_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.property_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.property_service.dto.UnitSettlementItemDto;
import io.github.kwatera_project.kwatera.property_service.model.*;
import io.github.kwatera_project.kwatera.property_service.repository.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PropertyService {

  private final PropertyRepository propertyRepository;
  private final UnitRepository unitRepository;
  private final PropertyImageRepository propertyImageRepository;
  private final UnitImageRepository unitImageRepository;
  private final UnitSettlementItemRepository unitSettlementItemRepository;
  private final io.github.kwatera_project.kwatera.property_service.client.NbpExchangeRateClient
      nbpExchangeRateClient;

  public PropertyService(
      PropertyRepository propertyRepository,
      UnitRepository unitRepository,
      PropertyImageRepository propertyImageRepository,
      UnitImageRepository unitImageRepository,
      UnitSettlementItemRepository unitSettlementItemRepository,
      io.github.kwatera_project.kwatera.property_service.client.NbpExchangeRateClient
          nbpExchangeRateClient) {
    this.propertyRepository = propertyRepository;
    this.unitRepository = unitRepository;
    this.propertyImageRepository = propertyImageRepository;
    this.unitImageRepository = unitImageRepository;
    this.unitSettlementItemRepository = unitSettlementItemRepository;
    this.nbpExchangeRateClient = nbpExchangeRateClient;
  }

  public List<UnitDto> getUnits(UUID propertyId, String currency) {
    if (!propertyRepository.existsById(propertyId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
    }
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

    return mapToDto(unit, currency);
  }

  public List<UUID> getUnitIdsByOwnerId(UUID ownerId) {
    List<UUID> propertyIds =
        propertyRepository.findByOwnerId(ownerId).stream().map(Property::getId).toList();

    if (propertyIds.isEmpty()) {
      return List.of();
    }

    return unitRepository.findByPropertyIdIn(propertyIds).stream().map(Unit::getId).toList();
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
        property.getTitle(),
        property.getDescription(),
        property.getLocation(),
        imageUrl);
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
      try {
        io.github.kwatera_project.kwatera.property_service.dto.NbpResponseDto nbpResponse =
            nbpExchangeRateClient.getExchangeRate(currency);
        if (nbpResponse != null && nbpResponse.rates() != null && !nbpResponse.rates().isEmpty()) {
          io.github.kwatera_project.kwatera.property_service.dto.NbpRateDto rateDto =
              nbpResponse.rates().get(0);
          java.math.BigDecimal rate = rateDto.mid();
          currencyInfo =
              new io.github.kwatera_project.kwatera.property_service.dto.CurrencyMetadataDto(
                  "PLN", currency.toUpperCase(), rate, rateDto.effectiveDate());
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
}

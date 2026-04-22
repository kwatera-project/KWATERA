package io.github.kwatera_project.kwatera.property_service.service;

import io.github.kwatera_project.kwatera.property_service.model.Property;
import io.github.kwatera_project.kwatera.property_service.model.Unit;
import io.github.kwatera_project.kwatera.property_service.repository.PropertyRepository;
import io.github.kwatera_project.kwatera.property_service.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PropertyServiceTest {

    private PropertyRepository propertyRepository;
    private UnitRepository unitRepository;
    private PropertyService propertyService;

    @BeforeEach
    void setUp() {
        propertyRepository = mock(PropertyRepository.class);
        unitRepository = mock(UnitRepository.class);
        propertyService = new PropertyService(propertyRepository, unitRepository);
    }

    @Test
    void getAll_shouldReturnProperties() {
        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setTitle("Test");
        property.setLocation("Warsaw");
        property.setDescription("Desc");
        property.setImageUrl("img.jpg");

        when(propertyRepository.findAll()).thenReturn(List.of(property));

        var result = propertyService.getAll();

        assertEquals(1, result.size());
        assertEquals("Test", result.get(0).getTitle());
    }

    @Test
    void getById_shouldReturnProperty() {
        UUID id = UUID.randomUUID();

        Property property = new Property();
        property.setId(id);
        property.setTitle("Test");
        property.setLocation("Warsaw");
        property.setDescription("Desc");
        property.setImageUrl("img.jpg");

        when(propertyRepository.findById(id)).thenReturn(Optional.of(property));

        var result = propertyService.getById(id);

        assertEquals(id, result.getId());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();

        when(propertyRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> propertyService.getById(id));
    }

    @Test
    void getUnits_shouldReturnUnits() {
        UUID propertyId = UUID.randomUUID();

        when(propertyRepository.existsById(propertyId)).thenReturn(true);

        Unit unit = new Unit();
        unit.setId(UUID.randomUUID());
        unit.setName("Room");
        unit.setDescription("Desc");
        unit.setCapacity(2);
        unit.setPricePerNight(BigDecimal.valueOf(200));

        when(unitRepository.findByPropertyId(propertyId))
                .thenReturn(List.of(unit));

        var result = propertyService.getUnits(propertyId);

        assertEquals(1, result.size());
        assertEquals("Room", result.get(0).getName());
    }

    @Test
    void getUnits_shouldThrowWhenPropertyNotExists() {
        UUID propertyId = UUID.randomUUID();

        when(propertyRepository.existsById(propertyId)).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> propertyService.getUnits(propertyId));
    }

    @Test
    void getUnitById_shouldReturnUnit() {
        UUID id = UUID.randomUUID();

        Unit unit = new Unit();
        unit.setId(id);
        unit.setName("Room");
        unit.setDescription("Desc");
        unit.setCapacity(2);
        unit.setPricePerNight(BigDecimal.valueOf(200));

        when(unitRepository.findById(id)).thenReturn(Optional.of(unit));

        var result = propertyService.getUnitById(id);

        assertEquals(id, result.getId());
    }

    @Test
    void getUnitById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();

        when(unitRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> propertyService.getUnitById(id));
    }
}
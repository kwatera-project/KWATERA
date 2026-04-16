package io.github.kwatera_project.kwatera.property_service.controller;
import io.github.kwatera_project.kwatera.property_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.property_service.service.PropertyService;
import io.github.kwatera_project.kwatera.property_service.model.Unit;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    public List<PropertyDto> getAllProperties() {
        return propertyService.getAll();

    }

    @GetMapping("/{id}")
    public PropertyDto getPropertyById(@PathVariable UUID id){
    return propertyService.getById(id);
    }

    @GetMapping("/{id}/units")
    public List<Unit> getUnits(@PathVariable UUID id){
        return propertyService.getUnits(id);
    }

}

package io.github.kwatera_project.kwatera.reservation_service.service;

import io.github.kwatera_project.kwatera.reservation_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.reservation_service.model.Property;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PropertyService {

    private final List<Property> properties = new ArrayList<>();

    public PropertyService() {
        Property p1 = new Property(1L, "Mini Lake House - domek nad samym jeziorem", "Rentyny", 5, 350.0, "https://plus.unsplash.com/premium_photo-1686090450479-370d5ddf4de1?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D");
        Property p2 = new Property(2L, "Pan Brda", "Łąck", 6, 400.0, "https://images.unsplash.com/photo-1595521624992-48a59aef95e3?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D");
        Property p3 = new Property(3L, "Kownatki 25a - Odkryj spokój między sosnami", "Kownatki", 6, 230.0, "https://images.unsplash.com/photo-1723663561534-9b129f182785?q=80&w=988&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D");

        properties.add(p1);
        properties.add(p2);
        properties.add(p3);
    }

    public List<PropertyDto> getAll() {
        List<PropertyDto> result = new ArrayList<>();

        for (Property property : properties) {
            result.add(property.toDto());
        }
        return result;
    }

    public PropertyDto getById(Long id) {
        for (Property property : properties) {
            if (property.getId().equals(id)) {
                return property.toDto();
            }
        }
        return null;
    }



}



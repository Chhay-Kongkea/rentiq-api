package co.istad.rentiq_api.features.category.exception;

import java.util.UUID;


public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(UUID id) {
        super("Category not found with id: " + id);
    }
}

package co.istad.rentiq_api.features.category.exception;


public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Integer id) {
        super("Category not found with id: " + id);
    }
}
package co.istad.rentiq_api.features.category.exception;


public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException(String name) {
        super("Category already exists with name: " + name);
    }
}
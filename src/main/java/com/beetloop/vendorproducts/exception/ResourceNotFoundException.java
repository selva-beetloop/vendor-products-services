package com.beetloop.vendorproducts.exception;

/** 404 — the product, variant or file id does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException product(Object id) {
        return new ResourceNotFoundException("Product " + id + " not found");
    }

    public static ResourceNotFoundException variant(Object id) {
        return new ResourceNotFoundException("Variant " + id + " not found");
    }

    public static ResourceNotFoundException file(Object id) {
        return new ResourceNotFoundException("File " + id + " not found");
    }
}

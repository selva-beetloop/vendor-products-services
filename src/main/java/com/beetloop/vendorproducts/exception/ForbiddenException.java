package com.beetloop.vendorproducts.exception;

/** 403 — authenticated caller may not touch this resource. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}

package com.beetloop.catalog.template.model;

public enum ChildCollection {
    /** Products: a listing holds N variants of one thing. */
    VARIANTS,
    /** Services: a listing holds N selected services, each configured independently. */
    CONFIGURATIONS,
    NONE
}

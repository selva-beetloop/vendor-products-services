package com.beetloop.catalog.shared.model;

/** Decimal string plus ISO-4217 currency. Never a float. */
public record Money(String amount, String currency) {
}

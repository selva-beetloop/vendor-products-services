package com.beetloop.catalog.template;

/**
 * SAVE   - permissive about completeness, strict about coherence. A half-filled step is a
 *          legitimate draft, so required fields are NOT enforced.
 * SUBMIT - the only real gate. Everything is enforced, from scratch, because the client
 *          demonstrably filters nothing.
 */
public enum ValidationMode {
    SAVE,
    SUBMIT
}

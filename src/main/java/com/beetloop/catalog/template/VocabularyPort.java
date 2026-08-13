package com.beetloop.catalog.template;

import java.util.List;

/**
 * Implemented by the masters module. Declared here so the template engine does not depend on it -
 * `businessRole` is validated against a vocabulary rather than a Java enum precisely because the
 * Packaging Machinery role set is open-ended behind the "More Roles" card.
 */
public interface VocabularyPort {

    boolean contains(String vocabularyCode, String parentCode, String value);

    List<String> codes(String vocabularyCode, String parentCode);

    /** True when the vendor has registered this as a custom "+ Add" value for the field. */
    boolean isCustomValue(String fieldKey, String value);
}

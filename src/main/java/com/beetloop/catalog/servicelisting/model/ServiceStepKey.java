package com.beetloop.catalog.servicelisting.model;

import java.util.List;

/**
 * Four outer steps for every service category; only the LABELS vary (Accreditations &
 * Certifications vs Compliance, Review & Publish vs Review & Submit). Labels come from the template.
 *
 * URLs use the kebab form, data{} uses the camel form.
 */
public final class ServiceStepKey {

    public static final String SELECT_SERVICE = "selectService";
    public static final String CONFIGURE_SERVICES = "configureServices";
    public static final String COMPLIANCE = "compliance";
    public static final String REVIEW = "review";

    public static final List<String> ALL =
            List.of(SELECT_SERVICE, CONFIGURE_SERVICES, COMPLIANCE, REVIEW);

    public static final List<String> COMPLETABLE =
            List.of(SELECT_SERVICE, CONFIGURE_SERVICES, COMPLIANCE);

    private ServiceStepKey() {
    }
}

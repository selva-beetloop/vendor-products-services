package com.beetloop.vendorproducts.security;

import com.beetloop.security.context.AuthContext;
import com.beetloop.vendorproducts.exception.ForbiddenException;
import org.springframework.stereotype.Component;

/**
 * JWT identity helpers. Vendor JWT {@code userId} is the vendor id (same as be-leads-rfq).
 */
@Component
public class CurrentUser {

    public String userId() {
        return AuthContext.requireUserId();
    }

    public boolean isStaff() {
        return AuthContext.hasAnyRole("QC_ADMIN", "QC_USER", "INTEL_QC", "INTEL_ADMIN");
    }

    public boolean isVendorQc() {
        return AuthContext.hasAnyRole("QC_ADMIN", "QC_USER");
    }

    public boolean isIntelQc() {
        return AuthContext.hasAnyRole("INTEL_QC", "INTEL_ADMIN");
    }

    /** Null scope means QC/Intel may see every vendor; otherwise list is filtered. */
    public String vendorScope() {
        return isStaff() ? null : userId();
    }

    public void requireOwner(String vendorId) {
        if (isStaff()) {
            return;
        }
        if (vendorId == null || !vendorId.equals(userId())) {
            throw new ForbiddenException("Not allowed to access another vendor's listing");
        }
    }
}

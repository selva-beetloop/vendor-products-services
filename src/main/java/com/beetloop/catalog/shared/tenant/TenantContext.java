package com.beetloop.catalog.shared.tenant;

import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.error.ErrorCode;

import java.util.Set;

/**
 * The vendor id is read from the JWT into here and applied as a mandatory repository-level filter.
 * It is NEVER read from a request body or path: a body vendorId is silently ignored, and a
 * cross-vendor id resolves to 404 rather than 403 so ids are not enumerable.
 */
public final class TenantContext {

    public record Principal(String vendorId, String userId, Set<String> roles, String requestId) {
    }

    private static final ThreadLocal<Principal> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Principal principal) {
        CURRENT.set(principal);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static Principal currentOrNull() {
        return CURRENT.get();
    }

    public static Principal current() {
        Principal p = CURRENT.get();
        if (p == null) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "No authenticated principal on this request.");
        }
        return p;
    }

    /** Throws 401 rather than returning null, so no query can accidentally run unscoped. */
    public static String vendorId() {
        String vendorId = current().vendorId();
        if (vendorId == null || vendorId.isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED,
                    "The access token carries no vendor_id claim.");
        }
        return vendorId;
    }

    public static String userId() {
        return current().userId();
    }

    public static String requestId() {
        Principal p = CURRENT.get();
        return p == null ? null : p.requestId();
    }

    public static boolean hasRole(String role) {
        Principal p = CURRENT.get();
        return p != null && p.roles().contains(role);
    }
}

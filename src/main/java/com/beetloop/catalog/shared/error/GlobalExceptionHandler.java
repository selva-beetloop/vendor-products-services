package com.beetloop.catalog.shared.error;

import com.beetloop.catalog.shared.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Single source of RFC 9457 problem+json responses. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> onValidation(ValidationException ex, HttpServletRequest req) {
        ProblemDetail pd = base(ex.getErrorCode(), ex.getMessage(), req);
        pd.setProperty("bannerMessage", ex.getBannerMessage());
        pd.setProperty("stepErrors", ex.stepErrors());
        pd.setProperty("fieldErrors", ex.getFieldErrors());
        if (!ex.getWarnings().isEmpty()) {
            pd.setProperty("warnings", ex.getWarnings());
        }
        return ResponseEntity.status(ex.getErrorCode().status()).body(pd);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> onApi(ApiException ex, HttpServletRequest req) {
        ProblemDetail pd = base(ex.getErrorCode(), ex.getMessage(), req);
        ex.getExtensions().forEach(pd::setProperty);
        return ResponseEntity.status(ex.getErrorCode().status()).body(pd);
    }

    /** Bean Validation on the envelope DTOs (not on data{}, which the template engine owns). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> onBeanValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        List<FieldError> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> errors.add(
                FieldError.of(null, fe.getField(), fe.getField(), "FIELD_REQUIRED",
                        fe.getDefaultMessage() == null ? "Invalid value" : fe.getDefaultMessage(),
                        fe.getRejectedValue())));
        return onValidation(new ValidationException(errors, List.of()), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> onUnreadable(HttpMessageNotReadableException ex,
                                                      HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(base(ErrorCode.MALFORMED, "Request body could not be parsed as JSON.", req));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> onTooLarge(MaxUploadSizeExceededException ex,
                                                    HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(base(ErrorCode.FILE_TOO_LARGE, "The uploaded file exceeds the configured limit.", req));
    }

    /**
     * The @Version backstop behind the explicit If-Match check. Reached when two writers race
     * between the read and the save.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> onOptimisticLock(OptimisticLockingFailureException ex,
                                                          HttpServletRequest req) {
        ProblemDetail pd = base(ErrorCode.STALE_VERSION,
                "The listing was modified concurrently. Reload it and reapply your change.", req);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> onAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(base(ErrorCode.FORBIDDEN, "You do not have permission to perform this action.", req));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> onUnhandled(Exception ex, HttpServletRequest req) {
        // requestId only, never a stack trace.
        log.error("Unhandled exception requestId={} path={}", TenantContext.requestId(), req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(base(ErrorCode.INTERNAL, "An unexpected error occurred.", req));
    }

    private ProblemDetail base(ErrorCode code, String detail, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.status(), detail);
        pd.setType(URI.create(code.type()));
        pd.setTitle(code.title());
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("code", code.code());
        pd.setProperty("requestId", TenantContext.requestId());
        return pd;
    }

    static Map<String, Object> noExtensions() {
        return Map.of();
    }
}

package com.libreuml.backend.infrastructure.in.web.advice;

import com.libreuml.backend.application.apikey.exception.ApiKeyNotFoundException;
import com.libreuml.backend.application.apikey.exception.ApiKeyOwnershipException;
import com.libreuml.backend.application.apikey.exception.InvalidRedemptionCodeException;
import com.libreuml.backend.application.apikey.exception.RedemptionLimitExceededException;
import com.libreuml.backend.application.auth.exception.InvalidRefreshTokenException;
import com.libreuml.backend.application.auth.exception.OAuthException;
import com.libreuml.backend.application.emailverification.exception.EmailAlreadyVerifiedException;
import com.libreuml.backend.application.emailverification.exception.EmailNotVerifiedException;
import com.libreuml.backend.application.emailverification.exception.InvalidVerificationTokenException;
import com.libreuml.backend.application.passwordreset.exception.InvalidPasswordResetTokenException;
import com.libreuml.backend.application.common.port.out.MetricsPort;
import com.libreuml.backend.application.courses.exception.CourseAlreadyExistsException;
import com.libreuml.backend.application.courses.exception.CourseNotFoundException;
import com.libreuml.backend.application.diagram.exception.DiagramConflictException;
import com.libreuml.backend.application.diagram.exception.DiagramNotFoundException;
import com.libreuml.backend.application.enrollment.exception.EnrollmentAlreadyExistsException;
import com.libreuml.backend.application.project.exception.ProjectConflictException;
import com.libreuml.backend.application.project.exception.ProjectNotFoundException;
import com.libreuml.backend.application.projectdiagram.exception.ProjectDiagramConflictException;
import com.libreuml.backend.application.projectdiagram.exception.ProjectDiagramNotFoundException;
import com.libreuml.backend.application.projectmodel.exception.ModelQuotaExceededException;
import com.libreuml.backend.application.projectmodel.exception.ProjectModelConflictException;
import com.libreuml.backend.application.projectmodel.exception.ProjectModelNotFoundException;
import com.libreuml.backend.application.resource.exception.ResourceNotFoundException;
import com.libreuml.backend.application.user.exception.IncorrectPasswordException;
import com.libreuml.backend.application.user.exception.UserAlreadyExistsException;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.domain.model.exception.DiagramOwnershipException;
import com.libreuml.backend.domain.model.exception.DiagramPayloadTooLargeException;
import com.libreuml.backend.domain.model.exception.ProjectOwnershipException;
import com.libreuml.backend.domain.model.exception.QuotaExceededException;
import com.libreuml.backend.domain.model.exception.UserNotAuthorizedException;
import com.libreuml.backend.infrastructure.in.web.dto.response.ErrorResponse;
import com.libreuml.backend.infrastructure.in.web.dto.response.FieldValidationError;
import com.libreuml.backend.infrastructure.in.web.dto.response.project.ModelQuotaResponse;
import com.libreuml.backend.infrastructure.in.web.dto.response.project.ProjectConflictResponse;
import com.libreuml.backend.infrastructure.in.web.dto.response.project.ProjectDiagramConflictResponse;
import com.libreuml.backend.infrastructure.in.web.dto.response.project.ProjectModelConflictResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final MetricsPort metricsPort;

    // ── Email Verification ────────────────────────────────────────────────────

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerified(
            EmailNotVerifiedException ex, HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage(), "EMAIL_NOT_VERIFIED", req);
    }

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyVerified(
            EmailAlreadyVerifiedException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVerificationToken(
            InvalidVerificationTokenException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordResetToken(
            InvalidPasswordResetTokenException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    // ── Auth (401) ────────────────────────────────────────────────────────────

    @ExceptionHandler(IncorrectPasswordException.class)
    public ResponseEntity<ErrorResponse> handleIncorrectPassword(
            IncorrectPasswordException ex, HttpServletRequest req) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_CREDENTIALS", req);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex, HttpServletRequest req) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    // ── Not Found (404) ───────────────────────────────────────────────────────

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(DiagramNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDiagramNotFound(
            DiagramNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectNotFound(
            ProjectNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(ProjectModelNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectModelNotFound(
            ProjectModelNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(ProjectDiagramNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectDiagramNotFound(
            ProjectDiagramNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCourseNotFound(
            CourseNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApiKeyNotFound(
            ApiKeyNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidRedemptionCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRedemptionCode(
            InvalidRedemptionCodeException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    // ── Forbidden (403) ───────────────────────────────────────────────────────

    @ExceptionHandler(UserNotAuthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUserNotAuthorized(
            UserNotAuthorizedException ex, HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(DiagramOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleDiagramOwnership(
            DiagramOwnershipException ex, HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage(), "OWNERSHIP_DENIED", req);
    }

    @ExceptionHandler(ProjectOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleProjectOwnership(
            ProjectOwnershipException ex, HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage(), "OWNERSHIP_DENIED", req);
    }

    @ExceptionHandler(ApiKeyOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleApiKeyOwnership(
            ApiKeyOwnershipException ex, HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    // ── Conflict (409) ────────────────────────────────────────────────────────

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(EnrollmentAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEnrollmentAlreadyExists(
            EnrollmentAlreadyExistsException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(CourseAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCourseAlreadyExists(
            CourseAlreadyExistsException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(DiagramConflictException.class)
    public ResponseEntity<ErrorResponse> handleDiagramConflict(
            DiagramConflictException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(ProjectConflictException.class)
    public ResponseEntity<ProjectConflictResponse> handleProjectConflict(ProjectConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ProjectConflictResponse("CONFLICT", ex.getMessage(), ex.getServerVersion()));
    }

    @ExceptionHandler(ProjectModelConflictException.class)
    public ResponseEntity<ProjectModelConflictResponse> handleProjectModelConflict(ProjectModelConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ProjectModelConflictResponse("CONFLICT", ex.getMessage(), ex.getServerVersion(), ex.getServerData()));
    }

    @ExceptionHandler(ProjectDiagramConflictException.class)
    public ResponseEntity<ProjectDiagramConflictResponse> handleProjectDiagramConflict(ProjectDiagramConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ProjectDiagramConflictResponse("CONFLICT", ex.getMessage(), ex.getServerVersion()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, "Resource was modified by another request. Reload and retry.", req);
    }

    // ── Unprocessable Entity (422) ────────────────────────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Request payload exceeds the maximum allowed size.", "PAYLOAD_TOO_LARGE", req);
    }

    @ExceptionHandler(DiagramPayloadTooLargeException.class)
    public ResponseEntity<ErrorResponse> handleDiagramPayloadTooLarge(
            DiagramPayloadTooLargeException ex, HttpServletRequest req) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "PAYLOAD_TOO_LARGE", req);
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(
            QuotaExceededException ex, HttpServletRequest req) {
        metricsPort.incrementQuotaRejection();
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req);
    }

    @ExceptionHandler(ModelQuotaExceededException.class)
    public ResponseEntity<ModelQuotaResponse> handleModelQuotaExceeded(ModelQuotaExceededException ex) {
        metricsPort.incrementQuotaRejection();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ModelQuotaResponse("QUOTA_EXCEEDED", ex.getMessage(), ex.getUsed(), ex.getQuota()));
    }

    @ExceptionHandler(RedemptionLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRedemptionLimitExceeded(
            RedemptionLimitExceededException ex, HttpServletRequest req) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req);
    }

    // ── OAuth (400) ───────────────────────────────────────────────────────────

    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<ErrorResponse> handleOAuthException(
            OAuthException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    // ── Validation (400) ──────────────────────────────────────────────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldValidationError(fe.getField(), fe.getRejectedValue(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                Instant.now(),
                req.getRequestURI(),
                errors,
                "VALIDATION_FAILED"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {
        List<FieldValidationError> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldValidationError(
                        v.getPropertyPath().toString(),
                        v.getInvalidValue(),
                        v.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                Instant.now(),
                req.getRequestURI(),
                errors,
                "VALIDATION_FAILED"));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "Request parameter validation failed", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'", req);
    }

    // ── Routing errors ────────────────────────────────────────────────────────

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return error(HttpStatus.METHOD_NOT_ALLOWED,
                "Method '" + ex.getMethod() + "' is not supported for this endpoint", req);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND,
                "No endpoint found for " + ex.getHttpMethod() + " " + ex.getRequestURL(), req);
    }

    // ── Fallback (500) ────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest req) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", req);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest req) {
        return error(status, message, null, req);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, String code, HttpServletRequest req) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                Instant.now(),
                req.getRequestURI(),
                null,
                code));
    }
}

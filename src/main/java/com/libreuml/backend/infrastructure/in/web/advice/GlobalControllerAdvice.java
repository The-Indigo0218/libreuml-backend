package com.libreuml.backend.infrastructure.in.web.advice;

import com.libreuml.backend.application.auth.exception.InvalidRefreshTokenException;
import com.libreuml.backend.application.auth.exception.OAuthException;
import com.libreuml.backend.application.courses.exception.CourseNotFoundException;
import com.libreuml.backend.application.diagram.exception.DiagramConflictException;
import com.libreuml.backend.application.diagram.exception.DiagramNotFoundException;
import com.libreuml.backend.domain.model.exception.DiagramOwnershipException;
import com.libreuml.backend.domain.model.exception.DiagramPayloadTooLargeException;
import com.libreuml.backend.domain.model.exception.UserNotAuthorizedException;
import com.libreuml.backend.application.resource.exception.ResourceNotFoundException;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalControllerAdvice {

    // 404 Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        return problemDetail;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("User Not Found");
        return problemDetail;
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ProblemDetail handleCourseNotFound(CourseNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Course Not Found");
        return problemDetail;
    }


    // 400 Bad Request (OAuth errors, including invalid/expired state)
    @ExceptionHandler(OAuthException.class)
    public ProblemDetail handleOAuthException(OAuthException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("OAuth Error");
        return problemDetail;
    }

    // 401 Unauthorized
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problemDetail.setTitle("Invalid Refresh Token");
        return problemDetail;
    }

    @ExceptionHandler(DiagramNotFoundException.class)
    public ProblemDetail handleDiagramNotFound(DiagramNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Diagram Not Found");
        return problemDetail;
    }

    // 403 Forbidden
    @ExceptionHandler(UserNotAuthorizedException.class)
    public ProblemDetail handleUserNotAuthorized(UserNotAuthorizedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problemDetail.setTitle("User Not Authorized");
        return problemDetail;
    }

    @ExceptionHandler(DiagramOwnershipException.class)
    public ProblemDetail handleDiagramOwnership(DiagramOwnershipException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problemDetail.setTitle("Diagram Access Denied");
        return problemDetail;
    }

    // 409 Conflict — optimistic locking: application-level version guard fires first; JPA @Version
    // is the safety net for true concurrent transactions that bypass the service-layer check.
    @ExceptionHandler(DiagramConflictException.class)
    public ProblemDetail handleDiagramConflict(DiagramConflictException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Diagram Conflict");
        return problemDetail;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The diagram was modified by another request. Reload and retry.");
        problemDetail.setTitle("Diagram Conflict");
        return problemDetail;
    }

    // 422 Unprocessable Entity — payload exceeds the 5 MB domain limit
    @ExceptionHandler(DiagramPayloadTooLargeException.class)
    public ProblemDetail handleDiagramPayloadTooLarge(DiagramPayloadTooLargeException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setTitle("Diagram Payload Too Large");
        return problemDetail;
    }

    // 400 Bad Request (Validation Errors and Type Mismatch)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Not valid argument");
        problemDetail.setTitle("Validation Failed");
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        problemDetail.setProperty("validationErrors", validationErrors);
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The  provided input is invalid" + ex.getMessage());
        problemDetail.setTitle("Invalid Input");
        return problemDetail;
    }

    // 500 Internal Server Error (Fallback)
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred ");
        problemDetail.setTitle("Internal Server Error");
        return problemDetail;
    }
}
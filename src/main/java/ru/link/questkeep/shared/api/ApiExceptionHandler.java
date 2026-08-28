package ru.link.questkeep.shared.api;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ru.link.questkeep.shared.exception.BookingConflictException;
import ru.link.questkeep.shared.exception.DomainException;
import ru.link.questkeep.shared.exception.ForbiddenException;
import ru.link.questkeep.shared.exception.ResourceNotFoundException;
import ru.link.questkeep.shared.exception.UnauthorizedException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(BookingConflictException.class)
	ProblemDetail conflict(BookingConflictException ex) {
		return problem(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail notFound(ResourceNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(UnauthorizedException.class)
	ProblemDetail unauthorized(UnauthorizedException ex) {
		return problem(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler({ ForbiddenException.class, AccessDeniedException.class, AuthorizationDeniedException.class })
	ProblemDetail forbidden(RuntimeException ex) {
		return problem(HttpStatus.FORBIDDEN, ex.getMessage() == null ? "Forbidden" : ex.getMessage());
	}

	@ExceptionHandler(DomainException.class)
	ProblemDetail domain(DomainException ex) {
		return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail validation(MethodArgumentNotValidException ex) {
		ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed");
		detail.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.toList());
		return detail;
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	ProblemDetail missingHeader(MissingRequestHeaderException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Missing header: " + ex.getHeaderName());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail unreadable(HttpMessageNotReadableException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Malformed request body");
	}

	private static ProblemDetail problem(HttpStatus status, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create("about:blank"));
		problem.setTitle(status.getReasonPhrase());
		return problem;
	}
}

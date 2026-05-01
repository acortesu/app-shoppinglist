package com.appcompras.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestExceptionHandlerTest {

	private final RestExceptionHandler handler = new RestExceptionHandler();

	@Test
	void businessRuleExceptionReturnsProperApiError() {
		BusinessRuleException exception = new BusinessRuleException(
				ApiErrorCode.INGREDIENT_NOT_FOUND,
				"Test ingredient not found"
		);
		HttpServletRequest request = mockRequest("/api/recipes");

		ResponseEntity<ApiError> response = handler.handleBusinessRule(exception, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals(400, response.getBody().status());
		assertEquals("INGREDIENT_NOT_FOUND", response.getBody().code());
		assertEquals("Test ingredient not found", response.getBody().error());
		assertNotNull(response.getBody().timestamp());
	}

	@Test
	void illegalArgumentExceptionReturnsProperApiError() {
		IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
		HttpServletRequest request = mockRequest("/api/plans");

		ResponseEntity<ApiError> response = handler.handleIllegalArgument(exception, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals(400, response.getBody().status());
		assertEquals("BUSINESS_RULE_VIOLATION", response.getBody().code());
		assertEquals("Invalid argument", response.getBody().error());
		assertNotNull(response.getBody().timestamp());
	}

	@Test
	void responseStatusExceptionReturnsProperApiError() {
		ResponseStatusException exception = new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"Resource not found"
		);
		HttpServletRequest request = mockRequest("/api/ingredients");

		ResponseEntity<ApiError> response = handler.handleResponseStatus(exception, request);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals(404, response.getBody().status());
		assertEquals("RESOURCE_NOT_FOUND", response.getBody().code());
		assertNotNull(response.getBody().timestamp());
	}

	@Test
	void apiErrorHasRequiredFields() {
		BusinessRuleException exception = new BusinessRuleException(
				ApiErrorCode.PLAN_RECIPE_NOT_FOUND,
				"Recipe not found"
		);
		HttpServletRequest request = mockRequest("/api/shopping-lists");

		ResponseEntity<ApiError> response = handler.handleBusinessRule(exception, request);
		ApiError error = response.getBody();

		assertEquals(400, error.status());
		assertNotNull(error.code());
		assertNotNull(error.error());
		assertNotNull(error.timestamp());
		assertNotNull(error.path());
	}

	private HttpServletRequest mockRequest(String path) {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRequestURI()).thenReturn(path);
		when(request.getAttribute("requestId")).thenReturn(null);
		return request;
	}
}

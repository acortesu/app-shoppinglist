package com.appcompras.config;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiErrorCodeContractTest {

	@Test
	void apiErrorCodeEnumContainsAllKnownBusinessRuleCodes() {
		Set<String> enumCodes = new HashSet<>();
		for (ApiErrorCode code : ApiErrorCode.values()) {
			enumCodes.add(code.name());
		}

		String[] expectedCodes = {
				"INGREDIENT_NOT_FOUND",
				"INVALID_INGREDIENT_UNIT",
				"PLAN_DUPLICATE_SLOT",
				"PLAN_RECIPE_NOT_FOUND",
				"PLAN_SLOT_OUT_OF_RANGE",
				"SHOPPING_ITEM_INGREDIENT_REQUIRED",
				"SHOPPING_ITEM_INVALID_PACKAGE_AMOUNT",
				"SHOPPING_ITEM_INVALID_SORT_ORDER",
				"SHOPPING_ITEM_INVALID_SUGGESTED_PACKAGES",
				"SHOPPING_ITEM_NOTE_TOO_LONG",
				"SHOPPING_ITEM_PACKAGE_FIELDS_INCOMPLETE"
		};

		for (String code : expectedCodes) {
			assertTrue(enumCodes.contains(code),
					"ApiErrorCode enum must contain: " + code);
		}
	}

	@Test
	void businessRuleExceptionAcceptsValidApiErrorCode() {
		assertDoesNotThrow(() -> {
			new BusinessRuleException(
					ApiErrorCode.INGREDIENT_NOT_FOUND,
					"Test message"
			);
		}, "Should be able to create BusinessRuleException with ApiErrorCode");
	}

	@Test
	void businessRuleExceptionPreservesCodeAndMessage() {
		ApiErrorCode code = ApiErrorCode.PLAN_RECIPE_NOT_FOUND;
		String message = "Test recipe not found";

		BusinessRuleException exception = new BusinessRuleException(code, message);

		assertTrue(exception.getCode() == code, "Code should be preserved");
		assertTrue(exception.getMessage().equals(message), "Message should be preserved");
		assertTrue(exception.getCodeAsString().equals(code.name()), "Code as string should match enum name");
	}

	@Test
	void apiErrorCodeEnumHasCorrectSize() {
		int expectedSize = 11;
		int actualSize = ApiErrorCode.values().length;
		assertTrue(actualSize == expectedSize,
				"ApiErrorCode enum should have " + expectedSize + " values, found " + actualSize);
	}
}

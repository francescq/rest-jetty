package com.workshare.micro.api.entity.model;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.Before;
import org.junit.Test;

public class RestRequestObjectTest {

	private EntityRequest tokenRequest;

	private Validator validator;
	private Set<ConstraintViolation<EntityRequest>> constraintViolations;

	@Before
	public void setup() {
		tokenRequest = new EntityRequest();
		tokenRequest.content = "";

		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	public void shouldValidateTokenRequestOk() throws Exception {
		constraintViolations = validator.validate(tokenRequest);

		assertTrue(0 != constraintViolations.size());
	}
}

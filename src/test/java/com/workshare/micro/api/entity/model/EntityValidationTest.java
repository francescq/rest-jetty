package com.workshare.micro.api.entity.model;

import static org.junit.Assert.assertFalse;

import java.util.Date;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.Before;
import org.junit.Test;

import com.workshare.micro.utils.UUIDGenerator;

public class EntityValidationTest {

	private Entity token;

	private UUIDGenerator uuids;
	private Validator validator;
	private Date futureDate;
	private Date date;
	private String createUser;
	private Set<ConstraintViolation<Entity>> constraintViolations;

	@Before
	public void setup() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
		uuids = new UUIDGenerator();
		futureDate = new Date();
		futureDate.setTime(futureDate.getTime() + 5000000);
		date = new Date();
		createUser = "createUser";
	}

	@Test
	public void shouldValidateEntity() throws Exception {

		token = new Entity(uuids.generateString(), null, null, null);

		constraintViolations = validator.validate(token);

		assertFalse(0 == constraintViolations.size());
	}
}

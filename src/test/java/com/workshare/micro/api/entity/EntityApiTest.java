package com.workshare.micro.api.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.workshare.micro.api.entity.model.Entity;
import com.workshare.micro.api.entity.persistence.dao.EntityDao;
import com.workshare.micro.utils.UUIDGenerator;

@SuppressWarnings("unchecked")
public class EntityApiTest {

	private EntityApi api;
	@Mock
	private EntityDao dao;
	@Mock
	private UUIDGenerator uuids;
	@Mock
	private ScheduledExecutorService expirer;

	private String entityRequest;

	private ArgumentCaptor<Entity> tokenCaptured;

	private static final String TOKEN_ID = "ID-XXX-ID";
	private static final String TOKEN_CONTENT = "{\"hello\":\"json\"}";

	private Runnable captureExpirer() {
		ArgumentCaptor<Runnable> runnableCaptured = ArgumentCaptor
				.forClass(Runnable.class);
		verify(expirer).scheduleAtFixedRate(runnableCaptured.capture(),
				anyLong(), anyLong(), any(TimeUnit.class));
		return runnableCaptured.getValue();
	}

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		when(uuids.generateString()).thenReturn(TOKEN_ID);

		api = new EntityApi(dao, uuids, expirer);

		entityRequest = TOKEN_CONTENT;

		tokenCaptured = ArgumentCaptor.forClass(Entity.class);
	}

	@Test
	public void shouldInvokeDaoOnCreateToken() throws Exception {

		api.create(entityRequest);

		verify(dao).create(any(Entity.class));
	}

	@Test
	public void shouldGetEmptyCollectionIfEmpty() throws Exception {
		when(dao.get()).thenReturn(new ArrayList<Entity>());

		Response res = api.get();

		assertEquals(200, res.getStatus());
		assertTrue(((List<Entity>) res.getEntity()).isEmpty());
	}

	private Date addMaxAge(int maxAge) {
		Date date = new Date();
		date.setTime(date.getTime() + maxAge);
		return date;
	}

	@Test
	public void shouldGetCollection() throws Exception {
		Entity expected = new Entity(TOKEN_ID, "content", new Date(),
				"createUser");
		List<Entity> list = Arrays.asList(expected, expected);
		when(dao.get()).thenReturn(list);

		Response res = api.get();

		assertEquals(200, res.getStatus());
		assertEquals(2, ((List<Entity>) res.getEntity()).size());
	}

	@Test
	public void shouldReturnTokenUrlWhenDaoCreateSucceeds() throws Exception {

		Response res = api.create(entityRequest);

		final Object currentLocation = res.getMetadata().get("Location").get(0)
				.toString();
		final Object expectedLocation = TOKEN_ID;
		assertEquals(201, res.getStatus());
		assertEquals(expectedLocation, currentLocation);
	}

	@Test
	public void shouldInvokeDaoOnGet() throws Exception {
		api.get(TOKEN_ID);

		verify(dao).get(anyString());
	}

	@Test
	public void shouldReturnEntityWhenFound() throws Exception {
		Entity expected = new Entity(TOKEN_ID, "content", new Date(),
				"createUser");
		when(dao.get(TOKEN_ID)).thenReturn(expected);

		Response res = api.get(TOKEN_ID);

		assertEquals(expected, res.getEntity());
		assertEquals(200, res.getStatus());
	}

	@Test
	public void shouldReturn404WhenEntityFound() throws Exception {

		when(dao.get(TOKEN_ID)).thenReturn(null);

		Response res = api.get(TOKEN_ID);

		assertEquals(null, res.getEntity());
		assertEquals(404, res.getStatus());
	}

	@Test
	public void shouldInvokeDaoOnDelete() throws Exception {

		api.delete(TOKEN_ID);

		verify(dao).delete(eq(TOKEN_ID));
	}
}

package com.workshare.micro.api.entity.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import com.googlecode.flyway.core.Flyway;
import com.workshare.micro.api.entity.persistence.dao.EntityDao;
import com.workshare.micro.utils.UUIDGenerator;

public class EntityDaoTest {

	private JdbcConnectionPool ds;
	private DBI dbi;
	private Handle h;
	private EntityDao entityDao;
	private Entity entity;
	private List<Entity> entityList;

	private UUIDGenerator uuids;
	private Flyway flyway;

	@Before
	public void connectDatabase() {
		ds = JdbcConnectionPool.create("jdbc:h2:mem:test", "username",
				"password");

		flyway = new Flyway();
		flyway.setDataSource(ds);
		flyway.migrate();

		dbi = new DBI(ds);
		h = dbi.open();
		entityDao = dbi.open(EntityDao.class);
		uuids = new UUIDGenerator();

		entity = new Entity(uuids.generateString(), "content", null, "user");
	}

	@After
	public void closeDatabase() {
		flyway.clean();

		h.close();
		ds.dispose();
	}

	private void generateEntityList(int numEntities) {
		entityList = new ArrayList<Entity>();
		for (int i = 0; i < numEntities; i++) {
			Entity t1 = new Entity(i + "", i + "", new Date(), i + "");
			entityList.add(t1);
		}
	}

	@Test
	public void testCreate() {

		entityDao.create(entity);

		Entity expected = entityDao.get(entity.getId());

		assertEquals(entity, expected);
	}

	@Test
	public void testDelete() {

		entityDao.create(entity);

		entityDao.delete(entity.getId());

		assertNull(entityDao.get(entity.getId()));
	}

	public void testCreateEntityGeneratesCreateDate() {
		entityDao.create(entity);

		Entity tokent = entityDao.get(entity.getId());

		assertTrue(tokent.getCreateDate() instanceof Date);
		assertEquals(entity.getContent(), tokent.getContent());
		assertEquals(entity.getCreateUser(), tokent.getCreateUser());
		assertNotNull(tokent.getCreateDate());
	}

	@Test
	public void testCreateBean() {
		entityDao.create(entity);

		Entity tokenRes = entityDao.get(entity.getId());
		assertNotNull(tokenRes);
	}

	@Test
	public void testGet() {
		entityDao.create(entity);

		Entity getToken = entityDao.get(entity.getId());

		Assert.assertNotNull(getToken);
		Assert.assertEquals(entity, getToken);
	}

	@Test
	public void testGetTokensCollection() {
		entityDao.create(entity);
		Entity entity2 = new Entity("2", "content", new Date(), "createUser");
		entityDao.create(entity2);

		List<Entity> list = entityDao.get();

		Assert.assertEquals(2, list.size());
	}

	@Test
	public void testGetEmptyCollection() {
		List<Entity> list = entityDao.get();

		assertTrue(list.isEmpty());
	}

	@Test
	public void testCreateEntitiesBatch() {

		generateEntityList(15);

		entityDao.createBatch(entityList);

		List<Entity> entities = entityDao.get();
		assertEquals(entityList.size(), entities.size());

	}

	@Test
	public void testGetNonExistentToken() {
		Entity token = entityDao.get("nonexistent");

		Assert.assertNull(token);
	}

	@Test(expected = UnableToExecuteStatementException.class)
	public void testCreateDuplicateToken() {
		entityDao.create(entity);
		entityDao.create(entity);
	}
}
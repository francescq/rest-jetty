package com.workshare.micro.api.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.workshare.micro.api.entity.model.Entity;
import com.workshare.micro.api.entity.persistence.CachingEntityDao;
import com.workshare.micro.api.entity.persistence.dao.EntityDao;
import com.workshare.micro.api.metrics.Monitor;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class CachingEntityDaoTest {

	@Mock
	private Entity entity;
	@Mock
	private EntityDao delegate;
	private CachingEntityDao dao;
	private ScheduledExecutorService scheduler;
	private Runnable scheduledTask;
	private Monitor monitor;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		when(entity.getId()).thenReturn("123");

		MetricRegistry registry = mock(MetricRegistry.class);
		monitor = mock(Monitor.class);
		when(monitor.metrics()).thenReturn(registry);

		makeTheEntity();
	}

	private void makeTheEntity() {
		scheduler = mock(ScheduledExecutorService.class);
		dao = new CachingEntityDao(delegate, scheduler, monitor);

		ArgumentCaptor<Runnable> runnableCaptured = ArgumentCaptor
				.forClass(Runnable.class);
		verify(scheduler).scheduleAtFixedRate(runnableCaptured.capture(),
				anyLong(), anyLong(), any(TimeUnit.class));
		scheduledTask = runnableCaptured.getValue();
	}

	@Test
	public void shouldInvokeCreateBatchOnDelegate() {
		Collection<Entity> tokens = mock(Collection.class);
		dao.createBatch(tokens);
		verify(delegate).createBatch(tokens);
	}

	@Test
	public void shouldInvokeCloseOnDelegate() {
		dao.close();
		verify(delegate).close();
	}

	@Test
	public void shouldInvokeCreateOnDelegateAsyncronously() {

		dao.create(entity);

		scheduledTask.run();

		ArgumentCaptor<Collection> batch = ArgumentCaptor
				.forClass(Collection.class);
		verify(delegate).createBatch(batch.capture());
		assertEquals(1, batch.getValue().size());
	}

	@Test
	public void shouldClearTheCacheAfterFlush() {

		dao.create(entity);

		scheduledTask.run();
		scheduledTask.run();

		ArgumentCaptor<Collection> batch = ArgumentCaptor
				.forClass(Collection.class);
		verify(delegate, times(2)).createBatch(batch.capture());
		Collection secondInvocationValue = batch.getAllValues().get(1);
		assertEquals(0, secondInvocationValue.size());
	}

	@Test
	public void shouldInvokeGetOnDelegateWhenCacheNotPresent() {
		dao.get("aa");

		verify(delegate).get(anyString());
	}

	@Test
	public void shouldUseCacheOnGetWhenCachePresent() {
		dao.create(entity);

		Entity tokenRes = dao.get(entity.getId());

		verifyZeroInteractions(delegate);
		assertEquals(entity, tokenRes);
	}

	@Test
	public void shouldInvokeGetAllOnDelegateWhenCacheNotPresent() {
		dao.get();

		verify(delegate).get();
	}

	@Test
	public void shouldUseCacheAndDelegateOnGet() {
		Entity tokenOnDb = mock(Entity.class);
		when(delegate.get()).thenReturn(Arrays.asList(tokenOnDb));
		dao.create(entity);

		List<Entity> tokens = dao.get();

		assertEquals(2, tokens.size());
	}

	@Test
	public void shouldInvokeDeleteOnDelegate() {
		dao.create(entity);
		scheduledTask.run();

		dao.delete(entity.getId());

		verify(delegate).delete(eq(entity.getId()));
	}

	@Test
	public void shouldDeleteFromCache() {
		dao.create(entity);

		dao.delete(entity.getId());

		Entity tokenRes = dao.get(entity.getId());
		assertNull(tokenRes);

	}

	@Test
	public void shouldRegisterGaugeMonitor() {
		verify(monitor.metrics()).register(eq("entity.cache.size"),
				any(Gauge.class));
	}

	@Test
	public void shouldGaugeMonitorRegisterCacheIncrease() {
		ArgumentCaptor<Gauge> metric = ArgumentCaptor.forClass(Gauge.class);
		verify(monitor.metrics()).register(eq("entity.cache.size"),
				metric.capture());

		dao.create(entity);
		dao.create(entity);

		Gauge<Integer> gauge = metric.getValue();
		assertEquals(2, gauge.getValue().intValue());
	}

	@Test
	public void shouldGaugeMonitorRegisterCacheClearance() {
		ArgumentCaptor<Gauge> metric = ArgumentCaptor.forClass(Gauge.class);
		verify(monitor.metrics()).register(eq("entity.cache.size"),
				metric.capture());

		dao.create(entity);
		dao.create(entity);
		scheduledTask.run();

		Gauge<Integer> gauge = metric.getValue();
		assertEquals(0, gauge.getValue().intValue());
	}

	@Test
	public void shouldReturnCachedElementsDuringWriting() throws Exception {

		Runnable fixture = new ReturnsCachedElementsDuringWritingFixture();

		dao.create(entity);
		fixture.run();

		Entity tokenFromCache = dao.get(entity.getId());
		assertNotNull(tokenFromCache);
	}

	private class ReturnsCachedElementsDuringWritingFixture implements Runnable {
		final CountDownLatch latch;
		final Thread writer;

		ReturnsCachedElementsDuringWritingFixture() {
			latch = new CountDownLatch(1);

			delegate = mock(EntityDao.class);
			Mockito.doAnswer(new Answer() {
				@Override
				public Object answer(InvocationOnMock invocation)
						throws Throwable {
					latch.countDown();
					Thread.sleep(1000L);
					return null;
				}
			}).when(delegate).createBatch(any(Collection.class));

			makeTheEntity();

			writer = new Thread(new Runnable() {
				@Override
				public void run() {
					scheduledTask.run();
				}
			});
		}

		@Override
		public void run() {
			try {
				writer.start();
				latch.await(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new RuntimeException("Fixture failed! WTF?");
			}
		}

	}
}

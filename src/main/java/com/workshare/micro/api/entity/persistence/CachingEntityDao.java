package com.workshare.micro.api.entity.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.skife.jdbi.v2.sqlobject.BindBean;

import com.codahale.metrics.Gauge;
import com.workshare.micro.api.entity.model.Entity;
import com.workshare.micro.api.entity.persistence.dao.EntityDao;
import com.workshare.micro.api.metrics.Monitor;

public class CachingEntityDao implements EntityDao {

	private static final long SCHEDULER_MILLISECONDS_DELAY = 1000;
	private static final Map<String, Entity> EMPTY_CACHE = Collections
			.emptyMap();

	private final AtomicInteger size;
	private final EntityDao delegate;
	private volatile Map<String, Entity> cacheCurrent;
	private volatile Map<String, Entity> cacheWriting = EMPTY_CACHE;

	public CachingEntityDao(EntityDao aDelegate,
			ScheduledExecutorService aScheduler, Monitor monitor) {
		this.delegate = aDelegate;
		this.cacheCurrent = new HashMap<String, Entity>();
		this.size = new AtomicInteger();

		Runnable delayedWriter = new Runnable() {
			@Override
			public void run() {
				cacheWriting = cacheCurrent;
				cacheCurrent = new ConcurrentHashMap<String, Entity>();
				size.set(0);
				delegate.createBatch(cacheWriting.values());
				cacheWriting = EMPTY_CACHE;
			}
		};

		aScheduler.scheduleAtFixedRate(delayedWriter, 0,
				SCHEDULER_MILLISECONDS_DELAY, TimeUnit.MILLISECONDS);

		monitor.metrics().register("entity.cache.size", new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return size.intValue();
			}
		});
	}

	@Override
	public Entity get(String id) {
		Entity entity = cacheCurrent.get(id);
		if (entity == null) {
			entity = cacheWriting.get(id);
		}

		return (entity == null) ? delegate.get(id) : entity;
	}

	@Override
	public List<Entity> get() {
		List<Entity> entities = new ArrayList<Entity>();
		entities.addAll(delegate.get());
		entities.addAll(cacheCurrent.values());
		return entities;
	}

	@Override
	public void delete(String id) {
		cacheCurrent.remove(id);
		delegate.delete(id);
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public void create(Entity entity) {
		cacheCurrent.put(entity.getId(), entity);
		size.incrementAndGet();
	}

	// TODO should we implement this for consistency?
	// we are not optimizing this because our APIs will never directly use this
	@Override
	public void createBatch(@BindBean Collection<Entity> entities) {
		delegate.createBatch(entities);
	}
}

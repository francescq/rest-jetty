package com.workshare.servlet;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.Executors;

import javax.sql.DataSource;
import javax.validation.Validation;
import javax.validation.Validator;

import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.googlecode.flyway.core.Flyway;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.wordnik.swagger.jaxrs.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.ApiListingResourceXML;
import com.workshare.micro.api.entity.EntityApi;
import com.workshare.micro.api.entity.persistence.CachingEntityDao;
import com.workshare.micro.api.entity.persistence.dao.EntityDao;
import com.workshare.micro.api.metrics.Monitor;
import com.workshare.micro.config.MicroConfig;
import com.workshare.micro.config.MicroConfigService;
import com.workshare.micro.utils.UUIDGenerator;

public class GuiceConfigurator extends GuiceServletContextListener {

	private static final Logger logger = LoggerFactory
			.getLogger(GuiceConfigurator.class);

	private static final String JERSEY_API_JSON_POJO_MAPPING_FEATURE = "com.sun.jersey.api.json.POJOMappingFeature";
	private static final String JERSEY_CONFIG_PROPERTY_PACKAGES = "com.sun.jersey.config.property.packages";

	@Override
	protected Injector getInjector() {

		final HashMap<String, String> params = new HashMap<String, String>();
		params.put(
				JERSEY_CONFIG_PROPERTY_PACKAGES,
				"com.wordnik.swagger.jaxrs.listing;com.workshare.micro.api.entity;com.workshare.micro.api");
		params.put(JERSEY_API_JSON_POJO_MAPPING_FEATURE, "true");

		final MicroConfig config = MicroConfigService.load();

		final Injector injector = Guice
				.createInjector(new JerseyServletModule() {
					@Override
					protected void configureServlets() {

						// bind generic objects
						bind(UUIDGenerator.class).asEagerSingleton();
						bind(ApiListingResourceJSON.class);
						bind(ApiListingResourceXML.class);

						// create and bind the metrics monitor
						Monitor monitor = new Monitor();
						bind(Monitor.class).toInstance(monitor);
						JmxReporter.forRegistry(monitor.metrics()).build()
								.start();

						// DataBase connectionPool
						DataSource dataSource = createPool();
						monitor.checks().register("health.database",
								createDbHealthCheck(dataSource));

						// Create the database DAO
						DBI dbi = new DBI(dataSource);
						EntityDao dbDao = dbi.onDemand(EntityDao.class);

						// Init flyWay databaseMigrating tools
						Flyway flyway = new Flyway();
						flyway.setDataSource(dataSource);
						flyway.migrate();

						// create and bind the caching DAO
						EntityDao cachingDao = new CachingEntityDao(dbDao,
								Executors.newSingleThreadScheduledExecutor(),
								monitor);
						bind(EntityDao.class).toInstance(cachingDao);

						// Validation Service
						Validator validator = Validation
								.buildDefaultValidatorFactory().getValidator();
						bind(Validator.class).toInstance(validator);

						// Bind APIs
						bind(EntityApi.class).asEagerSingleton();

						// Route all requests through GuiceContainer
						serve("/api/*").with(GuiceContainer.class, params);
					}

					private HealthCheck createDbHealthCheck(
							final DataSource database) {
						return new HealthCheck() {
							@Override
							protected Result check() throws Exception {
								if (isConnected()) {
									return HealthCheck.Result.healthy();
								} else {
									return HealthCheck.Result
											.unhealthy("Cannot connect to "
													+ config.jdbcUrl());
								}
							}

							private boolean isConnected() throws SQLException {
								Connection conn = database.getConnection();
								try {
									Statement stmt = conn.createStatement();
									try {
										ResultSet rslt = stmt
												.executeQuery(config
														.jdbcCheck());
										try {
											return rslt.next();
										} finally {
											rslt.close();
										}
									} finally {
										stmt.close();
									}
								} finally {
									conn.close();
								}
							}
						};
					}

					private DataSource createPool() {
						try {
							Class.forName(config.jdbcDriver());
						} catch (ClassNotFoundException ex) {
							logger.error(
									"Unable to load JDBC driver "
											+ config.jdbcDriver(), ex);
							die();
						}

						BoneCPConfig boneConfig = new BoneCPConfig();
						boneConfig.setJdbcUrl(config.jdbcUrl());
						boneConfig.setUsername(config.jdbcUser());
						boneConfig.setPassword(config.jdbcPass());
						return new BoneCPDataSource(boneConfig);
					}

					private void die() {
						logger.error("Unrecovarable error - system will shutdown");
						throw new RuntimeException(
								"Unrecoverable error - please kill me!");
					}

				});

		return injector;
	}
}

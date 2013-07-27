package com.workshare.micro.api.entity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.workshare.micro.api.entity.model.Entity;
import com.workshare.micro.api.entity.persistence.dao.EntityDao;
import com.workshare.micro.utils.UUIDGenerator;

@Path("/entity.json")
@Api(value = "/entity", description = "The entitys API")
@Produces({ "application/json" })
public class EntityApi {

	private final static long EXPIRER_MILLISECONDS_PERIOD = 1000 * 3600;

	@Inject
	private EntityDao entityDao;

	@Inject
	private UUIDGenerator uuids;

	public EntityApi() {
	}

	public EntityApi(EntityDao entityDao, UUIDGenerator uuids,
			ScheduledExecutorService aScheduler) {
		this();
		this.entityDao = entityDao;
		this.uuids = uuids;

		Runnable expireRunner = new Runnable() {
			@Override
			public void run() {
				expire();
			}
		};

		aScheduler.scheduleAtFixedRate(expireRunner, 0,
				EXPIRER_MILLISECONDS_PERIOD, TimeUnit.MILLISECONDS);
	}

	private void expire() {
		// Do something in an specific interval
	}

	@POST
	@Path("/")
	@ApiOperation(value = "Creates an entity with the specified content", notes = "The entity is created on the system and the url is returned in the location header")
	@ApiErrors(value = { @ApiError(code = 401, reason = "The user could not be authenticated (wrong or no session id)") })
	public Response create(
			@ApiParam(value = "The entity to be added in the system", required = true) @PathParam("body") String content)
			throws URISyntaxException {
		String id = uuids.generateString();
		String createUser = "createUser";

		Entity entity = new Entity(id, content, null, createUser);

		entityDao.create(entity);
		return Response.created(new URI(id)).build();
	}

	@GET
	@Path("/")
	@ApiOperation(value = "Return a list of all the entitys as defined by the filter", notes = "", responseClass = "com.workshare.micro.api.entitys.model.entity", multiValueResponse = true)
	@ApiErrors(value = { @ApiError(code = 403, reason = "Account list not available (at least to you).") })
	public Response get() {
		List<Entity> entitysList = entityDao.get();
		if (entitysList == null) {
			return Response.serverError().build();
		} else {
			return Response.ok(entitysList).build();
		}
	}

	@GET
	@Path("/{id}")
	@ApiOperation(value = "Creates a entity with the specified content", notes = "User/Session must be the entity creator", responseClass = "com.workshare.micro.api.entitys.model.entity")
	@ApiErrors(value = { @ApiError(code = 401, reason = "The user could not be authenticated (wrong or no session id)") })
	public Response get(
			@ApiParam(value = "id of the entity to get", required = true) @PathParam("id") String id) {
		Entity entity = entityDao.get(id);
		if (entity == null) {
			return Response.status(404).build();
		} else {
			return Response.ok().entity(entity).build();
		}
	}

	@DELETE
	@Path("/{id}")
	@ApiOperation(value = "Deletes a entity with the specified id", notes = "User/Session must be the entity creator")
	@ApiErrors(value = { @ApiError(code = 401, reason = "The user could not be authenticated (wrong or no session id)") })
	public Response delete(
			@ApiParam(value = "id of the entity to delete", required = true) @PathParam("id") String id) {

		entityDao.delete(id);
		return Response.ok().build();
	}
}

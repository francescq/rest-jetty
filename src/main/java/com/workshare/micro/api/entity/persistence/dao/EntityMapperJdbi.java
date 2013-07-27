package com.workshare.micro.api.entity.persistence.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.workshare.micro.api.entity.model.Entity;

public class EntityMapperJdbi implements ResultSetMapper<Entity> {

	@Override
	public Entity map(int index, ResultSet r, StatementContext ctx)
			throws SQLException {
		final Entity entity = new Entity(r.getString("id"),
				r.getString("content"), r.getTimestamp("create_date"),
				r.getString("create_user"));
		return entity;
	}
}
package com.workshare.micro.api.entity.persistence.dao;

import java.util.Collection;
import java.util.List;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import com.workshare.micro.api.entity.model.Entity;

@RegisterMapper(EntityMapperJdbi.class)
public interface EntityDao {
	public static final String create = "INSERT INTO ENTITIES (ID, CONTENT, CREATE_DATE, CREATE_USER) VALUES (:id, :content, NOW(), :createUser)";
	public static final String selectById = "SELECT ID, CONTENT, CREATE_DATE, CREATE_USER FROM ENTITIES WHERE ID = :id";
	public static final String selectCollection = "SELECT ID, CONTENT, CREATE_DATE, CREATE_USER FROM ENTITIES";
	public static final String delete = "DELETE FROM ENTITIES WHERE ID= :id";

	@SqlUpdate(create)
	public void create(@BindBean Entity entity);

	@SqlBatch(create)
	public void createBatch(@BindBean Collection<Entity> entityCollection);

	@SqlQuery(selectById)
	public Entity get(@Bind("id") String id);

	@SqlQuery(selectCollection)
	public List<Entity> get();

	@SqlUpdate(delete)
	public void delete(@Bind("id") String id);

	/**
	 * close with no args is used to close the connection
	 */
	void close();
}
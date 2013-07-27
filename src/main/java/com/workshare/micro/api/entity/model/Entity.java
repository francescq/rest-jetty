package com.workshare.micro.api.entity.model;

import java.util.Date;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.validator.constraints.NotBlank;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

@ApiClass
@XmlRootElement(name = "Entity")
public class Entity {

	@ApiProperty(required = true, value = "The unique id of this entity")
	@NotNull
	private final String id;

	@ApiProperty(required = true, value = "The (application) content of this entity")
	@NotNull
	@NotBlank(message = "contentEmpty")
	@Size(max = 2000, message = "contentOverflow")
	private final String content;

	@ApiProperty(required = true, value = "The creation date of this entity")
	private final Date createDate;

	@ApiProperty(required = true, value = "The id of the user who created date of this entity")
	@NotNull
	@Size(max = 100, message = "createUserOverflow")
	private final String createUser;

	public Entity(String id, String content, Date createDate, String createUser) {
		super();
		this.id = id;
		this.content = content;
		this.createDate = createDate;
		this.createUser = createUser;
	}

	public String getId() {
		return id;
	}

	public String getContent() {
		return content;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public String getCreateUser() {
		return createUser;
	}

	@Override
	public String toString() {
		final StringBuilder s = new StringBuilder("entity [id=").append(id)
				.append(", content=").append(content).append(", createDate=")
				.append(createDate).append(", createUser=").append(createUser)
				.append("]");
		return s.toString();
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Entity) {
			return getId().equals(((Entity) o).getId());
		} else {
			return false;
		}
	}
}
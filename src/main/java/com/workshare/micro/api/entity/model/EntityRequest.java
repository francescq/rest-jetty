package com.workshare.micro.api.entity.model;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.validator.constraints.NotBlank;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

@ApiClass
@XmlRootElement(name = "EntityRequest")
public class EntityRequest {

	@ApiProperty(required = true, value = "The content of this entity")
	@NotNull
	@NotBlank
	public String content;
}
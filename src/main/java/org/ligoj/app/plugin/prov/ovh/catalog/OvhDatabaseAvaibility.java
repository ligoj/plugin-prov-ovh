/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhDatabaseAvaibility  {
	
	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	@JsonProperty("name")
	private String name;
	
	@JsonProperty("version")
	private Object version;
	
	@JsonProperty("defaultVersion")
	private String defaultVersion;
	
	@JsonProperty("description")
	private String description;
	
	@JsonProperty("sslModes")
	private Object sslModes;

}

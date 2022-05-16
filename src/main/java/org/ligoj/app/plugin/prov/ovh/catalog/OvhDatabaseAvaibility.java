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
public class OvhDatabaseAvaibility {

	@JsonProperty("engine")
	private String engine;
	
	@JsonProperty("version")
	private String version;
	
	@JsonProperty("plan")
	private String plan;
	
	@JsonProperty("region")
	private String region;
	
	@JsonProperty("flavor")
	private String flavor;

	@JsonProperty("network")
	private String network;

	@JsonProperty("backup")
	private String backup;
	
	@JsonProperty("minNodeNumber")
	private int minNodeNumber;
	
	@JsonProperty("maxNodeNumber")
	private int maxNodeNumber;
	
	@JsonProperty("minDiskSize")
	private int minDiskSize;
	
	@JsonProperty("maxDiskSize")
	private int maxDiskSize;

}
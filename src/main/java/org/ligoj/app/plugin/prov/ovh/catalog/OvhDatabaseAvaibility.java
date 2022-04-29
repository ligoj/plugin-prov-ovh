/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.util.Date;

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
	
	@JsonProperty("default")
	private boolean defaultt;
	
	@JsonProperty("region")
	private String region;
	
	@JsonProperty("flavor")
	private String flavor;
	
	@JsonProperty("startDate")
	private Date startDate;

	@JsonProperty("endOfLife")
	private String endOfLife;

	@JsonProperty("upstreamEndOfLife")
	private String upstreamEndOfLife;

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
	
	@JsonProperty("status")
	private String status;

}
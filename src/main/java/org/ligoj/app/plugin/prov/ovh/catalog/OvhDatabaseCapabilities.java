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
public class OvhDatabaseCapabilities  {
	
	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	@JsonProperty("engine")
	private String engine;
	
	@JsonProperty("version")
	private double version;
	
	@JsonProperty("plan")
	private String plan;
	
	@JsonProperty("default")
	private boolean DatabaseDefault;
	
	@JsonProperty("region")
	private String region;
	
	@JsonProperty("flavor")
	private String flavor;
	
	@JsonProperty("type")
	private String type;
	
	@JsonProperty("startDate")
	private Date startDate;
	
	@JsonProperty("endOfLife")
	private Date endDate;
	
	@JsonProperty("upstreamEndOfLife")
	private Date upstreamEndOfLife;
	
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

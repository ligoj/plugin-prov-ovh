/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;


@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhFlavor {

	@Getter
	@Setter
	private String id;

	@Getter
	@Setter
	private String name;

	@Setter
	private String region;

	@Getter
	@Setter
	private int ram;

	@Getter
	@Setter
	private int disk;

	@Getter
	@Setter
	private int vcpus;

	@Setter
	private String type;
	
	@Getter
	@Setter
	private String osType;
	
	@Getter
	@Setter
	private int inboundBandwidth;
	
	@Getter
	@Setter
	private int outboundBandwidth;

}

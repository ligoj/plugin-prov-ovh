/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhFlavor {

	private String id;

	private String name;

	private String region;

	private int ram;

	private int disk;

	private int vcpus;

	private String type;
	private String osType;

	private int inboundBandwidth;
	private int outboundBandwidth;

}

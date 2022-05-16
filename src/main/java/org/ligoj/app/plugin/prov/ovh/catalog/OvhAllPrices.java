/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhAllPrices {

	@Getter
	@Setter
	private List<OvhStorage> storage = Collections.emptyList();

	@Getter
	@Setter
	private List<OvhStorage> archive = Collections.emptyList();

	@Setter
	private List<OvhBandwidthArchiveIn> bandwidthArchiveIn = Collections.emptyList();

	@Getter
	@Setter
	private List<OvhVolume> volumes = Collections.emptyList();

	@Getter
	@Setter
	private List<OvhInstancePrice> instances = Collections.emptyList();

	@Getter
	@Setter
	private List<OvhStorage> snapshots = Collections.emptyList();

	@Setter
	private List<OvhBandwidthStorage> bandwidthStorage = Collections.emptyList();

	@Setter
	private List<OvhBandwidthArchiveOut> bandwidthArchiveOut = Collections.emptyList();

}

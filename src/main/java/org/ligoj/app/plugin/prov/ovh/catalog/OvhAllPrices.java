/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhAllPrices {

	private List<OvhProjectCreation> projectCreation = Collections.emptyList();

	private List<OvhStorage> storage = Collections.emptyList();

	private List<OvhArchive> archive = Collections.emptyList();

	private List<OvhBandwidthArchiveIn> bandwidthArchiveIn = Collections.emptyList();

	private List<OvhVolume> volumes = Collections.emptyList();

	private List<OvhFlavor> instances = Collections.emptyList();

	private List<OvhSnapshot> snapshots = Collections.emptyList();

	private List<OvhBandwidthStorage> bandwidthStorage = Collections.emptyList();

	private List<OvhBandwidthArchiveOut> bandwidthArchiveOut = Collections.emptyList();

}

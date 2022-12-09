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
	private List<OvhAttrInstance> storage = Collections.emptyList();

	@Getter
	@Setter
	private List<OvhAttrInstance> archive = Collections.emptyList();

	@Getter
	@Setter
	private List<OvhAttrInstance> volumes = Collections.emptyList();

	@Getter
	@Setter
	private List<OvhAttrInstance> instances = Collections.emptyList();

	@Getter
	@Setter
	private List<OvhAttrInstance> snapshots = Collections.emptyList();
	
	@Getter
	@Setter
	private List<OvhAttrInstance> databases = Collections.emptyList();

}

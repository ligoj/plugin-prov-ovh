/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhAllPrices {


	@Getter
	private final List<OvhAttrInstance> storage = new ArrayList<OvhAttrInstance>();

	@Getter
	private final List<OvhAttrInstance> archive = new ArrayList<OvhAttrInstance>();

	@Getter
	private final List<OvhAttrInstance> volumes = new ArrayList<OvhAttrInstance>();

	@Getter
	private final List<OvhAttrInstance> instances = new ArrayList<OvhAttrInstance>();

	@Getter
	private final List<OvhAttrInstance> snapshots = new ArrayList<OvhAttrInstance>();

	@Getter
	private final List<OvhAttrInstance> databases = new ArrayList<OvhAttrInstance>();

}

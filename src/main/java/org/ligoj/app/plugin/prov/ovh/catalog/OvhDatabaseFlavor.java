/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * OVH database flavor: database's type attributes.
 *
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhDatabaseFlavor {

	/**
	 * Flavor name.
	 */
	private String name;

	/**
	 * Amount of vCPUs.
	 */
	private int core;

	/**
	 * Memory size in GiB.
	 */
	private int memory;

	/**
	 * Storage size in GiB.
	 */
	private int storage;
}

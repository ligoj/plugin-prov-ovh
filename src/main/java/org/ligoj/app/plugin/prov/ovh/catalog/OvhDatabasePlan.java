/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * OVH database plan.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhDatabasePlan {

	/**
	 * Name
	 */
	private String name;

	/**
	 * Short description.
	 */
	private String description;

	/**
	 * Backup retrention code: <code>P2D</code>, <code>P30D</code>
	 */
	private String backupRetention;

}

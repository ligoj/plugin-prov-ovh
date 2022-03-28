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
public class OvhDatabases {

	private List<OvhDatabaseAvaibility> databaseAvaibility = Collections.emptyList();
	private List<OvhDatabaseCapabilities> databaseCapabilities = Collections.emptyList();

}

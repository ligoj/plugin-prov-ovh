/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * AWS prices JSON file structure.
 *
 * @param <T> The region pricing container type.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhPrices<T extends OvhRegionPrices> {

	private StorageConfig<T> config;

	/**
	 * The JSON storage configuration.
	 *
	 * @param <T> The region pricing type.
	 */
	@Getter
	@Setter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class StorageConfig<T> {
		private Collection<T> regions;
	}
}

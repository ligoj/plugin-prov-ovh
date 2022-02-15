/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.util.HashMap;
import java.util.Map;

import org.ligoj.app.plugin.prov.catalog.AbstractUpdateContext;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Context used to perform catalog update.
 */
@NoArgsConstructor
public class UpdateContext extends AbstractUpdateContext {

	/**
	 * Mapping from storage human name to API name.
	 */
	@Getter
	private final Map<String, String> mapStorageToApi = new HashMap<>();

	/**
	 * Mapping from Spot region name to API name.
	 */
	@Getter
	private final Map<String, String> mapSpotToNewRegion = new HashMap<>();

	/**
	 * Offers index.
	 */
	@Getter
	@Setter
	private Map<String, OvhPriceOffer> offers;

}

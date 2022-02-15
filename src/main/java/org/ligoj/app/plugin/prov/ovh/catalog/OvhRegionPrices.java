/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * The region container type.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhRegionPrices {
	private String region;
}

/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog.vm.ec2;

import java.util.Collection;

import org.ligoj.app.plugin.prov.ovh.catalog.OvhRegionPrices;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Spot region prices JSON file structure.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotRegion extends OvhRegionPrices {
	private Collection<SpotInstanceType> instanceTypes;

	/**
	 * The Spot prices container.
	 */
	@Getter
	@Setter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SpotInstanceType {
		private Collection<OvhEc2SpotPrice> sizes;
	}

}

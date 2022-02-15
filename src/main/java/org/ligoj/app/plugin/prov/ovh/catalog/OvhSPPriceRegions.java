/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Setter;

/**
 * Service regional price configuration.
 * 
 * @see {@link https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AWSLambda/current/region_index.json}
 * @see {@link https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AWSLambda/20210304163809/af-south-1/index.json}
 * @see {@link https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AWSLambda/20210304163809/af-south-1/index.csv}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhSPPriceRegions implements RegionalPrices {

	/**
	 * AWS service code.
	 */
	@Setter
	private List<OvhPriceRegion> regions;

	@Override
	public Map<String, OvhPriceRegion> getPRegions() {
		return regions.stream().collect(Collectors.toMap(OvhPriceRegion::getRegionCode, Function.identity()));
	}

}

/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.util.Map;

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
public class AwsPriceRegions implements RegionalPrices {

	/**
	 * AWS service code.
	 */
	@Setter
	private Map<String, AwsPriceRegion> regions;

	@Override
	public Map<String, AwsPriceRegion> getPRegions() {
		return regions;
	}

}

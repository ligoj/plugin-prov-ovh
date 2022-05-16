/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhInstancePrice {

	@Getter
	private double monthlyPriceValue;

	@Getter
	private double priceValue;

	@Getter
	@Setter
	private String region;

	@Setter
	@Getter
	private String flavorId;

	@JsonProperty("monthlyPrice")
	private void getMonthlyPrice(final Map<String, Object> monthlyPrice) {
		this.monthlyPriceValue = ((Number) monthlyPrice.get("value")).doubleValue();
	}

	@JsonProperty("price")
	private void getPrice(final Map<String, Object> price) {
		this.priceValue = (double) price.get("value");
	}

}

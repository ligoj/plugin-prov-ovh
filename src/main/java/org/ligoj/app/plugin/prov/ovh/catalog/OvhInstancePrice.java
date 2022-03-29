/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhInstancePrice {

	private double monthlyPriceValue;

	private String monthlyPriceCurrencyCode;

	private double priceValue;

	private String priceCurrencyCode;

	private String region;

	private String flavorId;

	private String flavorName;

	@JsonProperty("monthlyPrice")
	private void getMonthlyPrice(final Map<String, Object> monthlyPrice) {
		this.monthlyPriceValue = ((Number) monthlyPrice.get("value")).doubleValue();
		this.monthlyPriceCurrencyCode = (String) monthlyPrice.get("currencyCode");
	}

	@JsonProperty("price")
	private void getPrice(final Map<String, Object> price) {
		this.priceValue = (double) price.get("value");
		this.priceCurrencyCode = (String) price.get("currencyCode");
	}

}

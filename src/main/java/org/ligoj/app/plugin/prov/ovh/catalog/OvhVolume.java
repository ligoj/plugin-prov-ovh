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
public class OvhVolume {

	private double monthlyPriceValue;

	private String monthlyPriceCurrencyCode;

	private String priceValue;

	private String priceCurrencyCode;

	@JsonProperty("region")
	private String region;

	@JsonProperty("volumeName")
	private String volumeName;

	@JsonProperty("monthlyPrice")
	private void getMonthlyPrice(Map<String, Object> monthlyPrice) {
		this.monthlyPriceValue = (double) monthlyPrice.get("value");
		this.monthlyPriceCurrencyCode = (String) monthlyPrice.get("currencyCode");
	}

	@JsonProperty("price")
	private void getPrice(Map<String, String> price) {
		this.priceValue = price.get("value");
		this.priceCurrencyCode = price.get("currencyCode");
	}
}

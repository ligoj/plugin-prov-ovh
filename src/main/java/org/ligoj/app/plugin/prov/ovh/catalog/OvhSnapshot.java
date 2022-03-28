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
public class OvhSnapshot {

	private double monthlyPriceValue;

	private String monthlyPriceCurrencyCode;

	private String value;

	private String currencyCode;

	@JsonProperty("region")
	private String region;

	@JsonProperty("monthlyPrice")
	private void getMonthlyPrice(Map<String, Object> monthlyPrice) {
		this.monthlyPriceValue = (double) monthlyPrice.get("value");
		this.monthlyPriceCurrencyCode = (String) monthlyPrice.get("currencyCode");
	}

	@JsonProperty("price")
	private void getPrice(Map<String, String> price) {
		this.value = (String) price.get("value");
		this.currencyCode = (String) price.get("currencyCode");
	}

}

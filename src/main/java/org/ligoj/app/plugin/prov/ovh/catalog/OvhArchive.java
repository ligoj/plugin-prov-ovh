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
public class OvhArchive {

	private double monthlyPrice;

	private String currencyCode;

	@JsonProperty("region")
	private String region;

	@JsonProperty("monthlyPrice")
	private void getMonthlyPrice(Map<String, Object> monthlyPrice) {
		this.monthlyPrice = (double) monthlyPrice.get("value");
		this.currencyCode = (String) monthlyPrice.get("currencyCode");
	}

}

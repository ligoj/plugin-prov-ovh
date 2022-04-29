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
public class OvhDatabase {

	private Double monthlyPrice;

	private Double hourlyPrice;

	private String term;

	private String planCode;

	@JsonProperty("regions")
	private String regions;

	@JsonProperty("all")
	private void getMonthlyPrice(Map<String, Object> allPrice) {
		String mPrice =  (String) allPrice.get("monthly");
		this.monthlyPrice = Double.parseDouble(mPrice.replace("$", "").replace(",", ""));
		String hPrice =  (String) allPrice.get("monthly");
		this.hourlyPrice = Double.parseDouble(hPrice.replace("$", "").replace(",", ""));
	}
}

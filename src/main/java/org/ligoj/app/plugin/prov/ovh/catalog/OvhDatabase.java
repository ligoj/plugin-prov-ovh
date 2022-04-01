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

	private double monthlyPrice;

	private double hourlyPrice;

	private String term;

	private String planCode;

	@JsonProperty("regions")
	private String regions;

	@JsonProperty("all")
	private void getMonthlyPrice(Map<String, Object> allPrice) {
		this.monthlyPrice = (double) allPrice.get("monthly");
		this.hourlyPrice = (double) allPrice.get("hourly");
	}
}

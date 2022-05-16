/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * OVH database price details. All prices are excluding VAT. To convert HTML prices to exepected JSON price: <code>
 *
location = 'https://www.ovhcloud.com/fr/public-cloud/prices/';
var $ = jQuery;
JSON.stringify($.makeArray(jQuery('tr[data-price]'))
.filter(tr => $(tr).attr('data-planCode').match(/databases.+/))
.map(tr => (
    {
        ...JSON.parse(decodeURIComponent($(tr).attr('data-price'))),
        regions: $(tr).attr('data-regions'),
        planCode: $(tr).attr('data-planCode')
    }
)));</code>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhDatabasePrice {

	/**
	 * Monthly price,
	 */
	@Getter
	private Double monthlyPrice;

	/**
	 * Hourly price.
	 */
	@Getter
	private Double hourlyPrice;

	/**
	 * Term code code corresponding to the commitment.
	 */
	@Getter
	@Setter
	private String term;

	/**
	 * Plan's code built like this : <code>databases.$ENGINE-$PLAN-$FLAVOR.hour.consumption</code>.
	 */
	@Getter
	@Setter
	private String planCode;

	@JsonProperty("all")
	private void getMonthlyPrice(Map<String, Object> allPrice) {
		final var monthlyPrice = (String) allPrice.get("monthly");
		this.monthlyPrice = Double.parseDouble(monthlyPrice.replaceAll("[^0-9.]", "").trim());
		final var hourlyPrice = (String) allPrice.get("monthly");
		this.hourlyPrice = Double.parseDouble(hourlyPrice.replaceAll("[^0-9.]", "").trim());
	}
}

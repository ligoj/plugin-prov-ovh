/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OvhStorage {

	private String region;

	@Setter
	@JsonIgnore
	private double price;

	@JsonProperty("monthlyPrice")
	private void getMonthlyPrice(final Map<String, Object> monthlyPrice) {
		this.price = (double) monthlyPrice.get("value");
	}

}

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
public class OvhFlavor  {
	
	
    private String monthlyPriceValue;
	
	private String monthlyPriceCurrencyCode;
	
	private double priceValue;
	
	private String priceCurrencyCode;
	
	@JsonProperty("region")
	private String region;
		
	@JsonProperty("flavorId")
	private String flavorId;
	
	@JsonProperty("flavorName")
	private String flavorName;
	

    @JsonProperty("monthlyPrice")
    private void getMonthlyPrice(Map<String,String> monthlyPrice) {
        this.monthlyPriceValue = (String)monthlyPrice.get("value");
        this.monthlyPriceCurrencyCode = (String)monthlyPrice.get("currencyCode");
    }
	
    @JsonProperty("price")
    private void getPrice(Map<String,Object> price) {
        this.priceValue = (double)price.get("value");
        this.priceCurrencyCode = (String)price.get("currencyCode");
    }

}

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
public class OvhBandwidthArchiveOut  {
	
	private double value;
	
	private String currencyCode;
	
	@JsonProperty("region")
	private String region;
	
    @JsonProperty("price")
    private void getPrice(Map<String,Object> price) {
        this.value = (double)price.get("value");
        this.currencyCode = (String)price.get("currencyCode");
    }

}

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
 * OVH price details. All prices are excluding VAT. To convert HTML prices to exepected JSON price: <code>
 *
*/

public class OvhAttrInstance {
 
    /**
     * Monthly price,
     */
    @Getter
    @Setter
    private Double monthlyCost;
 
    /**
     * Hourly price.
     */
    @Getter
    @Setter
    private Double hourlyCost;
    
    /**
     * Monthly windows price,
     */
    @Getter
    @Setter
    private Double monthlyWindowsCost;
 
    /**
     * Hourly windows price.
     */
    @Getter
    @Setter
    private Double hourlyWindowsCost;
    
    /**
     * Monthly linux price,
     */
    @Getter
    @Setter
    private Double monthlyLinuxCost;
 
    /**
     * Hourly linux  price.
     */
    @Getter
    @Setter
    private Double hourlyLinuxCost;
 
    /**
     * Plan's code built like this : <code>databases.$ENGINE-$PLAN-$FLAVOR.hour.consumption</code>.
     */
    @Getter
    @Setter
    private String planCode;
 
    /**
     * Region code, lower case.
     */
    @Getter
    @Setter
    private String region;
    
    /**
     * price
     */
    @Getter
    @Setter
    private Double price;
    
    /**
    * Name
    */
    @Getter
    @Setter
   private String name;

   /**
    * RAM
    */
   @Getter
   @Setter
   private Double RAM;
   
   /**
    * GPU
    */
   @Setter
   private String GPU;
   
   /**
    * CPU
    */
   @Getter
   @Setter
   private Double CPU;
   
   /**
    * Storage
    */
   @Getter
   @Setter
   private String storage;
   
   /**
    * publicNetwork
    */
   @Getter
   @Setter
   private String publicNetwork;
   
   /**
    * privateNetwork
    */
   @Getter
   @Setter
   private String privateNetwork;
   
   /**
    * dedicatedNode
    */
   @Getter
   @Setter
   private String dedicatedNode;
   
   /**
    * NVMeDisks
    */
   @Setter
   private String NVMeDisks;
   
   /**
    * engine
    */
   @Getter
   @Setter
   private String engine;
   
   /**
    * plan
    */
   @Getter
   @Setter
   private String plan;
   
   /**
    * flavor
    */
   @Getter
   @Setter
   private String flavor;
   

}

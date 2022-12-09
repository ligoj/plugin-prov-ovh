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
@Getter
@Setter
public class OvhAttrInstance {
 
    /**
     * Monthly price,
     */
    private Double monthlyCost;
 
    /**
     * Hourly price.
     */
    private Double hourlyCost;
    
    /**
     * Monthly windows price,
     */
    private Double monthlyWindowsCost;
 
    /**
     * Hourly windows price.
     */
    private Double hourlyWindowsCost;
    
    /**
     * Monthly linux price,
     */
    private Double monthlyLinuxCost;
 
    /**
     * Hourly linux  price.
     */
    private Double hourlyLinuxCost;
 
    /**
     * Plan's code built like this : <code>databases.$ENGINE-$PLAN-$FLAVOR.hour.consumption</code>.
     */
    private String planCode;
 
    /**
     * Region code, lower case.
     */
    private String region;
    
    /**
     * price
     */
    private Double price;
    
    /**
    * Name
    */
   private String name;

   /**
    * RAM
    */
   private Double RAM;
   
   /**
    * GPU
    */
   private String GPU;
   
   /**
    * CPU
    */
   private Double CPU;
   
   /**
    * Storage
    */
   private String storage;
   
   /**
    * publicNetwork
    */
   private String publicNetwork;
   
   /**
    * privateNetwork
    */
   private String privateNetwork;
   
   /**
    * dedicatedNode
    */
   private String dedicatedNode;
   
   /**
    * NVMeDisks
    */
   private String NVMeDisks;
   
   /**
    * engine
    */
   private String engine;
   
   /**
    * plan
    */
   private String plan;
   
   /**
    * flavor
    */
   private String flavor;
   

}

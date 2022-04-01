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
public class OvhDatabaseCapabilities {

	private String nameEngine;

	private Object versions;
	
	private String defaultVersion;
	
	private String descriptionEngine;
	
	private Object sslModes;	
	
	private String namePlan;

	private String descriptionPlan;
	
	private String backupRetention;
		
	private String nameFlavor;

	private int core;
	
	private int memory;
	
	private int storage;
	
	private String nameOption;

	private String type;
	
	@JsonProperty("engines")
	private void getEngine(Map<String, Object> engine) {
		this.nameEngine = (String) engine.get("name");
		this.versions = engine.get("versions");
		this.defaultVersion = (String) engine.get("defaultVersion");
		this.descriptionEngine = (String) engine.get("description");
		this.sslModes = engine.get("sslModes");
	}
	
	@JsonProperty("plans")
	private void getPlan(Map<String, Object> plan) {
		this.namePlan = (String) plan.get("name");
		this.descriptionPlan = (String)plan.get("description");
		this.backupRetention = (String) plan.get("backupRetention");
	}
	
	@JsonProperty("flavors")
	private void getFlavor(Map<String, Object> flavor) {
		this.nameFlavor = (String) flavor.get("name");
		this.core = (int)flavor.get("core");
		this.memory = (int) flavor.get("memory");
		this.storage = (int) flavor.get("storage");
	}

	@JsonProperty("options")
	private void getOption(Map<String, Object> Option) {
		this.nameOption = (String) Option.get("name");
		this.type = (String)Option.get("type");
	}

}

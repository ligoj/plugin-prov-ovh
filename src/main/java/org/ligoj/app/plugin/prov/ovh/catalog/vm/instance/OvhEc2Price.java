/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog.vm.instance;

import lombok.Getter;
import lombok.Setter;

/**
 * AWS EC2 price configuration
 */
@Getter
@Setter
public class OvhEc2Price extends AbstractOvhVmOsPrice {

	private String software;
	
	/**
	 * API Volume type. Not <code>null</code> for EBS price.
	 */
	private String volume;
	
	private String capacityStatus;
}

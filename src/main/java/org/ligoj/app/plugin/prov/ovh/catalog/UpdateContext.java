/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.util.HashSet;
import java.util.Set;

import org.ligoj.app.plugin.prov.catalog.AbstractUpdateContext;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Context used to perform catalog update.
 */
@NoArgsConstructor
public class UpdateContext extends AbstractUpdateContext {

	/**
	 * Used regions identifier (lower case) by at least one instance price. This set is iterated by the process
	 * importing databases prices. This mecanism is required because database prices refers to global region only like
	 * <code>GRA</code>, not specific datacenter like <code>GRA7</code>
	 */
	@Getter
	private Set<String> usedRegions = new HashSet<>();

}

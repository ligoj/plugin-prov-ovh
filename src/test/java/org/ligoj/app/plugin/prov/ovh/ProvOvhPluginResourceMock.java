/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh;

/**
 * Configuration class used to mock AWS calls
 */
public class ProvOvhPluginResourceMock extends ProvOvhPluginResource {
	@Override
	public boolean validateAccess(int subscription) {
		return true;
	}

}

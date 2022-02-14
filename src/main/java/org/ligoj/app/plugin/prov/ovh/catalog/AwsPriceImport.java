/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.io.IOException;
import java.net.URISyntaxException;

import org.ligoj.app.plugin.prov.catalog.AbstractImportCatalogResource;
import org.ligoj.app.plugin.prov.ovh.ProvAwsPluginResource;
import org.ligoj.app.plugin.prov.ovh.catalog.vm.ec2.AwsPriceImportEc2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Setter;

/**
 * The provisioning price service for AWS. Manage install or update of prices.
 */
@Component
@Setter
public class AwsPriceImport extends AbstractImportCatalogResource {

	@Autowired
	private AwsPriceImportBase base;

	@Autowired
	private AwsPriceImportEc2 ec2;

	/**
	 * Install or update prices.
	 *
	 * @param force When <code>true</code>, all cost attributes are update.
	 * @throws IOException        When CSV or XML files cannot be read.
	 * @throws URISyntaxException When CSV or XML files cannot be read.
	 */
	public void install(final boolean force) throws IOException, URISyntaxException {
		final var context = initContext(new UpdateContext(), ProvAwsPluginResource.KEY, force);

		base.install(context);
		ec2.install(context);
		context.cleanup();
	}
}

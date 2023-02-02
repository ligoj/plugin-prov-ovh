/*

 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.codec.digest.DigestUtils;
import org.ligoj.app.api.SubscriptionStatusWithData;
import org.ligoj.app.plugin.prov.AbstractProvResource;
import org.ligoj.app.plugin.prov.ProvResource;
import org.ligoj.app.plugin.prov.catalog.ImportCatalogService;
import org.ligoj.app.plugin.prov.ovh.catalog.OvhPriceImport;
import org.ligoj.bootstrap.core.curl.CurlProcessor;
import org.ligoj.bootstrap.core.curl.CurlRequest;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The provisioning service for OVH. There is complete quote configuration along the subscription.
 */
@Service
@Path(ProvOvhPluginResource.URL)
@Produces(MediaType.APPLICATION_JSON)
public class ProvOvhPluginResource extends AbstractProvResource implements ImportCatalogService {

	/**
	 * Plug-in key.
	 */
	public static final String URL = ProvResource.SERVICE_URL + "/ovh";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);

	/**
	 * Configuration key used for {@link #DEFAULT_REGION}
	 */
	public static final String CONF_REGION = KEY + ":region";

	/**
	 * Parameter used for OVH authentication
	 */
	public static final String PARAMETER_APP_KEY = KEY +":app-key-id";

	/**
	 * Parameter used for OVH authentication
	 */
	public static final String PARAMETER_APP_SECRET = KEY +":app-secret";

	/**
	 * OVH Consumer key
	 */
	public static final String PARAMETER_CONSUMER_KEY = KEY +":consumer-key";
	/**
	 * OVH Service Name
	 */
	public static final String PARAMETER_SERVICE_NAME =  KEY +":service-name";

	/**
	 * Default OVH Service endpoint
	 */
	public static final String ENDPOINT = "https://eu.api.ovh.com/1.0";

	/**
	 * Configuration key used for enabled instance type pattern names. When value is <code>null</code>, no restriction.
	 */
	public static final String CONF_ITYPE = ProvOvhPluginResource.KEY + ":instance-type";

	/**
	 * Configuration key used for enabled database type pattern names. When value is <code>null</code>, no restriction.
	 */
	public static final String CONF_DTYPE = ProvOvhPluginResource.KEY + ":database-type";
	/**
	 * Configuration key used for enabled database engine pattern names. When value is <code>null</code>, no
	 * restriction.
	 */
	public static final String CONF_ENGINE = ProvOvhPluginResource.KEY + ":database-engine";

	/**
	 * Configuration key used for enabled OS pattern names. When value is <code>null</code>, no restriction.
	 */
	public static final String CONF_OS = ProvOvhPluginResource.KEY + ":os";

	@Autowired
	protected OvhPriceImport priceImport;

	@Override
	public String getKey() {
		return KEY;
	}

	/**
	 * Check OVH connection and account.
	 *
	 * @param node       The node identifier. May be <code>null</code>.
	 * @param parameters the parameter values of the node.
	 * @return <code>true</code> if OVH connection is up
	 */
	@Override
	public boolean checkStatus(final String node, final Map<String, String> parameters) {
		return validateAccess(parameters);
	}

	@Override
	public void create(final int subscription) {
		if (!validateAccess(subscription)) {
			throw new BusinessException("Cannot access to OVH services with these parameters");
		}
	}

	@Override
	public SubscriptionStatusWithData checkSubscriptionStatus(final int subscription, final String node,
			final Map<String, String> parameters) {
		// Validate the account
		if (validateAccess(subscription)) {
			// Return the quote details
			return super.checkSubscriptionStatus(subscription, node, parameters);
		}
		return new SubscriptionStatusWithData(false);
	}

	/**
	 * Fetch the prices from the OVH server. Install or update the prices
	 */
	@Override
	public void install() throws IOException, URISyntaxException {
		priceImport.install(false);
	}

	@Override
	public void updateCatalog(final String node, final boolean force) throws IOException, URISyntaxException {
		// OVH catalog is shared with all instances, require tool level access
		nodeResource.checkWritableNode(KEY);
		priceImport.install(force);
	}

	/**
	 * Create Curl request for OVH service. Initialize default values for OVH secrets and compute signature.
	 *
	 * @param subscription Subscription's identifier.
	 * @return initialized request
	 */
	protected CurlRequest newRequest(final String query, final int subscription) {
		return newRequest(toUrl(query), subscriptionResource.getParameters(subscription));
	}

	/**
	 * Create Curl request for OVH service. Initialize default values for OVH secrets and compute signature.
	 *
	 * @param parameters Subscription's parameters.
	 * @return Initialized request.
	 */
	protected CurlRequest newRequest(final String query, final Map<String, String> parameters) {
		final var appKey = parameters.get(PARAMETER_APP_KEY);
		final var appSecret = parameters.get(PARAMETER_APP_SECRET);
		final var consumerKey = parameters.get(PARAMETER_CONSUMER_KEY);
		final var method = "GET";
		final var body = "";

		final var timestamp = System.currentTimeMillis() / 1000;

		// build signature
		final var toSign = new StringBuilder(appSecret).append("+").append(consumerKey).append("+").append(method)
				.append("+").append(query).append("+").append(body).append("+").append(timestamp).toString();
		final var signature = new StringBuilder("$1$").append(DigestUtils.sha1Hex(toSign)).toString();
		final var request = new CurlRequest(method, query, body);
		request.getHeaders()
				.putAll(Map.of("Content-Type", "application/json", "X-Ovh-Application", appKey, "X-Ovh-Consumer",
						consumerKey, "X-Ovh-Signature", signature, "X-Ovh-Timestamp", Long.toString(timestamp)));
		request.setSaveResponse(true);
		return request;
	}

	/**
	 * Check OVH connection and account.
	 *
	 * @param parameters Subscription parameters.
	 * @return <code>true</code> if OVH connection is up
	 */
	private boolean validateAccess(final Map<String, String> parameters) {
		final var query = "/cloud/project";
		try (var curlProcessor = new CurlProcessor()) {
			return curlProcessor.process(newRequest(toUrl(query), parameters));
		}
	}

	/**
	 * Return the full URL from a query.
	 *
	 * @param query Target remote query.
	 * @return The base host URL from a query.
	 */
	protected String toUrl(final String query) {
		return ENDPOINT + query;
	}

	/**
	 * Check OVH connection and account.
	 *
	 * @param subscription Subscription identifier.
	 * @return <code>true</code> if OVH connection is up
	 */
	public boolean validateAccess(final int subscription) {
		final var parameters = subscriptionResource.getParameters(subscription);
		final var query = "/cloud/project/" + parameters.get(PARAMETER_SERVICE_NAME) + "/region";
		try (var curlProcessor = new CurlProcessor()) {
			return curlProcessor.process(newRequest(query, subscription));
		}
	}

	@Override
	public String getName() {
		return "OVH";
	}}

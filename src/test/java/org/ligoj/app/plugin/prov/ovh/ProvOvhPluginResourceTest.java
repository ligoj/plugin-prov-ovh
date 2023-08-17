/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh;

import jakarta.transaction.Transactional;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractServerTest;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.iam.model.CacheUser;
import org.ligoj.app.model.*;
import org.ligoj.app.plugin.prov.model.ProvLocation;
import org.ligoj.app.plugin.prov.model.ProvQuote;
import org.ligoj.app.plugin.prov.ovh.catalog.OvhPriceImport;
import org.ligoj.bootstrap.core.curl.CurlRequest;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Test class of {@link ProvOvhPluginResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class ProvOvhPluginResourceTest extends AbstractServerTest {

	private static final String MOCK_URL = "http://localhost:" + MOCK_PORT + "/mock";

	protected int subscription;

	@Autowired
	private ProvOvhPluginResource resource;

	@BeforeEach
	void prepareData() throws IOException {
		persistSystemEntities();
		persistEntities("csv",
				new Class[]{Node.class, Project.class, CacheCompany.class, CacheUser.class, DelegateNode.class,
						Subscription.class, ProvLocation.class, ProvQuote.class, Parameter.class,
						ParameterValue.class},
				StandardCharsets.UTF_8);
		this.subscription = getSubscription("Jupiter");
		cacheManager.getCache("curl-tokens").clear();
	}

	@Test
	void getKey() {
		Assertions.assertEquals("service:prov:ovh", resource.getKey());
	}

	@Test
	void getName() {
		Assertions.assertEquals("OVH", resource.getName());
	}

	/**
	 * Return the subscription identifier of the given project. Assumes there is only one subscription for a service.
	 */
	private int getSubscription(final String project) {
		return getSubscription(project, ProvOvhPluginResource.KEY);
	}

	@Test
	void install() throws IOException, URISyntaxException {
		final var resource2 = new ProvOvhPluginResource();
		resource2.priceImport = Mockito.mock(OvhPriceImport.class);
		resource2.install();
	}

	@Test
	void updateCatalog() throws IOException, URISyntaxException {
		// Re-Install a new configuration
		final var resource2 = new ProvOvhPluginResource();
		super.applicationContext.getAutowireCapableBeanFactory().autowireBean(resource2);
		resource2.priceImport = Mockito.mock(OvhPriceImport.class);
		resource2.updateCatalog("service:prov:ovh:account", false);
		resource2.updateCatalog("service:prov:ovh:account", true);
	}

	@Test
	void updateCatalogNoRight() {
		initSpringSecurityContext("any");

		// Re-Install a new configuration
		Assertions.assertEquals("read-only-node", Assertions.assertThrows(BusinessException.class, () -> resource.updateCatalog("service:prov:ovh:account", false)).getMessage());
	}

	private ProvOvhPluginResource newSpyResource() {
		final var resource0 = new ProvOvhPluginResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource0);
		return Mockito.spy(resource0);
	}

	/**
	 * prepare call to AWS
	 */
	@Test
	void newRequest() {
		final var request = resource.newRequest("/", subscription);
		Assertions.assertTrue(request.getHeaders().containsKey("X-Ovh-Signature"));
		Assertions.assertEquals("https://eu.api.ovh.com/1.0/", request.getUrl());
		Assertions.assertEquals("GET", request.getMethod());
	}

	@Test
	void create() {
		final var resource = newSpyResource();
		Mockito.doReturn(true).when(resource).validateAccess(ArgumentMatchers.anyInt());
		resource.create(subscription);
	}

	@Test
	void createFailed() {
		final var resource = newSpyResource();
		Mockito.doReturn(false).when(resource).validateAccess(ArgumentMatchers.anyInt());
		Assertions.assertEquals("Cannot access to OVH services with these parameters",
				Assertions.assertThrows(BusinessException.class, () -> resource.create(-1)).getMessage());
	}

	@Test
	void checkSubscriptionStatusUp() {
		final var status = resource.checkSubscriptionStatus(subscription, null, new HashMap<>());
		Assertions.assertTrue(status.getStatus().isUp());
	}

	@Test
	void checkSubscriptionStatusDown() {
		final var resource = newSpyResource();
		Mockito.doReturn(false).when(resource).validateAccess(ArgumentMatchers.anyInt());
		final var status = resource.checkSubscriptionStatus(subscription, null, new HashMap<>());
		Assertions.assertFalse(status.getStatus().isUp());
	}

	@Test
	void validateAccessUp() {
		Assertions.assertTrue(validateAccess(HttpStatus.SC_OK));
	}

	@Test
	void validateAccessDown() {
		Assertions.assertFalse(validateAccess(HttpStatus.SC_FORBIDDEN));
	}

	@Test
	void checkStatus() {
		Assertions.assertTrue(validateAccess(HttpStatus.SC_OK));
		final var resource = newSpyResource();
		Mockito.doReturn(MOCK_URL).when(resource).toUrl(ArgumentMatchers.anyString());
		final var parameters = new HashMap<String, String>();
		parameters.put("service:prov:ovh:app-key-id", "SECRET1");
		parameters.put("service:prov:ovh:app-secret", "SECRET2");
		parameters.put("service:prov:ovh:consumer-key", "SECRET3");
		parameters.put("service:prov:ovh:service-name", "SECRET4");
		Assertions.assertTrue(resource.checkStatus(null, parameters));
	}

	@SuppressWarnings("unchecked")
	private boolean validateAccess(int status) {
		final var resource = newSpyResource();
		final var mockRequest = new CurlRequest("GET", MOCK_URL, null);
		mockRequest.setSaveResponse(true);
		Mockito.doReturn(mockRequest).when(resource).newRequest(ArgumentMatchers.anyString(),
				ArgumentMatchers.any(Map.class));

		httpServer.stubFor(get(urlEqualTo("/mock")).willReturn(aResponse().withStatus(status)));
		httpServer.start();
		return resource.validateAccess(subscription);
	}
}
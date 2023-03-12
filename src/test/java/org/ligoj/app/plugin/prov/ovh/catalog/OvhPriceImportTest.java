/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.ligoj.app.plugin.prov.quote.instance.QuoteInstanceQuery.builder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractServerTest;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.iam.model.CacheUser;
import org.ligoj.app.model.DelegateNode;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.plugin.prov.ProvResource;
import org.ligoj.app.plugin.prov.QuoteVo;
import org.ligoj.app.plugin.prov.catalog.AbstractImportCatalogResource;
import org.ligoj.app.plugin.prov.catalog.ImportCatalogResource;
import org.ligoj.app.plugin.prov.dao.ProvQuoteRepository;
import org.ligoj.app.plugin.prov.model.ProvLocation;
import org.ligoj.app.plugin.prov.model.ProvQuote;
import org.ligoj.app.plugin.prov.model.ProvQuoteInstance;
import org.ligoj.app.plugin.prov.model.ProvQuoteStorage;
import org.ligoj.app.plugin.prov.model.ProvStorageOptimized;
import org.ligoj.app.plugin.prov.model.ProvTenancy;
import org.ligoj.app.plugin.prov.model.ProvUsage;
import org.ligoj.app.plugin.prov.model.Rate;
import org.ligoj.app.plugin.prov.model.SupportType;
import org.ligoj.app.plugin.prov.model.VmOs;
import org.ligoj.app.plugin.prov.ovh.ProvOvhPluginResource;
import org.ligoj.app.plugin.prov.quote.database.ProvQuoteDatabaseResource;
import org.ligoj.app.plugin.prov.quote.database.QuoteDatabaseQuery;
import org.ligoj.app.plugin.prov.quote.instance.ProvQuoteInstanceResource;
import org.ligoj.app.plugin.prov.quote.instance.QuoteInstanceEditionVo;
import org.ligoj.app.plugin.prov.quote.storage.ProvQuoteStorageResource;
import org.ligoj.app.plugin.prov.quote.storage.QuoteStorageEditionVo;
import org.ligoj.app.plugin.prov.quote.storage.QuoteStorageQuery;
import org.ligoj.app.plugin.prov.quote.support.ProvQuoteSupportResource;
import org.ligoj.bootstrap.resource.system.configuration.ConfigurationResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link OvhPriceImport}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class OvhPriceImportTest extends AbstractServerTest {

	private static final double DELTA = 0.001;

	private OvhPriceImport resource;

	@Autowired
	private ProvResource provResource;

	@Autowired
	private ProvQuoteInstanceResource qiResource;

	@Autowired
	private ProvQuoteDatabaseResource qbResource;

	@Autowired
	private ProvQuoteStorageResource qsResource;

	@Autowired
	private ProvQuoteSupportResource qs2Resource;

	@Autowired
	private ProvQuoteRepository repository;

	@Autowired
	private ConfigurationResource configuration;

	protected int subscription;

	@BeforeEach
	void prepareData() throws IOException {
		persistSystemEntities();
		persistEntities("csv",
				new Class[]{Node.class, Project.class, CacheCompany.class, CacheUser.class, DelegateNode.class,
						Parameter.class, ProvLocation.class, Subscription.class, ParameterValue.class,
						ProvQuote.class},
				StandardCharsets.UTF_8.name());
		this.subscription = getSubscription("gStack");

		// Mock catalog import helper
		final var helper = new ImportCatalogResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(helper);
		this.resource = initCatalog(helper, new OvhPriceImport());

		clearAllCache();
		initSpringSecurityContext(DEFAULT_USER);
		resetImportTask();

		final var usage12 = new ProvUsage();
		usage12.setName("12month");
		usage12.setRate(100);
		usage12.setDuration(12);
		usage12.setConfiguration(repository.findBy("subscription.id", subscription));
		em.persist(usage12);

		final var usage36 = new ProvUsage();
		usage36.setName("36month");
		usage36.setRate(100);
		usage36.setDuration(36);
		usage36.setConfiguration(repository.findBy("subscription.id", subscription));
		em.persist(usage36);

		final var usageDev = new ProvUsage();
		usageDev.setName("dev");
		usageDev.setRate(30);
		usageDev.setDuration(1);
		usageDev.setConfiguration(repository.findBy("subscription.id", subscription));
		em.persist(usageDev);
		em.flush();
		em.clear();
	}

	private <T extends AbstractImportCatalogResource> T initCatalog(ImportCatalogResource importHelper, T catalog) {
		applicationContext.getAutowireCapableBeanFactory().autowireBean(catalog);
		catalog.setImportCatalogResource(importHelper);
		MethodUtils.getMethodsListWithAnnotation(catalog.getClass(), PostConstruct.class).forEach(m -> {
			try {
				m.invoke(catalog);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// Ignore;
			}
		});
		return catalog;
	}

	private void resetImportTask() {
		this.resource.getImportCatalogResource().endTask("service:prov:ovh", false);
		this.resource.getImportCatalogResource().startTask("service:prov:ovh", t -> {
			t.setLocation(null);
			t.setNbPrices(0);
			t.setNbTypes(0);
			t.setWorkload(0);
			t.setDone(0);
			t.setPhase(null);
		});
	}

	@Test
	void installOffLine() throws Exception {

		configuration.put(OvhPriceImport.CONF_DTYPE, ".*(db1|db2).*");
		configuration.put(OvhPriceImport.CONF_OS, "(WINDOWS|LINUX|CENTOS)");
		configuration.put(OvhPriceImport.CONF_ITYPE, ".*-.*");
		configuration.put(OvhPriceImport.CONF_ENGINE,
				"(mongodb|mysql|postgresql|kafka|redis|opensearch|kafkaMirrorMaker).*");
		configuration.put(OvhPriceImport.CONF_FLAVOR, "(db1|db2).*");

		// Install a new configuration
		final var quote = install();

		// Check the whole quote
		final var instance = check(quote, 1302.0d, 2602.6d, 1299.4d);
		// Check the 3 years term
		var lookup = qiResource.lookup(instance.getConfiguration().getSubscription().getId(),
				builder().cpu(7).ram(1741).usage("36month").build());
		Assertions.assertEquals(112d, lookup.getCost(), DELTA);
		Assertions.assertEquals(112d, lookup.getPrice().getCost(), DELTA);
		Assertions.assertEquals(112d, lookup.getPrice().getCostPeriod(), DELTA);
		Assertions.assertEquals("monthly.postpaid", lookup.getPrice().getTerm().getCode());// monthly
		Assertions.assertFalse(lookup.getPrice().getTerm().isEphemeral());
		Assertions.assertEquals(1.0, lookup.getPrice().getPeriod(), DELTA);
		Assertions.assertEquals("linux/gra/monthly.postpaid/b2-30", lookup.getPrice().getCode());
		Assertions.assertEquals("b2-30", lookup.getPrice().getType().getCode());
		Assertions.assertEquals("gra", lookup.getPrice().getLocation().getName());
		Assertions.assertEquals("Gravelines", lookup.getPrice().getLocation().getDescription());
		checkImportStatus();

		// CPU Intensive
		lookup = qiResource.lookup(instance.getConfiguration().getSubscription().getId(),
				builder().cpu(2).ram(4096).build());
		Assertions.assertEquals("linux/gra/monthly.postpaid/d2-4", lookup.getPrice().getCode());

		// General Purpose
		lookup = qiResource.lookup(instance.getConfiguration().getSubscription().getId(),
				builder().cpu(2).ram(8000).build());
		Assertions.assertEquals("linux/gra/monthly.postpaid/d2-8", lookup.getPrice().getCode());

		// Install again to check the update without change
		resetImportTask();
		resource.install(false);
		provResource.updateCost(subscription);
		check(provResource.getConfiguration(subscription), 1302.0d, 2602.6d, 1299.4d);
		checkImportStatus();

		// Now, change a price within the remote catalog

		// Point to another catalog with different prices
		configuration.put(OvhPriceImport.CONF_API_PRICES, "http://localhost:" + MOCK_PORT + "/v2");

		// Install the new catalog, update occurs
		resetImportTask();
		resource.install(false);
		provResource.updateCost(subscription);

		// Check the new price
		final var newQuote = provResource.getConfiguration(subscription);
		Assertions.assertEquals(1302.0d, newQuote.getCost().getMin(), DELTA);

		// Compute price is updated
		final var instance2 = newQuote.getInstances().get(0);
		Assertions.assertEquals(1299.4d, instance2.getCost(), DELTA);

		// Check status
		checkImportStatus();

		// Check the support
		Assertions.assertEquals(1, qs2Resource
				.lookup(subscription, 0, SupportType.ALL, SupportType.ALL, SupportType.ALL, SupportType.ALL, Rate.BEST)
				.size());

		final var lookupSu = qs2Resource
				.lookup(subscription, 0, null, SupportType.ALL, SupportType.ALL, SupportType.ALL, Rate.BEST).get(0);
		Assertions.assertEquals("Enterprise", lookupSu.getPrice().getType().getName());
		Assertions.assertEquals(5850.0d, lookupSu.getCost(), DELTA);

		// Check the database
		var lookupB = qbResource.lookup(subscription, QuoteDatabaseQuery.builder().cpu(1).engine("MYSQL").build());
		Assertions.assertNull(lookupB.getPrice().getEdition());
		Assertions.assertEquals("gra/monthly.postpaid/mysql-essential-db1-4", lookupB.getPrice().getCode());
		Assertions.assertEquals(4096.0, lookupB.getPrice().getType().getRam());
		Assertions.assertEquals(2.0, lookupB.getPrice().getType().getCpu());
		Assertions.assertNull(lookupB.getPrice().getStorageEngine());

	}

	private void checkImportStatus() {
		final var status = this.resource.getImportCatalogResource().getTask("service:prov:ovh");
		Assertions.assertEquals(7, status.getDone());
		Assertions.assertEquals(6, status.getWorkload());
		Assertions.assertEquals("support", status.getPhase());
		Assertions.assertEquals(DEFAULT_USER, status.getAuthor());
		Assertions.assertTrue(status.getNbPrices().intValue() >= 18);//18
		Assertions.assertTrue(status.getNbTypes().intValue() >= 9);//9
		Assertions.assertTrue(status.getNbLocations() >= 1);//1
	}

	private void mockServer() throws IOException {
		configuration.put(OvhPriceImport.CONF_API_PRICES, "http://localhost:" + MOCK_PORT);
		httpServer.stubFor(get(urlEqualTo("/price.json")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(
				IOUtils.toString(new ClassPathResource("mock-server/ovh/prices.json").getInputStream(), "UTF-8"))));
		httpServer.stubFor(get(urlEqualTo("/flavor.json")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(
				IOUtils.toString(new ClassPathResource("mock-server/ovh/flavors.json").getInputStream(), "UTF-8"))));
		httpServer.stubFor(
				get(urlEqualTo("/database-availability.json")).willReturn(aResponse().withStatus(HttpStatus.SC_OK)
						.withBody(IOUtils.toString(
								new ClassPathResource("mock-server/ovh/database-availability.json").getInputStream(),
								"UTF-8"))));
		httpServer.stubFor(
				get(urlEqualTo("/database-capabilities.json")).willReturn(aResponse().withStatus(HttpStatus.SC_OK)
						.withBody(IOUtils.toString(
								new ClassPathResource("mock-server/ovh/database-capabilities.json").getInputStream(),
								"UTF-8"))));
		httpServer.stubFor(get(urlEqualTo("/database-price.json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(IOUtils.toString(
						new ClassPathResource("mock-server/ovh/database-price.json").getInputStream(), "UTF-8"))));

		httpServer.stubFor(
				get(urlEqualTo("/v2/price.json")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(IOUtils
						.toString(new ClassPathResource("mock-server/ovh/v2/prices.json").getInputStream(), "UTF-8"))));
		httpServer.stubFor(get(urlEqualTo("/v2/flavor.json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(IOUtils.toString(
						new ClassPathResource("mock-server/ovh/v2/flavors.json").getInputStream(), "UTF-8"))));

		httpServer.stubFor(
				get(urlEqualTo("/v2/database-availability.json")).willReturn(aResponse().withStatus(HttpStatus.SC_OK)
						.withBody(IOUtils.toString(
								new ClassPathResource("mock-server/ovh/v2/database-availability.json").getInputStream(),
								"UTF-8"))));
		httpServer.stubFor(
				get(urlEqualTo("/v2/database-capabilities.json")).willReturn(aResponse().withStatus(HttpStatus.SC_OK)
						.withBody(IOUtils.toString(
								new ClassPathResource("mock-server/ovh/v2/database-capabilities.json").getInputStream(),
								"UTF-8"))));
		httpServer.stubFor(get(urlEqualTo("/v2/database-price.json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(IOUtils.toString(
						new ClassPathResource("mock-server/ovh/v2/database-price.json").getInputStream(), "UTF-8"))));

		httpServer.start();
	}

	private ProvQuoteInstance check(final QuoteVo quote, final double minCost, final double maxCost,
			final double instanceCost) {
		Assertions.assertEquals(minCost, quote.getCost().getMin(), DELTA);
		Assertions.assertEquals(maxCost, quote.getCost().getMax(), DELTA);
		checkStorage(quote.getStorages());
		return checkInstance(quote.getInstances().get(0), instanceCost);
	}

	private ProvQuoteInstance checkInstance(final ProvQuoteInstance instance, final double cost) {
		Assertions.assertEquals(cost, instance.getCost(), DELTA);
		final var price = instance.getPrice();
		Assertions.assertEquals(0, price.getInitialCost());
		Assertions.assertEquals(VmOs.LINUX, price.getOs());
		Assertions.assertEquals(ProvTenancy.SHARED, price.getTenancy());
		Assertions.assertEquals(1299.4d, price.getCost(), DELTA);
		Assertions.assertEquals(1299.4d, price.getCostPeriod(), DELTA);
		Assertions.assertEquals(0.0, price.getPeriod(), DELTA);
		final var term = price.getTerm();
		Assertions.assertEquals("consumption", term.getCode());
		Assertions.assertEquals("consumption", term.getName());
		Assertions.assertFalse(term.isEphemeral());
		Assertions.assertEquals(0.0, term.getPeriod());// 1
		Assertions.assertEquals("c2-120", price.getType().getCode());
		Assertions.assertEquals("c2-120", price.getType().getName());
		Assertions.assertEquals("{Disk: 400 GB SSD}", price.getType().getDescription());
		Assertions.assertNull(price.getType().getProcessor());
		Assertions.assertFalse(price.getType().isAutoScale());

		return instance;
	}

	private ProvQuoteStorage checkStorage(final List<ProvQuoteStorage> storages) {
		var volume = storages.get(0);
		Assertions.assertEquals(1.2d, volume.getCost(), DELTA);
		Assertions.assertEquals(100, volume.getSize(), DELTA);
		Assertions.assertNotNull(volume.getQuoteInstance());
		final var typeV = volume.getPrice().getType();
		Assertions.assertEquals("volume", typeV.getCode());
		Assertions.assertEquals("volume", typeV.getName());
		Assertions.assertEquals(7500, typeV.getIops());
		Assertions.assertEquals(300, typeV.getThroughput());
		Assertions.assertEquals(0d, volume.getPrice().getCostTransaction(), DELTA);
		Assertions.assertEquals(1, typeV.getMinimal());
		Assertions.assertEquals(4096, typeV.getMaximal().intValue());
		Assertions.assertEquals(Rate.BEST, typeV.getLatency());
		Assertions.assertEquals(ProvStorageOptimized.IOPS, typeV.getOptimized());

		var storage = storages.get(1);
		Assertions.assertEquals(1.2d, storage.getCost(), DELTA);
		Assertions.assertEquals(100, storage.getSize(), DELTA);
		final var typeS = storage.getPrice().getType();
		Assertions.assertEquals("storage", typeS.getCode());
		Assertions.assertEquals("Object Storage", typeS.getName());
		Assertions.assertEquals(5000, typeS.getIops());
		Assertions.assertEquals(200, typeS.getThroughput());
		Assertions.assertEquals(0d, storage.getPrice().getCostTransaction(), DELTA);
		Assertions.assertEquals(1d, typeS.getMinimal());
		Assertions.assertEquals(Rate.GOOD, typeS.getLatency());
		Assertions.assertEquals(ProvStorageOptimized.DURABILITY, typeS.getOptimized());

		return volume;
	}

	/**
	 * Common offline install and configuring an instance
	 *
	 * @return The new quote from the installed
	 */
	private QuoteVo install() throws Exception {
		mockServer();

		// Check the basic quote
		return installAndConfigure();
	}

	@Test
	void installOnLine() throws Exception {
		configuration.delete(OvhPriceImport.CONF_API_PRICES);
		configuration.put(OvhPriceImport.CONF_REGIONS, "(gra|sbg|waw).*");
		configuration.put(OvhPriceImport.CONF_ITYPE, ".*");
		configuration.put(OvhPriceImport.CONF_DTYPE, ".*");
		configuration.put(OvhPriceImport.CONF_ENGINE, "(MYSQL)");
		configuration.put(OvhPriceImport.CONF_OS, "(WINDOWS|LINUX|CENTOS)");
		configuration.put(OvhPriceImport.CONF_FLAVOR, ".*");

		final var quote = installAndConfigure();
		Assertions.assertTrue(quote.getCost().getMin() >= 1186.21);
	}

	/**
	 * Install and check
	 */
	private QuoteVo installAndConfigure() throws IOException, Exception {
		resource.install(false);
		em.flush();
		em.clear();
		Assertions.assertEquals(0, provResource.getConfiguration(subscription).getCost().getMin(), DELTA); // 0

		var lookup = qiResource.lookup(subscription,
				builder().cpu(2).ram(15000).os(VmOs.WINDOWS).location("sbg").usage("36month").build());

		Assertions.assertEquals("windows/sbg/monthly.postpaid/b2-15", lookup.getPrice().getCode());

		// Request an instance for a generic Linux OS
		lookup = qiResource.lookup(subscription,
				builder().type("c2-120").os(VmOs.LINUX).location("gra").usage("dev").build());
		Assertions.assertEquals("linux/gra/consumption/c2-120", lookup.getPrice().getCode());

		// New instance for "s-1vcpu-1gb"
		var ivo = new QuoteInstanceEditionVo();
		ivo.setCpu(1d);
		ivo.setRam(1);
		ivo.setLocation("gra");
		ivo.setPrice(lookup.getPrice().getId());
		ivo.setName("server1");
		ivo.setMaxQuantity(2);
		ivo.setSubscription(subscription);
		var createInstance = qiResource.create(ivo);
		Assertions.assertTrue(createInstance.getTotal().getMin() >= 1);
		Assertions.assertTrue(createInstance.getId() > 0);

		// Lookup block storage (volume) within a region different from the one of
		// attached server -> no match
		// ---------------------------------
		Assertions.assertEquals(0,
				qsResource.lookup(subscription,
								QuoteStorageQuery.builder().size(5).location("sgb").instance(createInstance.getId()).build())
						.size());

		// Lookup STANDARD SSD storage within the same region than the attached server
		// volume
		// ---------------------------------
		var sLookup = qsResource.lookup(subscription, QuoteStorageQuery.builder().size(5).latency(Rate.LOW)
				.location("gra").instance(createInstance.getId()).build()).get(0);
		var price = sLookup.getPrice();
		var type = price.getType();
		Assertions.assertEquals("gra/volume", price.getCode());
		Assertions.assertEquals("volume", type.getCode());
		Assertions.assertEquals("gra", price.getLocation().getName());
		Assertions.assertEquals("Gravelines", price.getLocation().getDescription());
		Assertions.assertEquals(0.06, sLookup.getCost(), DELTA);

		// New storage attached to the created instance
		var svo = new QuoteStorageEditionVo();
		svo.setSize(100);
		svo.setName("storage1");
		svo.setSubscription(subscription);
		svo.setInstance(createInstance.getId());
		svo.setType(sLookup.getPrice().getType().getCode());
		var createStorage = qsResource.create(svo);
		Assertions.assertTrue(createStorage.getTotal().getMin() > 1);
		Assertions.assertTrue(createStorage.getId() > 0);

		// Lookup blob storage
		// ---------------------------------
		sLookup = qsResource.lookup(subscription, QuoteStorageQuery.builder().size(5).latency(Rate.GOOD)
				.location("gra").optimized(ProvStorageOptimized.DURABILITY).build()).get(0);
		price = sLookup.getPrice();
		Assertions.assertEquals("gra/storage", price.getCode());
		type = price.getType();
		Assertions.assertEquals("storage", type.getCode());
		Assertions.assertEquals("Object Storage", type.getName());
		Assertions.assertEquals("gra", price.getLocation().getName());
		Assertions.assertEquals("Hauts-de-France", price.getLocation().getSubRegion());
		Assertions.assertEquals(0.06, sLookup.getCost(), DELTA);

		// New storage2
		var svo2 = new QuoteStorageEditionVo();
		svo2.setSize(100);
		svo2.setName("storage2");
		svo2.setSubscription(subscription);
		svo2.setType(sLookup.getPrice().getType().getCode());
		var createStorage2 = qsResource.create(svo2);
		Assertions.assertTrue(createStorage2.getTotal().getMin() > 1);
		Assertions.assertTrue(createStorage2.getId() > 0);

		// Lookup archive storage
		// ---------------------------------
		sLookup = qsResource.lookup(subscription, QuoteStorageQuery.builder().size(5).latency(Rate.WORST)
				.location("gra").optimized(ProvStorageOptimized.DURABILITY).build()).get(0);
		Assertions.assertEquals(0.01, sLookup.getCost(), DELTA);
		price = sLookup.getPrice();
		Assertions.assertEquals("gra/archive", price.getCode());
		type = price.getType();
		Assertions.assertEquals("archive", type.getCode());
		Assertions.assertEquals("Cloud Archive", type.getName());
		Assertions.assertEquals("gra", price.getLocation().getName());
		Assertions.assertEquals("Hauts-de-France", price.getLocation().getSubRegion());

		// New storage3
		var svo3 = new QuoteStorageEditionVo();
		svo3.setSize(100);
		svo3.setName("storage3");
		svo3.setSubscription(subscription);
		svo3.setType(sLookup.getPrice().getType().getCode());
		var createStorage3 = qsResource.create(svo3);
		Assertions.assertTrue(createStorage3.getTotal().getMin() > 1);
		Assertions.assertTrue(createStorage3.getId() > 0);

		// Lookup snapshot storage
		// ---------------------------------
		sLookup = qsResource.lookup(subscription, QuoteStorageQuery.builder().size(5).latency(Rate.LOW).location("gra")
				.optimized(ProvStorageOptimized.DURABILITY).build()).get(0);

		// Lookup Database in an available region
		// ---------------------------------
		var dLookup = qbResource.lookup(subscription, QuoteDatabaseQuery.builder().engine("MYSQL").cpu(4).build());
		//var dLookup = qbResource.lookup(subscription, QuoteDatabaseQuery.builder().engine("MySQL").cpu(4).build());
		var dPrice = dLookup.getPrice();
		Assertions.assertEquals("gra/monthly.postpaid/mysql-essential-db1-15", dPrice.getCode());
		var dType = dPrice.getType();
		Assertions.assertEquals("essential/db1-15", dType.getCode());
		Assertions.assertEquals("essential/db1-15", dType.getName());
		Assertions.assertEquals("gra", dPrice.getLocation().getName());
		Assertions.assertEquals("Gravelines", dPrice.getLocation().getDescription());

		// Lookup Database in an available region with partial usage (dev)
		// ---------------------------------
		dLookup = qbResource.lookup(subscription,
				QuoteDatabaseQuery.builder().engine("MYSQL").cpu(4).usage("dev").build());
		dPrice = dLookup.getPrice();
		Assertions.assertEquals("gra/consumption/mysql-essential-db1-15", dPrice.getCode());
		dType = dPrice.getType();
		Assertions.assertEquals("essential/db1-15", dType.getCode());
		Assertions.assertEquals("essential/db1-15", dType.getName());
		Assertions.assertEquals("gra", dPrice.getLocation().getName());
		Assertions.assertEquals("Gravelines", dPrice.getLocation().getDescription());

		em.flush();
		em.clear();
		return provResource.getConfiguration(subscription);
	}

	/**
	 * Return the subscription identifier of the given project. Assumes there is only one subscription for a service.
	 */
	private int getSubscription(final String project) {
		return getSubscription(project, ProvOvhPluginResource.KEY);
	}

}

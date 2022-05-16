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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

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

import com.fasterxml.jackson.annotation.JsonProperty;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

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
				new Class[] { Node.class, Project.class, CacheCompany.class, CacheUser.class, DelegateNode.class,
						Parameter.class, ProvLocation.class, Subscription.class, ParameterValue.class,
						ProvQuote.class },
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
			t.setNbInstancePrices(null);
			t.setNbInstanceTypes(null);
			t.setNbStorageTypes(null);
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
		configuration.put(OvhPriceImport.CONF_ENGINE, "(mongodb|mysql|postgresql|kafka|redis|opensearch|kafkaMirrorMaker)");
		configuration.put(OvhPriceImport.CONF_FLAVOR, "(db1|db2).*");
		
		// Install a new configuration
		final var quote = install();

		// Check the whole quote
		final var instance = check(quote, 6866.47d, 13731.74d, 6861.27d); // 6865.27d 13730.54d

		// Check the 3 years term
		var lookup = qiResource.lookup(instance.getConfiguration().getSubscription().getId(),
				builder().cpu(7).ram(1741).constant(true).usage("36month").build());
		Assertions.assertEquals(4899.0d, lookup.getCost(), DELTA);// 2240d
		Assertions.assertEquals(4899.0d, lookup.getPrice().getCost(), DELTA);
		Assertions.assertEquals(4899.0d, lookup.getPrice().getCostPeriod(), DELTA);
		Assertions.assertEquals("monthly.postpaid", lookup.getPrice().getTerm().getCode());// monthly
		Assertions.assertFalse(lookup.getPrice().getTerm().isEphemeral());
		Assertions.assertEquals(1.0, lookup.getPrice().getPeriod(), DELTA);
		Assertions.assertEquals("monthly.postpaid/t1-180", lookup.getPrice().getCode());
		Assertions.assertEquals("t1-180", lookup.getPrice().getType().getCode());
		Assertions.assertEquals("gra7", lookup.getPrice().getLocation().getName());
		Assertions.assertEquals("Gravelines", lookup.getPrice().getLocation().getDescription());
		checkImportStatus();

		// Check physical CPU
		// CPU Intensive
		lookup = qiResource.lookup(instance.getConfiguration().getSubscription().getId(),
				builder().cpu(2).ram(4096).constant(true).build());
		Assertions.assertEquals("monthly.postpaid/t1-180", lookup.getPrice().getCode());
		// Assertions.assertEquals("null",
		// lookup.getPrice().getType().getProcessor());//Intel Xeon

		// General Purpose
		lookup = qiResource.lookup(instance.getConfiguration().getSubscription().getId(),
				builder().cpu(2).ram(8000).constant(true).build());
		Assertions.assertEquals("monthly.postpaid/t1-180", lookup.getPrice().getCode());
		// Assertions.assertEquals("null",
		// lookup.getPrice().getType().getProcessor());//Intel Xeon Skylake

		// Install again to check the update without change
		resetImportTask();
		resource.install(false);
		provResource.updateCost(subscription);
		check(provResource.getConfiguration(subscription), 6866.47d, 13731.74d, 6861.27d);
		checkImportStatus();

		// Now, change a price within the remote catalog

		// Point to another catalog with different prices
		configuration.put(OvhPriceImport.CONF_API_PRICES, "http://localhost:" + MOCK_PORT + "/v2");
		// configuration.put(OvhPriceImport.CONF_API_PRICES, "http://localhost:" +
		// MOCK_PORT);

		// Install the new catalog, update occurs
		resetImportTask();
		resource.install(false);
		provResource.updateCost(subscription);

		// Check the new price
		final var newQuote = provResource.getConfiguration(subscription);
		Assertions.assertEquals(7617.47d, newQuote.getCost().getMin(), DELTA);// 16d 6865.27 7605.27d

		// Compute price is updated
		final var instance2 = newQuote.getInstances().get(0);
		Assertions.assertEquals(7591.27d, instance2.getCost(), DELTA);// 6861.27

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
		Assertions.assertEquals("gra7/consumption/mysql-essential-db1-15", lookupB.getPrice().getCode());
		Assertions.assertEquals(15360.0, lookupB.getPrice().getType().getRam());// 1024
		Assertions.assertEquals(4.0, lookupB.getPrice().getType().getCpu());// 1
		Assertions.assertNull(lookupB.getPrice().getStorageEngine());

		// Check the databasePlan
		var databasePlan = new OvhDatabasePlan();
		databasePlan.setBackupRetention("essential");
		databasePlan.setDescription("Essential plan");
		databasePlan.setName("P2D");
		Assertions.assertEquals("essential", databasePlan.getBackupRetention());
		Assertions.assertEquals("Essential plan", databasePlan.getDescription());
		Assertions.assertEquals("P2D", databasePlan.getName());

		// Check databasePrice
		var databasePrice = new OvhDatabasePrice();
		databasePrice.setHourlyPrice(0.0314);
		databasePrice.setMonthlyPrice(22.82);
		databasePrice.setTerm("hourly");
		Assertions.assertEquals(0.0314, databasePrice.getHourlyPrice());
		Assertions.assertEquals(22.82, databasePrice.getMonthlyPrice());
		Assertions.assertEquals("hourly", databasePrice.getTerm());

		// Check databaseFlavor
		var databaseFlavor = new OvhDatabaseFlavor();
		databaseFlavor.setCore(2);
		databaseFlavor.setMemory(7);
		databaseFlavor.setStorage(50);
		databaseFlavor.setName("db1-7");
		Assertions.assertEquals(2, databaseFlavor.getCore());
		Assertions.assertEquals(7, databaseFlavor.getMemory());
		Assertions.assertEquals(50, databaseFlavor.getStorage());
		Assertions.assertEquals("db1-7", databaseFlavor.getName());

		// Check DatabaseAvaibility
		var databaseAvaibility = new OvhDatabaseAvaibility();
		databaseAvaibility.setBackup("automatic");
		databaseAvaibility.setDefaultt(false);
		databaseAvaibility.setEndOfLife(null);
		databaseAvaibility.setEngine("mongodb");
		databaseAvaibility.setFlavor("db1-2");
		databaseAvaibility.setMaxDiskSize(25);
		databaseAvaibility.setMinDiskSize(25);
		databaseAvaibility.setMaxNodeNumber(1);
		databaseAvaibility.setMinNodeNumber(1);
		databaseAvaibility.setNetwork("public");
		databaseAvaibility.setPlan("essential");
		databaseAvaibility.setRegion("GRA7");
		databaseAvaibility.setStartDate(new Date(2021 - 12 - 17));
		databaseAvaibility.setStatus("STABLE");
		databaseAvaibility.setUpstreamEndOfLife(null);
		databaseAvaibility.setVersion("4.2");

		Assertions.assertEquals("automatic", databaseAvaibility.getBackup());
		Assertions.assertNull(databaseAvaibility.getEndOfLife());
		Assertions.assertEquals("mongodb", databaseAvaibility.getEngine());
		Assertions.assertEquals("db1-2", databaseAvaibility.getFlavor());
		Assertions.assertEquals(25, databaseAvaibility.getMaxDiskSize());
		Assertions.assertEquals(1, databaseAvaibility.getMaxNodeNumber());
		Assertions.assertEquals(25, databaseAvaibility.getMinDiskSize());
		Assertions.assertEquals(1, databaseAvaibility.getMinNodeNumber());
		Assertions.assertEquals("public", databaseAvaibility.getNetwork());
		Assertions.assertEquals("essential", databaseAvaibility.getPlan());
		Assertions.assertEquals("GRA7", databaseAvaibility.getRegion());
		Assertions.assertEquals(new Date(2021 - 12 - 17), databaseAvaibility.getStartDate());
		Assertions.assertEquals("STABLE", databaseAvaibility.getStatus());
		Assertions.assertNull(databaseAvaibility.getUpstreamEndOfLife());
		Assertions.assertEquals("4.2", databaseAvaibility.getVersion());
		Assertions.assertFalse(databaseAvaibility.isDefaultt());
		
		
		// databaseCapabilities
		var databaseCapabilities = new OvhDatabaseCapabilities();
		List<OvhDatabaseFlavor> databaseFlavors = new ArrayList<OvhDatabaseFlavor>();
		databaseFlavors.add(databaseFlavor);
		databaseCapabilities.setFlavors(databaseFlavors);
		
		// Check BandwidthArchiveIn
		var BandwidthArchiveIn = new OvhBandwidthArchiveIn();
		BandwidthArchiveIn.setValue(0.01);
		BandwidthArchiveIn.setCurrencyCode("EUR");
		BandwidthArchiveIn.setRegion("gra7");
		Assertions.assertEquals(0.01, BandwidthArchiveIn.getValue());
		Assertions.assertEquals("EUR", BandwidthArchiveIn.getCurrencyCode());
		Assertions.assertEquals("gra7", BandwidthArchiveIn.getRegion());

		// Check BandwidthArchiveOut
		var BandwidthArchiveOut = new OvhBandwidthArchiveOut();
		BandwidthArchiveOut.setValue(0.01);
		BandwidthArchiveOut.setCurrencyCode("EUR");
		BandwidthArchiveOut.setRegion("gra7");
		Assertions.assertEquals(0.01, BandwidthArchiveOut.getValue());
		Assertions.assertEquals("EUR", BandwidthArchiveOut.getCurrencyCode());
		Assertions.assertEquals("gra7", BandwidthArchiveOut.getRegion());

		// Check BandwidthStorage
		var BandwidthStorage = new OvhBandwidthStorage();
		BandwidthStorage.setValue(0.01);
		BandwidthStorage.setCurrencyCode("EUR");
		BandwidthStorage.setRegion("gra7");
		Assertions.assertEquals(0.01, BandwidthStorage.getValue());
		Assertions.assertEquals("EUR", BandwidthStorage.getCurrencyCode());
		Assertions.assertEquals("gra7", BandwidthStorage.getRegion());
		
		//Check Databases 
		var databases = new OvhDatabases();
		List<OvhDatabaseAvaibility> databasesAvaibilitys = new ArrayList<OvhDatabaseAvaibility>() ;
		List<OvhDatabaseCapabilities> databasesCapabilities= new ArrayList<OvhDatabaseCapabilities>();
		databasesAvaibilitys.add(databaseAvaibility);
		databasesCapabilities.add(databaseCapabilities);
		databases.setDatabaseAvaibility(databasesAvaibilitys);
		databases.setDatabaseCapabilities(databasesCapabilities);
		Assertions.assertEquals(databaseAvaibility.getEngine(), databases.getDatabaseAvaibility().get(0).getEngine());
		Assertions.assertEquals(databaseCapabilities.getFlavors().get(0).getCore(),  databases.getDatabaseCapabilities().get(0).getFlavors().get(0).getCore());
		
		// Check AllPrice
		var allPrice = new OvhAllPrices();
		List <OvhBandwidthArchiveIn> BandwidthArchiveIns = new ArrayList<OvhBandwidthArchiveIn>();
		BandwidthArchiveIns.add(BandwidthArchiveIn);
		List <OvhBandwidthArchiveOut> BandwidthArchiveOuts = new ArrayList<OvhBandwidthArchiveOut>();
		BandwidthArchiveOuts.add(BandwidthArchiveOut);
		List <OvhBandwidthStorage> BandwidthStorages = new ArrayList<OvhBandwidthStorage>();
		BandwidthStorages.add(BandwidthStorage);
		allPrice.setBandwidthArchiveIn(BandwidthArchiveIns);
		allPrice.setBandwidthArchiveOut(BandwidthArchiveOuts);
		allPrice.setBandwidthStorage(BandwidthStorages);
		Assertions.assertEquals(0.01, allPrice.getBandwidthArchiveIn().get(0).getValue());
		Assertions.assertEquals(0.01, allPrice.getBandwidthArchiveOut().get(0).getValue());
		Assertions.assertEquals(0.01, allPrice.getBandwidthStorage().get(0).getValue());
		
		// var sLookup = qsResource.lookup(subscription,
		// QuoteStorageQuery.builder().size(5).latency(Rate.LOW).location("gra7"));

	}

	private void checkImportStatus() {
		final var status = this.resource.getImportCatalogResource().getTask("service:prov:ovh");
		Assertions.assertEquals(7, status.getDone());
		Assertions.assertEquals(6, status.getWorkload());
		Assertions.assertEquals("support", status.getPhase());
		Assertions.assertEquals(DEFAULT_USER, status.getAuthor());
		Assertions.assertTrue(status.getNbInstancePrices().intValue() >= 18);
		Assertions.assertTrue(status.getNbInstanceTypes().intValue() >= 9);
		Assertions.assertTrue(status.getNbLocations() >= 1);
		Assertions.assertTrue(status.getNbStorageTypes().intValue() >= 3);
	}

	private void mockServer() throws IOException {
		configuration.put(OvhPriceImport.CONF_API_PRICES, "http://localhost:" + MOCK_PORT);
		httpServer.stubFor(get(urlEqualTo("/price.json")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(
				IOUtils.toString(new ClassPathResource("mock-server/ovh/prices.json").getInputStream(), "UTF-8"))));
		httpServer.stubFor(get(urlEqualTo("/flavor.json")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(
				IOUtils.toString(new ClassPathResource("mock-server/ovh/flavors.json").getInputStream(), "UTF-8"))));
		httpServer.stubFor(get(urlEqualTo("/databaseAvaibility.json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(IOUtils.toString(
						new ClassPathResource("mock-server/ovh/databaseAvaibility.json").getInputStream(), "UTF-8"))));
		httpServer.stubFor(
				get(urlEqualTo("/databaseCapabilities.json")).willReturn(aResponse().withStatus(HttpStatus.SC_OK)
						.withBody(IOUtils.toString(
								new ClassPathResource("mock-server/ovh/databaseCapabilities.json").getInputStream(),
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
				get(urlEqualTo("/v2/databaseAvaibility.json")).willReturn(aResponse().withStatus(HttpStatus.SC_OK)
						.withBody(IOUtils.toString(
								new ClassPathResource("mock-server/ovh/v2/databaseAvaibility.json").getInputStream(),
								"UTF-8"))));
		httpServer.stubFor(
				get(urlEqualTo("/v2/databaseCapabilities.json")).willReturn(aResponse().withStatus(HttpStatus.SC_OK)
						.withBody(IOUtils.toString(
								new ClassPathResource("mock-server/ovh/v2/databaseCapabilities.json").getInputStream(),
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
		checkLocation(quote.getLocations());
		return checkInstance(quote.getInstances().get(0), instanceCost);
	}

	private void checkLocation(List<ProvLocation> locations) {
		var location1 = new OvhRegion();
		location1.setName("gra7");

		var location2 = new OvhRegion();
		location2.setName("sgb5");

		List<OvhRegion> Locations = new ArrayList<OvhRegion>();
		;

		Locations.add(location1);
		Locations.add(location2);

		var Regions = new OvhRegions();
		Regions.setRegions(Locations);
		Assertions.assertEquals("gra7", Regions.getRegions().get(0).getName());

		var regionPrice = new OvhRegionPrices();
		regionPrice.setRegion("gra7");
		Assertions.assertEquals("gra7", regionPrice.getRegion());

	}

	private ProvQuoteInstance checkInstance(final ProvQuoteInstance instance, final double cost) {
		Assertions.assertEquals(cost, instance.getCost(), DELTA);
		final var price = instance.getPrice();
		Assertions.assertEquals(0, price.getInitialCost());
		Assertions.assertEquals(VmOs.LINUX, price.getOs());
		Assertions.assertEquals(ProvTenancy.SHARED, price.getTenancy());
		Assertions.assertEquals(6861.27d, price.getCost(), DELTA);// 5d
		Assertions.assertEquals(6861.27d, price.getCostPeriod(), DELTA);// 5d
		Assertions.assertEquals(0.0, price.getPeriod(), DELTA);// 1
		final var term = price.getTerm();
		Assertions.assertEquals("consumption", term.getCode());// monthly
		Assertions.assertEquals("consumption", term.getName());// monthly
		Assertions.assertFalse(term.isEphemeral());
		Assertions.assertEquals(0.0, term.getPeriod());// 1
		Assertions.assertEquals("t1-180", price.getType().getCode());
		Assertions.assertEquals("t1-180", price.getType().getName());
		Assertions.assertEquals("{Disk: 50, Network: 10000/10000}", price.getType().getDescription());// {Disk: 25,
																										// Category:
																										// Standard}
		Assertions.assertNull(price.getType().getProcessor());
		Assertions.assertFalse(price.getType().isAutoScale());

		var flavor = new OvhFlavor();
		flavor.setRegion("gra7");
		flavor.setType("ovh.ssd.ram");
		Assertions.assertEquals("gra7", flavor.getRegion());
		Assertions.assertEquals("ovh.ssd.ram", flavor.getType());

		return instance;
	}

	private ProvQuoteStorage checkStorage(final List<ProvQuoteStorage> storages) {
		var volume = storages.get(0);
		Assertions.assertEquals(4d, volume.getCost(), DELTA);
		Assertions.assertEquals(100, volume.getSize(), DELTA);
		Assertions.assertNotNull(volume.getQuoteInstance());
		final var typeV = volume.getPrice().getType();
		Assertions.assertEquals("volume.classic", typeV.getCode());
		Assertions.assertEquals("Classic", typeV.getName());
		Assertions.assertEquals(250, typeV.getIops());
		Assertions.assertEquals(300, typeV.getThroughput());
		Assertions.assertEquals(0d, volume.getPrice().getCostTransaction(), DELTA);
		Assertions.assertEquals(1, typeV.getMinimal());
		Assertions.assertEquals(4096, typeV.getMaximal().intValue());
		Assertions.assertEquals(Rate.BEST, typeV.getLatency());
		Assertions.assertEquals(ProvStorageOptimized.IOPS, typeV.getOptimized());

		var storage = storages.get(1);
		Assertions.assertEquals(1d, storage.getCost(), DELTA);
		Assertions.assertEquals(100, storage.getSize(), DELTA);
		// Assertions.assertNotNull(storage.getQuoteInstance());
		final var typeS = storage.getPrice().getType();
		Assertions.assertEquals("storage", typeS.getCode());
		Assertions.assertEquals("Object Storage", typeS.getName());
		Assertions.assertEquals(5000, typeS.getIops());
		Assertions.assertEquals(200, typeS.getThroughput());
		Assertions.assertEquals(0d, storage.getPrice().getCostTransaction(), DELTA);
		Assertions.assertEquals(1d, typeS.getMinimal());
		// Assertions.assertEquals(4096, typeS.getMaximal().intValue());
		Assertions.assertEquals(Rate.GOOD, typeS.getLatency());
		Assertions.assertEquals(ProvStorageOptimized.DURABILITY, typeS.getOptimized());

		var storage2 = new OvhStorage();
		storage2.setRegion(storage.getPrice().getLocation().getName());
		storage2.setPrice(storage.getCost());
		Assertions.assertEquals(1.0, storage2.getPrice());
		Assertions.assertEquals("gra7", storage2.getRegion());

		var archive = storages.get(2);
		Assertions.assertEquals(0.2, archive.getCost(), DELTA);
		Assertions.assertEquals(100, archive.getSize(), DELTA);
		// Assertions.assertNotNull(archive.getQuoteInstance());
		final var typeA = archive.getPrice().getType();
		Assertions.assertEquals("archive", typeA.getCode());
		Assertions.assertEquals("Cloud Archive", typeA.getName());
		Assertions.assertEquals(7500, typeA.getIops());
		Assertions.assertEquals(300, typeA.getThroughput());
		Assertions.assertEquals(0d, archive.getPrice().getCostTransaction(), DELTA);
		Assertions.assertEquals(1, typeA.getMinimal());
		// Assertions.assertEquals(4096, typeA.getMaximal().intValue());
		Assertions.assertEquals(Rate.WORST, typeA.getLatency());
		Assertions.assertEquals(ProvStorageOptimized.DURABILITY, typeA.getOptimized());

		var archive2 = new OvhArchive();
		archive2.setRegion(archive.getPrice().getLocation().getName());
		archive2.setCurrencyCode("EUR");
		archive2.setMonthlyPrice(archive.getCost());
		Assertions.assertEquals(0.2, archive2.getMonthlyPrice());
		Assertions.assertEquals("EUR", archive2.getCurrencyCode());
		Assertions.assertEquals("gra7", archive2.getRegion());

		var Snapshot = new OvhSnapshot();
		Snapshot.setRegion("gra7");
		Snapshot.setCurrencyCode("EUR");
		Snapshot.setValue("2e-05");
		Snapshot.setMonthlyPriceValue(0.01);
		Snapshot.setMonthlyPriceCurrencyCode("EUR");
		Assertions.assertEquals("EUR", Snapshot.getMonthlyPriceCurrencyCode());
		Assertions.assertEquals("EUR", Snapshot.getCurrencyCode());
		Assertions.assertEquals("gra7", Snapshot.getRegion());
		Assertions.assertEquals("2e-05", Snapshot.getValue());
		Assertions.assertEquals(0.01, Snapshot.getMonthlyPriceValue());

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
		configuration.put(OvhPriceImport.CONF_REGIONS, "(sfo1|sfo2|nyc1|sgp1)");
		configuration.put(OvhPriceImport.CONF_ITYPE, "(m6-|s-).*");
		configuration.put(OvhPriceImport.CONF_DTYPE, ".*(db1|db2).*");
		configuration.put(OvhPriceImport.CONF_ENGINE, "(MYSQL)");
		configuration.put(OvhPriceImport.CONF_OS, "(WINDOWS|LINUX|CENTOS)");

		final var quote = installAndConfigure();
		Assertions.assertTrue(quote.getCost().getMin() >= 15);
		final var lookup = qiResource.lookup(subscription,
				builder().cpu(8).ram(26000).constant(true).type("m6-32vcpu-256gb").usage("36month").build());

		Assertions.assertTrue(lookup.getCost() > 900d);
		final var instance2 = lookup.getPrice();
		Assertions.assertEquals("monthly", instance2.getTerm().getCode());
		Assertions.assertEquals("m6-32vcpu-256gb", instance2.getType().getCode());
		Assertions.assertEquals("nyc1/monthly/centos/m6-32vcpu-256gb", instance2.getCode());
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
				builder().cpu(2).ram(15000).constant(true).os(VmOs.WINDOWS).location("sbg5").usage("36month").build());
		Assertions.assertEquals("monthly.postpaid/win-r2-15-flex", lookup.getPrice().getCode());

		// Request an instance for a generic Linux OS
		lookup = qiResource.lookup(subscription,
				builder().constant(true).type("t1-180").os(VmOs.LINUX).location("gra7").usage("dev").build());
		Assertions.assertEquals("consumption/t1-180", lookup.getPrice().getCode());

		// New instance for "s-1vcpu-1gb"
		var ivo = new QuoteInstanceEditionVo();
		ivo.setCpu(1d);
		ivo.setRam(1);
		ivo.setLocation("gra7");
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
						QuoteStorageQuery.builder().size(5).location("sgb5").instance(createInstance.getId()).build())
						.size());

		// Lookup STANDARD SSD storage within the same region than the attached server
		// volume
		// ---------------------------------
		// var sLookup = qsResource.lookup(subscription,
		// QuoteStorageQuery.builder().size(5).latency(Rate.LOW)
		// .location("gra7").instance(createInstance.getId()).build()).get(0);
		var sLookup = qsResource.lookup(subscription, QuoteStorageQuery.builder().size(5).latency(Rate.LOW)
				.location("gra7").instance(createInstance.getId()).build()).get(0);
		Assertions.assertEquals(0.2, sLookup.getCost(), DELTA);
		var price = sLookup.getPrice();
		Assertions.assertEquals("gra7/volume.classic", price.getCode());
		var type = price.getType();
		Assertions.assertEquals("volume.classic", type.getCode());
		Assertions.assertEquals("gra7", price.getLocation().getName());
		Assertions.assertEquals("Gravelines", price.getLocation().getDescription());

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

		/*
		 * New instance2 for "s-1vcpu-1gb" var ivo2 = new QuoteInstanceEditionVo();
		 * ivo2.setCpu(1d); ivo2.setRam(1); ivo2.setLocation("gra7");
		 * ivo2.setPrice(lookup.getPrice().getId()); ivo2.setName("server2");
		 * ivo2.setMaxQuantity(2); ivo2.setSubscription(subscription); var
		 * createInstance2 = qiResource.create(ivo2);
		 * Assertions.assertTrue(createInstance2.getTotal().getMin() >= 1);
		 * Assertions.assertTrue(createInstance2.getId() > 0);
		 */

		// Lookup blob storage
		// Storage
		// ---------------------------------
		sLookup = qsResource.lookup(subscription, QuoteStorageQuery.builder().size(5).latency(Rate.LOW).location("gra7")
				.optimized(ProvStorageOptimized.DURABILITY).build()).get(0);
		// sLookup = qsResource.lookup(subscription,
		// QuoteStorageQuery.builder().size(5).latency(Rate.LOW)
		// .location("gra7").optimized(ProvStorageOptimized.DURABILITY).instance(createInstance2.getId()).build()).get(0);
		Assertions.assertEquals(0.05, sLookup.getCost(), DELTA);
		price = sLookup.getPrice();
		Assertions.assertEquals("gra7/storage", price.getCode());
		type = price.getType();
		Assertions.assertEquals("storage", type.getCode());
		Assertions.assertEquals("Object Storage", type.getName());
		Assertions.assertEquals("gra7", price.getLocation().getName());
		Assertions.assertEquals("Hauts-de-France", price.getLocation().getSubRegion());

		// New storage2
		var svo2 = new QuoteStorageEditionVo();
		svo2.setSize(100);
		svo2.setName("storage2");
		svo2.setSubscription(subscription);
		// svo2.setInstance(createInstance2.getId());
		svo2.setType(sLookup.getPrice().getType().getCode());
		var createStorage2 = qsResource.create(svo2);
		Assertions.assertTrue(createStorage2.getTotal().getMin() > 1);
		Assertions.assertTrue(createStorage2.getId() > 0);

		// Lookup blob storage
		// archive
		// ---------------------------------
		sLookup = qsResource.lookup(subscription, QuoteStorageQuery.builder().size(5).latency(Rate.WORST)
				.location("gra7").optimized(ProvStorageOptimized.DURABILITY).build()).get(0);
		Assertions.assertEquals(0.01, sLookup.getCost(), DELTA);
		price = sLookup.getPrice();
		Assertions.assertEquals("gra7/archive", price.getCode());
		type = price.getType();
		Assertions.assertEquals("archive", type.getCode());
		Assertions.assertEquals("Cloud Archive", type.getName());
		Assertions.assertEquals("gra7", price.getLocation().getName());
		Assertions.assertEquals("Hauts-de-France", price.getLocation().getSubRegion());

		// New storage3
		var svo3 = new QuoteStorageEditionVo();
		svo3.setSize(100);
		svo3.setName("storage3");
		svo3.setSubscription(subscription);
		// svo.setInstance(createInstance.getId());
		svo3.setType(sLookup.getPrice().getType().getCode());
		var createStorage3 = qsResource.create(svo3);
		Assertions.assertTrue(createStorage3.getTotal().getMin() > 1);
		Assertions.assertTrue(createStorage3.getId() > 0);

		// Lookup blob storage
		// Snapshot
		// ---------------------------------
		sLookup = qsResource.lookup(subscription, QuoteStorageQuery.builder().size(5).latency(Rate.LOW).location("gra7")
				.optimized(ProvStorageOptimized.DURABILITY).build()).get(0);

		// Lookup Database unavailable in a region
		// ---------------------------------
		Assertions.assertNull(qbResource.lookup(subscription,
				QuoteDatabaseQuery.builder().location("sbg5").engine("MYSQL").cpu(3).build()));

		// Lookup Database in an available region
		// ---------------------------------
		var dLookup = qbResource.lookup(subscription, QuoteDatabaseQuery.builder().engine("MySQL").cpu(2).build());
		Assertions.assertEquals(217.84d, dLookup.getCost(), DELTA);
		var dPrice = dLookup.getPrice();
		Assertions.assertEquals("gra7/consumption/mysql-essential-db1-15", dPrice.getCode());
		var dType = dPrice.getType();
		Assertions.assertEquals("essential/db1-15", dType.getCode());
		Assertions.assertEquals("essential/db1-15", dType.getName());
		Assertions.assertEquals("gra7", dPrice.getLocation().getName());
		Assertions.assertEquals("Gravelines", dPrice.getLocation().getDescription());

		em.flush();
		em.clear();
		return provResource.getConfiguration(subscription);
	}

	/**
	 * Return the subscription identifier of the given project. Assumes there is
	 * only one subscription for a service.
	 */
	private int getSubscription(final String project) {
		return getSubscription(project, ProvOvhPluginResource.KEY);
	}

}

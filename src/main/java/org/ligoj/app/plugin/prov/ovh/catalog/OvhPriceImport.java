/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.plugin.prov.catalog.AbstractImportCatalogResource;
import org.ligoj.app.plugin.prov.catalog.AbstractUpdateContext;
import org.ligoj.app.plugin.prov.model.AbstractPrice;
import org.ligoj.app.plugin.prov.model.ImportCatalogStatus;
import org.ligoj.app.plugin.prov.model.ProvDatabasePrice;
import org.ligoj.app.plugin.prov.model.ProvDatabaseType;
import org.ligoj.app.plugin.prov.model.ProvInstancePrice;
import org.ligoj.app.plugin.prov.model.ProvInstancePriceTerm;
import org.ligoj.app.plugin.prov.model.ProvInstanceType;
import org.ligoj.app.plugin.prov.model.ProvLocation;
import org.ligoj.app.plugin.prov.model.ProvStorageOptimized;
import org.ligoj.app.plugin.prov.model.ProvStoragePrice;
import org.ligoj.app.plugin.prov.model.ProvStorageType;
import org.ligoj.app.plugin.prov.model.ProvSupportPrice;
import org.ligoj.app.plugin.prov.model.ProvSupportType;
import org.ligoj.app.plugin.prov.model.ProvTenancy;
import org.ligoj.app.plugin.prov.model.Rate;
import org.ligoj.app.plugin.prov.model.VmOs;
import org.ligoj.app.plugin.prov.ovh.ProvOvhPluginResource;
import org.ligoj.bootstrap.core.INamableBean;
import org.ligoj.bootstrap.core.curl.CurlProcessor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * The provisioning price service for AWS. Manage install or update of prices.
 * 
 * @see https://us.ovhcloud.com/legal/sla/public-cloud for SLA
 * @see https://www.ovhcloud.com/en/support-levels/plans/ for Support prices
 */
@Component
@Setter
@Slf4j
public class OvhPriceImport extends AbstractImportCatalogResource {

	@Override
	protected int getWorkload(final ImportCatalogStatus status) {
		return 6; // TODO init + get catalog + vm + db + support+storage
	}

	/**
	 * Configuration key used for URL prices.
	 */
	public static final String CONF_API_PRICES = ProvOvhPluginResource.KEY + ":prices-url";

	/**
	 * Name space for local configuration files
	 */
	protected static final String PREFIX = "digitalocean";

	/**
	 * Configuration key used for enabled instance type pattern names. When value is
	 * <code>null</code>, no restriction.
	 */
	public static final String CONF_ITYPE = ProvOvhPluginResource.KEY + ":instance-type";

	/**
	 * Configuration key used for enabled database type pattern names. When value is
	 * <code>null</code>, no restriction.
	 */
	public static final String CONF_DTYPE = ProvOvhPluginResource.KEY + ":database-type";
	/**
	 * Configuration key used for enabled database engine pattern names. When value
	 * is <code>null</code>, no restriction.
	 */
	public static final String CONF_ENGINE = ProvOvhPluginResource.KEY + ":database-engine";

	/**
	 * Configuration key used for enabled OS pattern names. When value is
	 * <code>null</code>, no restriction.
	 */
	public static final String CONF_OS = ProvOvhPluginResource.KEY + ":os";

	/**
	 * Configuration key used for enabled regions pattern names. When value is
	 * <code>null</code>, no restriction.
	 */
	public static final String CONF_REGIONS = ProvOvhPluginResource.KEY + ":regions";

	/**
	 * Path to root bulk price index.
	 */
	public static final String OVH_PRICES_PATH = "/price.json"; // Public: "/cloud/price"

	public static final String OVH_REGIONS_PATH = "/region.json"; // Public: "/cloud/project/%s/region"

	public static final String OVH_FLAVORS_PATH = "/flavor.json"; // Public: "/cloud/project/%s/flavor"

	/**
	 * <code>
	 * curl -s https://www.ovhcloud.com/en/public-cloud/prices/ | \
	grep "data-price"  \
	| sed 's/^.*<tr data-price="//g' \
	| sed 's|}"||g' \
	| sed 's|">.*$|"},|' \
	| sed 's/ data-planCode="/,"planCode":"/' \
	| sed 's/ data-regions="/,"regions":"/' \
	| sed 's/ data-price-type="/,"term":"/' \
	| sed 's/&quot;/"/g'\
	| sed 's|\\/|/|g'\
	| sed 's|  | |g' > database-price.json
	</code>
	 */
	public static final String OVH_PRICES_DATABASE_PATH = "/database-price.json";

	public static final String OVH_AVAIBILITY_DATABASE_PATH = "/databaseAvaibility.json"; // Public: "/cloud/project/%s/database/availability"

	public static final String OVH_CAPABILITIES_DATABASE_PATH = "/databaseCapabilities.json"; // Public: "/cloud/project/%s/database/capabilities"

	/**
	 * Default pricing URL.
	 */
	protected static final String DEFAULT_API_PRICES = "https://da9smdsh48mvy.cloudfront.net"; // https://ovh.ligoj.io";

	private static final TypeReference<Map<String, ProvLocation>> MAP_LOCATION = new TypeReference<>() {
		// Nothing to extend
	};

	private static final TypeReference<List<OvhFlavor>> FLAVOR_LIST = new TypeReference<>() {
		// Nothing to extend
	};

	private static final TypeReference<List<OvhDatabase>> DATABASE_LIST = new TypeReference<>() {
		// Nothing to extend
	};

	private static final TypeReference<List<OvhDatabaseAvaibility>> DATABASE_AVAIBILITY_LIST = new TypeReference<>() {
		// Nothing to extend OvhDatabaseCapabilities
	};

	private static final TypeReference<List<OvhDatabaseCapabilities>> DATABASE_CAPABILITIES_LIST = new TypeReference<>() {
		// Nothing to extend
	};

	/**
	 * Install or update prices.
	 *
	 * @param force When <code>true</code>, all cost attributes are update.
	 * @throws IOException When CSV or XML files cannot be read.
	 */
	public void install(final boolean force) throws IOException {
		final var context = initContext(new UpdateContext(), ProvOvhPluginResource.KEY, force);
		final var node = context.getNode();

		// Get previous data
		nextStep(context, "initialize");
		context.setValidOs(Pattern.compile(configuration.get(CONF_OS, ".*"), Pattern.CASE_INSENSITIVE));
		context.setValidDatabaseType(Pattern.compile(configuration.get(CONF_DTYPE, ".*"), Pattern.CASE_INSENSITIVE));
		context.setValidDatabaseEngine(Pattern.compile(configuration.get(CONF_ENGINE, ".*"), Pattern.CASE_INSENSITIVE));
		context.setValidInstanceType(Pattern.compile(configuration.get(CONF_ITYPE, ".*"), Pattern.CASE_INSENSITIVE));
		context.setValidRegion(Pattern.compile(configuration.get(CONF_REGIONS, ".*")));
		context.getMapRegionById().putAll(toMap("ovh/regions.json", MAP_LOCATION));
		context.setInstanceTypes(itRepository.findAllBy(BY_NODE, node).stream()
				.collect(Collectors.toMap(ProvInstanceType::getCode, Function.identity())));
		context.setDatabaseTypes(dtRepository.findAllBy(BY_NODE, node).stream()
				.collect(Collectors.toMap(ProvDatabaseType::getCode, Function.identity())));
		context.setPriceTerms(iptRepository.findAllBy(BY_NODE, node).stream()
				.collect(Collectors.toMap(ProvInstancePriceTerm::getCode, Function.identity())));
		context.setStorageTypes(stRepository.findAllBy(BY_NODE, node).stream()
				.collect(Collectors.toMap(ProvStorageType::getCode, Function.identity())));
		context.setPreviousStorage(spRepository.findAllBy("type.node", node).stream()
				.collect(Collectors.toMap(ProvStoragePrice::getCode, Function.identity())));
		context.setSupportTypes(st2Repository.findAllBy(BY_NODE, node).stream()
				.collect(Collectors.toMap(ProvSupportType::getName, Function.identity())));
		context.setPreviousSupport(sp2Repository.findAllBy("type.node", node).stream()
				.collect(Collectors.toMap(ProvSupportPrice::getCode, Function.identity())));
		context.setRegions(locationRepository.findAllBy(BY_NODE, context.getNode()).stream()
				.filter(r -> isEnabledRegion(context, r))
				.collect(Collectors.toMap(INamableBean::getName, Function.identity())));
		context.setPrevious(ipRepository.findAllBy("term.node", node).stream()
				.collect(Collectors.toMap(ProvInstancePrice::getCode, Function.identity())));

		// Fetch the remote prices stream and build the prices object
		nextStep(context, "retrieve-catalog");

		// Instance(VM)
		var hourlyTerm = installPriceTerm(context, "consumption", 0);
		var monthlyTerm = installPriceTerm(context, "monthly.postpaid", 1);

		try (var curl = new CurlProcessor()) {
			final var flavors = getFlavors().stream().collect(Collectors.toMap(OvhFlavor::getId, Function.identity()));
			final var prices = getPrices();

			// For each price/region/OS/software
			// Install term, type and price
			nextStep(context, "install-vm");
			final var instances = prices.getInstances();
			instances.stream().filter(i -> isEnabledRegion(context, i.getRegion().toLowerCase()))
					.forEach(i -> installInstancePrice(context, i, flavors, hourlyTerm, monthlyTerm));
		}
	
		// Database
		nextStep(context, "install-database");
		context.setPreviousDatabase(dpRepository.findAllBy("term.node", node).stream()
				.collect(Collectors.toMap(ProvDatabasePrice::getCode, Function.identity())));
		
		try (var curl = new CurlProcessor()) {
			final var pricesDatabases = getPricesDatabase();
			final var databasesAvaibility = getDatabases();
			final var datbaseCapabilities = getDatabasesCapabilities();
			
			
			databasesAvaibility.stream().filter(e -> isEnabledEngine(context, e.getEngine()))
			.forEach(engine -> datbaseCapabilities.stream().forEach(s -> {
				final var codeType = s.getNameFlavor();
				if (isEnabledDatabaseType(context, codeType)) {
					var type = installDatabaseType(context, codeType, s);
					context.getRegions().keySet().stream().filter(r -> isEnabledRegionDatabase(context, r))
							.forEach(region -> pricesDatabases.stream().forEach(price -> {
								
								// Install monthly based price
								var partialCode = codeType + "/" + engine;
								installDatabasePrice(context, monthlyTerm,
										monthlyTerm.getCode() + "/" + partialCode, type,									
										price.getMonthlyPrice(), engine.getEngine(), null, false, region);

								// Install hourly based price
								installDatabasePrice(context, hourlyTerm,
										hourlyTerm.getCode() + "-" + partialCode, type,
										price.getHourlyPrice() ,
										engine.getEngine(), null, false, region);
							}));
				}
			}));
		}
		
//
//		// Database
//		nextStep(context, "install-database");
//		context.setPreviousDatabase(dpRepository.findAllBy("term.node", node).stream()
//				.collect(Collectors.toMap(ProvDatabasePrice::getCode, Function.identity())));
//		try (var curl = new CurlProcessor()) {
//			final var mapper = new ObjectMapper();
//
//			mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
//			final var rawJS = StringUtils.defaultString(curl.get(getPricesApi() + "/aurora.js"), "");
//			final var engineMatcher = Pattern.compile("e.DBAAS_DBS=(\\[[^=]*\\])", Pattern.MULTILINE).matcher(rawJS);
//			// Engine
//			if (!engineMatcher.find()) {
//				// Prices format has changed too much, unable to parse data
//			throw new BusinessException("DigitalOcean prices API cannot be parsed, engines not found");
//			}
//			final var dbaasDbs = mapper
//					.readValue(
//							StringUtils.replace(StringUtils
//									.replace(StringUtils.replace(engineMatcher.group(1), "!0", "true"), "!1", "false")
//									.replaceAll("![^,}]+", "\"\""), "!", ""),
//							new TypeReference<List<NamedBean<Integer>>>() {
//							});
//			// Instance price
//			final var iMatcher = Pattern.compile("e.DBAAS_SIZES=(\\[[^=]*\\])", Pattern.MULTILINE).matcher(rawJS);
//			if (!iMatcher.find()) {
//				// Prices format has changed too much, unable to parse data
//				throw new BusinessException("DigitalOcean prices API cannot be parsed, sizes not found");
//			}
//			final var dbaasSizes = mapper.readValue(StringUtils.replace(iMatcher.group(1), "*l", ""),
//					new TypeReference<List<DatabasePrice>>() {
//					});
//
//			// For each price/region/engine
//			// Install term, type and price
//			dbaasDbs.stream().map(NamedBean::getName).filter(e -> isEnabledEngine(context, e))
//					.forEach(engine -> dbaasSizes.stream().forEach(s -> {
//						final var codeType = String.format("db-%d-%d", s.getCpu(), s.getMemory());
//						if (isEnabledDatabaseType(context, codeType)) {
//							var type = installDatabaseType(context, codeType, s);
//							context.getRegions().keySet().stream().filter(r -> isEnabledRegionDatabase(context, r))
//									.forEach(region -> {
//										// Install monthly based price
//										var partialCode = codeType + "/" + engine;
//										installDatabasePrice(context, monthlyTerm,
//												monthlyTerm.getCode() + "/" + partialCode, type,
//												s.getMonthlyPrice() * PRICE_MULTIPLIER, engine, null, false, region);
//
//										// Install hourly based price
//										installDatabasePrice(context, hourlyTerm,
//												hourlyTerm.getCode() + "-" + partialCode, type,
//												s.getMonthlyPrice() * PRICE_MULTIPLIER / 672d * context.getHoursMonth(),
//												engine, null, false, region);
//									});
//						}
//					}));
//		}

		// Install storage
		installStorage(context);

		// Support
		nextStep(context, "install-support");
		// Install previous types
		installSupportTypes(context);

		// Fetch previous prices
		final var previous = sp2Repository.findAllBy("type.node", context.getNode()).stream()
				.collect(Collectors.toMap(AbstractPrice::getCode, Function.identity()));

		// Complete the set
		csvForBean.toBean(ProvSupportPrice.class, "csv/ovh-prov-support-price.csv").forEach(t -> {
			final var entity = previous.computeIfAbsent(t.getCode(), n -> t);
			// Merge the support type details
			final var price = copyAsNeeded(context, entity, s -> {
				s.setLimit(t.getLimit());
				s.setMin(t.getMin());
				s.setRate(t.getRate());
			});
			saveAsNeeded(context, price, t.getCost(), sp2Repository);
		});
		nextStep(context, "support", null, 1);
	}

	private void installSupportTypes(final UpdateContext context) throws IOException {
		// Fetch previous prices
		final var previous = st2Repository.findAllBy(BY_NODE, context.getNode()).stream()
				.collect(Collectors.toMap(INamableBean::getName, Function.identity()));

		// Complete the set
		csvForBean.toBean(ProvSupportType.class, "csv/ovh-prov-support-type.csv").forEach(t -> {
			final var entity = previous.computeIfAbsent(t.getCode(), n -> t);
			// Merge the support type details
			copyAsNeeded(context, entity, t2 -> {
				t2.setName(t.getCode());
				t2.setDescription(t.getDescription());
				t2.setAccessApi(t.getAccessApi());
				t2.setAccessChat(t.getAccessChat());
				t2.setAccessEmail(t.getAccessEmail());
				t2.setAccessPhone(t.getAccessPhone());
				t2.setSlaStartTime(t.getSlaStartTime());
				t2.setSlaEndTime(t.getSlaEndTime());
				t2.setDescription(t.getDescription());

				t2.setSlaBusinessCriticalSystemDown(t.getSlaBusinessCriticalSystemDown());
				t2.setSlaGeneralGuidance(t.getSlaGeneralGuidance());
				t2.setSlaProductionSystemDown(t.getSlaProductionSystemDown());
				t2.setSlaProductionSystemImpaired(t.getSlaProductionSystemImpaired());
				t2.setSlaSystemImpaired(t.getSlaSystemImpaired());
				t2.setSlaWeekEnd(t.isSlaWeekEnd());

				t2.setCommitment(t.getCommitment());
				t2.setSeats(t.getSeats());
				t2.setLevel(t.getLevel());
			}, st2Repository);
		});
	}

	private String getApiPriceUrl(final String service, String templatePath) {
		return configuration.get(CONF_API_PRICES, DEFAULT_API_PRICES) + String.format(templatePath, service);
	}

	private String getFlavorsUrl() {
		return getApiPriceUrl("-public-", OVH_FLAVORS_PATH);
	}

	private String getDatabaseAvaibilityUrl() {
		return getApiPriceUrl("-public-", OVH_AVAIBILITY_DATABASE_PATH);
	}

	private String getDatabaseCapabilitiesyUrl() {
		return getApiPriceUrl("-public-", OVH_CAPABILITIES_DATABASE_PATH);
	}

	private String getPricesDatabaseUrl() {
		return getApiPriceUrl("-public-", OVH_PRICES_DATABASE_PATH);
	}

	private String getRegionsUrl() {
		return getApiPriceUrl("-public-", OVH_REGIONS_PATH);
	}

	private String getPricesUrl() {
		return getApiPriceUrl("-public-", OVH_PRICES_PATH);
	}

	private <C> C getResource(final Supplier<String> supplierUrl, final Class<C> resourceType,
			final TypeReference<C> typeReference) throws IOException {
		try (var curl = new CurlProcessor()) {
			final var rawJson = StringUtils.defaultString(curl.get(supplierUrl.get()), "{}");
			if (typeReference == null) {
				return objectMapper.readValue(rawJson, resourceType);
			}
			return objectMapper.readValue(rawJson, typeReference);
		}
	}

	private List<OvhDatabaseAvaibility> getDatabases() throws IOException {
		return getResource(this::getDatabaseAvaibilityUrl, null, DATABASE_AVAIBILITY_LIST);
	}

	private List<OvhDatabaseCapabilities> getDatabasesCapabilities() throws IOException {
		return getResource(this::getDatabaseCapabilitiesyUrl, null, DATABASE_CAPABILITIES_LIST);
	}

	private List<OvhDatabase> getPricesDatabase() throws IOException {
		return getResource(this::getPricesDatabaseUrl, null, DATABASE_LIST);
	}
	
	//private OvhDatabase getPricesDatabase() throws IOException {
	//	return getResource(this::getPricesDatabaseUrl, OvhDatabase.class, null);
	//}

	private OvhAllPrices getPrices() throws IOException {
		return getResource(this::getPricesUrl, OvhAllPrices.class, null);
	}

	private List<OvhFlavor> getFlavors() throws IOException {
		return getResource(this::getFlavorsUrl, null, FLAVOR_LIST);
	}

	private OvhRegions getRegions() throws IOException {
		return getResource(this::getRegionsUrl, OvhRegions.class, null);
	}

	private void installInstancePrice(final UpdateContext context, final OvhInstancePrice instance,
			final Map<String, OvhFlavor> flavors, final ProvInstancePriceTerm hourlyTerm,
			final ProvInstancePriceTerm monthlyTerm) {
		final var flavor = flavors.get(instance.getFlavorId());
		if (flavor == null) {
			log.warn("Unknown flavor id {}", instance.getFlavorId());
			return;
		}
		// Check the filtered types and OS
		final var os = getOs(flavor.getOsType());
		if (!isEnabledOs(context, os) || !isEnabledType(context, flavor.getName())) {
			return;
		}

		final var region = installRegion(context, instance.getRegion().toLowerCase());
		final var type = installInstanceType(context, flavor.getName(), flavor);
		installInstancePrice(context, hourlyTerm, os, type, instance.getPriceValue() * context.getHoursMonth(), region);
		installInstancePrice(context, monthlyTerm, os, type, instance.getMonthlyPriceValue(), region);
	}

	/**
	 * Check a database is available within the given region.
	 *
	 * @param context The update context.
	 * @param region  The region code to test.
	 * @return <code>true</code> when the region is available and enabled for the
	 *         database service.
	 */
	protected boolean isEnabledRegionDatabase(final UpdateContext context, final String region) {
		return isEnabledRegion(context, region);
	}

	@Override
	protected boolean isEnabledEngine(final AbstractUpdateContext context, final String engine) {
		// REDIS is not really an SGBD
		return super.isEnabledEngine(context, engine) && !engine.equalsIgnoreCase("REDIS");
	}

	/**
	 * Check a volume is available within the given region.
	 *
	 * @param context The update context.
	 * @param region  The region code to test.
	 * @return <code>true</code> when the region is available and enabled for the
	 *         volume service.
	 */
	protected boolean isEnabledRegionVolume(UpdateContext context, final String region) {
		return isEnabledRegion(context, region);
	}

	/**
	 * Install the storage types and prices.
	 */
	private void installStorage(final UpdateContext context) {
		nextStep(context, "install-vm-storage");

		// Block storage
		// See https://www.digitalocean.com/docs/volumes/
		// Standard
		installBlockStorage(context, "do-block-storage-standard", t -> {
			t.setIops(5000);
			t.setThroughput(200);
			t.setInstanceType("s-%");
		});

		// Optimized
		installBlockStorage(context, "do-block-storage-optimized", t -> {
			t.setIops(7500);
			t.setThroughput(300);
			t.setNotInstanceType("s-%");
			t.setInstanceType("%");
		});

		// Snapshot
		final var ssType = installStorageType(context, "do-snapshot", t -> {
			t.setLatency(Rate.GOOD);
			t.setDurability9(11);
			t.setOptimized(ProvStorageOptimized.DURABILITY);
		});
		context.getRegions().keySet().stream().filter(r -> isEnabledRegion(context, r))
				.forEach(r -> installStoragePrice(context, r, ssType, 0.05, r + "/" + ssType.getCode()));
	}

	private void installBlockStorage(UpdateContext context, final String code, final Consumer<ProvStorageType> filler) {
		final var type = installStorageType(context, code, t -> {
			filler.accept(t);
			t.setLatency(Rate.GOOD);
			t.setMaximal(16 * 1024d); // 16TiB
			t.setOptimized(ProvStorageOptimized.IOPS);
		});
		context.getRegions().keySet().stream().filter(r -> isEnabledRegionVolume(context, r))
				.forEach(region -> installStoragePrice(context, region, type, 0.1, region + "/" + type.getCode()));
	}

	private VmOs getOs(final String osName) {
		return EnumUtils.getEnum(VmOs.class, osName.toUpperCase());
	}

	/**
	 * Install or update a storage type.
	 */
	private ProvStorageType installStorageType(final UpdateContext context, final String code,
			final Consumer<ProvStorageType> aType) {
		final var type = context.getStorageTypes().computeIfAbsent(code, c -> {
			final var newType = new ProvStorageType();
			newType.setNode(context.getNode());
			newType.setCode(c);
			return newType;
		});

		return copyAsNeeded(context, type, t -> {
			t.setName(code /* human readable name */);
			t.setMinimal(1);
			t.setIncrement(null);
			t.setAvailability(99d);
			aType.accept(t);
		}, stRepository);
	}

	/**
	 * Install or update a storage price.
	 */
	private void installStoragePrice(final UpdateContext context, final String region, final ProvStorageType type,
			final double cost, final String code) {
		final var price = context.getPreviousStorage().computeIfAbsent(code, c -> {
			final var newPrice = new ProvStoragePrice();
			newPrice.setType(type);
			newPrice.setCode(c);
			return newPrice;
		});

		copyAsNeeded(context, price, p -> {
			p.setLocation(installRegion(context, region));
			p.setType(type);
		});

		// Update the cost
		saveAsNeeded(context, price, cost, spRepository);
	}

	/**
	 * Install a new instance price as needed.
	 */
	private void installInstancePrice(final UpdateContext context, final ProvInstancePriceTerm term, final VmOs os,
			final ProvInstanceType type, final double monthlyCost, final ProvLocation region) {
		final var price = context.getPrevious().computeIfAbsent(term.getCode() + "/" + type.getCode(), code -> {
			// New instance price (not update mode)
			final var newPrice = new ProvInstancePrice();
			newPrice.setCode(code);
			return newPrice;
		});
		copyAsNeeded(context, price, p -> {
			p.setLocation(region);
			p.setOs(os);
			p.setTerm(term);
			p.setTenancy(ProvTenancy.SHARED);
			p.setType(type);
			p.setPeriod(term.getPeriod());
		});

		// Update the cost
		saveAsNeeded(context, price, monthlyCost, ipRepository);
	}

	/**
	 * Install a new instance type as needed.
	 */
	private ProvInstanceType installInstanceType(final UpdateContext context, final String code,
			final OvhFlavor aType) {
		final var type = context.getInstanceTypes().computeIfAbsent(code, c -> {
			// New instance type (not update mode)
			final var newType = new ProvInstanceType();
			newType.setNode(context.getNode());
			newType.setCode(c);
			return newType;
		});

		// Merge as needed
		return copyAsNeeded(context, type, t -> {
			t.setName(code);
			t.setCpu(aType.getVcpus());
			t.setRam((int) Math.ceil(aType.getRam() / 1000 * 1024)); // Convert in MiB
			t.setDescription("{Disk: " + aType.getDisk() + ", Network: " + aType.getInboundBandwidth() + "/"
					+ aType.getOutboundBandwidth() + "}");
			t.setConstant(true);
			t.setAutoScale(false);

			// Rating
			t.setCpuRate(Rate.MEDIUM);
			t.setRamRate(Rate.MEDIUM);
			t.setNetworkRate(Rate.MEDIUM);
			t.setStorageRate(Rate.MEDIUM);
		}, itRepository);
	}

	/**
	 * Install a new price term as needed and complete the specifications.
	 */
	protected ProvInstancePriceTerm installPriceTerm(final UpdateContext context, final String code, final int period) {
		final var term = context.getPriceTerms().computeIfAbsent(code, t -> {
			final var newTerm = new ProvInstancePriceTerm();
			newTerm.setNode(context.getNode());
			newTerm.setCode(t);
			return newTerm;
		});

		// Complete the specifications
		return copyAsNeeded(context, term, t -> {
			t.setName(code /* human readable name */);
			t.setPeriod(period);
			t.setReservation(false);
			t.setConvertibleFamily(false);
			t.setConvertibleType(false);
			t.setConvertibleLocation(false);
			t.setConvertibleOs(false);
			t.setEphemeral(false);
		});
	}

	/**
	 * Install a new database type as needed.
	 */
	private ProvDatabaseType installDatabaseType(final UpdateContext context, final String code,
			final OvhDatabaseCapabilities aType) {
		final var type = context.getDatabaseTypes().computeIfAbsent(code, c -> {
			final var newType = new ProvDatabaseType();
			newType.setNode(context.getNode());
			newType.setCode(c);
			return newType;
		});

		// Merge as needed
		return copyAsNeeded(context, type, t -> {
			t.setName("DB " + aType.getCore() + "vCPU " + aType.getMemory() + "GiB");
			t.setCpu(aType.getCore());
			t.setRam(aType.getMemory() * 1024); // Convert in MiB
			t.setConstant(true);
			t.setAutoScale(false);

			// Rating
			t.setCpuRate(Rate.MEDIUM);
			t.setRamRate(Rate.MEDIUM);
			t.setNetworkRate(Rate.MEDIUM);
			t.setStorageRate(Rate.MEDIUM);
		}, dtRepository);
	}

	/**
	 * Install a new instance price as needed.
	 */
	private void installDatabasePrice(final UpdateContext context, final ProvInstancePriceTerm term,
			final String localCode, final ProvDatabaseType type, final double monthlyCost, final String engine,
			final String storageEngine, final boolean byol, final String region) {
		final var price = context.getPreviousDatabase().computeIfAbsent(region + "/" + localCode, c -> {
			// New instance price
			final var newPrice = new ProvDatabasePrice();
			newPrice.setCode(c);
			return newPrice;
		});

		copyAsNeeded(context, price, p -> {
			p.setLocation(installRegion(context, region));
			p.setEngine(engine.toUpperCase(Locale.ENGLISH));
			p.setStorageEngine(storageEngine);
			p.setLicense(null /* ProvInstancePrice.LICENSE_BYOL */);
			p.setTerm(term);
			p.setType(type);
			p.setPeriod(term.getPeriod());
		});

		// Update the cost
		saveAsNeeded(context, price, round3Decimals(monthlyCost), dpRepository);
	}

	public void installSupportPrice(final UpdateContext context, final String code, final ProvSupportPrice aPrice) {
		final var price = context.getPreviousSupport().computeIfAbsent(code, c -> {
			// New instance price
			final var newPrice = new ProvSupportPrice();
			newPrice.setCode(c);
			return newPrice;
		});

		// Merge the support type details
		copyAsNeeded(context, price, p -> {
			p.setLimit(aPrice.getLimit());
			p.setMin(aPrice.getMin());
			p.setRate(aPrice.getRate());
			p.setType(aPrice.getType());
		});

		// Update the cost
		saveAsNeeded(context, price, price.getCost(), aPrice.getCost(), (cR, c) -> price.setCost(cR),
				sp2Repository::save);
	}

}
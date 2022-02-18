/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.ligoj.app.plugin.prov.catalog.ImportCatalog;
import org.ligoj.app.plugin.prov.model.AbstractCodedEntity;
import org.ligoj.app.plugin.prov.model.ProvLocation;
import org.ligoj.app.plugin.prov.model.ProvStorageType;
import org.ligoj.app.plugin.prov.ovh.ProvOvhPluginResource;
import org.ligoj.bootstrap.core.INamableBean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

/**
 * The base import data.
 */
@Component
@Slf4j
public class OvhPriceImportBase extends AbstractOvhImport implements ImportCatalog<UpdateContext> {

	/**
	 * Configuration key used for enabled regions pattern names. When value is <code>null</code>, no restriction.
	 */
	public static final String CONF_REGIONS = ProvOvhPluginResource.KEY + ":regions";

	/**
	 * Path to root bulk price index.
	 */
	public static final String OVH_PRICES_PATH = "/price";

	private static final String AWS_PRICES_BASE = ProvOvhPluginResource.ENDPOINT + "/cloud";

	/**
	 * Configuration key used for AWS URL prices.
	 */
	public static final String CONF_URL_TMP_PRICES = ProvOvhPluginResource.KEY + ":%s-prices-url";

	/**
	 * Configuration key used for {@link #AWS_PRICES_BASE}
	 */
	public static final String CONF_URL_AWS_PRICES = String.format(CONF_URL_TMP_PRICES, "ovh");

	private static final TypeReference<Map<String, ProvLocation>> MAP_LOCATION = new TypeReference<>() {
		// Nothing to extend
	};

	@Override
	public void install(final UpdateContext context) throws IOException {
		importCatalogResource.nextStep(context.getNode().getId(), t -> t.setPhase("region"));
		context.setValidRegion(Pattern.compile(configuration.get(CONF_REGIONS, ".*")));
		context.getMapRegionById().putAll(toMap("ovh-regions.json", MAP_LOCATION));

		// Complete the by-name map
		context.getMapStorageToApi().putAll(toMap("storage-to-api.json", MAP_STR));
		context.getMapRegionById().forEach((id, r) -> context.getMapStorageToApi().put(r.getName(), id));

		// The previously installed location cache. Key is the location AWS name
		context.setRegions(locationRepository.findAllBy(BY_NODE, context.getNode()).stream()
				.filter(r -> isEnabledRegion(context, r))
				.collect(Collectors.toMap(INamableBean::getName, Function.identity())));

		// The previously installed storage types cache. Key is the storage name
		context.setStorageTypes(stRepository.findAllBy(BY_NODE, context.getNode()).stream()
				.collect(Collectors.toMap(AbstractCodedEntity::getCode, Function.identity())));
		installStorageTypes(context);
		context.getMapSpotToNewRegion().putAll(toMap("spot-to-new-region.json", MAP_STR));
		loadBaseIndex(context);
		nextStep(context, "region");
	}

	/**
	 * Get the root AWS bulk index file and save it in the context.
	 */
	private void loadBaseIndex(final UpdateContext context) throws IOException {
		final var basePrice = configuration.get(CONF_URL_AWS_PRICES, AWS_PRICES_BASE);
		context.setBaseUrl(basePrice);
		final var baseUrl = basePrice + OVH_PRICES_PATH;
		log.info("AWS {} import: download root index {}", "lambda", baseUrl);
		try (var reader = new BufferedReader(new InputStreamReader(new URL(baseUrl).openStream()))) {
			context.setOffers(objectMapper.readValue(reader, OvhPriceIndex.class).getOffers());
		}
	}

	private void installStorageTypes(final UpdateContext context) throws IOException {
		csvForBean.toBean(ProvStorageType.class, "csv/aws-prov-storage-type.csv").forEach(t -> {
			final var entity = context.getStorageTypes().computeIfAbsent(t.getName(), n -> {
				final var newType = new ProvStorageType();
				newType.setNode(context.getNode());
				newType.setCode(n);
				return newType;
			});

			// Merge the storage type details
			entity.setName(entity.getCode());
			entity.setDescription(t.getDescription());
			entity.setInstanceType(t.getInstanceType());
			entity.setDatabaseType(t.getDatabaseType());
			entity.setIops(t.getIops());
			entity.setLatency(t.getLatency());
			entity.setMaximal(t.getMaximal());
			entity.setMinimal(t.getMinimal());
			entity.setOptimized(t.getOptimized());
			entity.setThroughput(t.getThroughput());
			entity.setAvailability(t.getAvailability());
			entity.setDurability9(t.getDurability9());
			entity.setEngine(t.getEngine());
			stRepository.save(entity);
		});
	}
}

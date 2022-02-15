/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.prov.ovh.catalog.vm;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.plugin.prov.model.ProvInstancePrice;
import org.ligoj.app.plugin.prov.model.ProvInstancePriceTerm;
import org.ligoj.app.plugin.prov.ovh.catalog.vm.ec2.OvhPriceImportEc2;

/**
 * Test class of {@link OvhPriceImportEc2}
 */
class OvhPriceImportEc2Test {

	@Test
	void getOnDemandCode() {
		Assertions.assertNull(new OvhPriceImportEc2().getOnDemandCode(new HashMap<String, ProvInstancePrice>()));
		var term1 = new ProvInstancePriceTerm();
		term1.setName("Reserved");
		var term2 = new ProvInstancePriceTerm();
		term2.setName("OnDemand");
		var price1 = new ProvInstancePrice();
		price1.setCode("1");
		price1.setTerm(term1);
		var price2 = new ProvInstancePrice();
		price2.setCode("2");
		price2.setTerm(term2);
		Assertions.assertNull(new OvhPriceImportEc2().getOnDemandCode(Map.of("A", price1, "B", price2)));
		price1.setCode("1.2.3");
		Assertions.assertNull(new OvhPriceImportEc2().getOnDemandCode(Map.of("A", price1, "B", price2)));
		price2.setCode("4.5.6");
		Assertions.assertEquals(".5.6", new OvhPriceImportEc2().getOnDemandCode(Map.of("A", price1, "B", price2)));
	}
}

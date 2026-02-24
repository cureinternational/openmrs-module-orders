package org.openmrs.module.orders.api.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OrderContextTest {

	@Test
	public void shouldCreateOrderContextWithAllFields() {
		OrderContext context = new OrderContext("order-1", "patient-1", "encounter-1", "CBC");

		assertEquals("order-1", context.getOrderUuid());
		assertEquals("patient-1", context.getPatientUuid());
		assertEquals("encounter-1", context.getEncounterUuid());
		assertEquals("CBC", context.getConceptDisplay());
	}

	@Test
	public void shouldAllowNullConceptDisplay() {
		OrderContext context = new OrderContext("order-1", "patient-1", "encounter-1", null);

		assertNull(context.getConceptDisplay());
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRejectNullOrderUuid() {
		new OrderContext(null, "patient-1", "encounter-1", "CBC");
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRejectEmptyOrderUuid() {
		new OrderContext("", "patient-1", "encounter-1", "CBC");
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRejectNullPatientUuid() {
		new OrderContext("order-1", null, "encounter-1", "CBC");
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRejectNullEncounterUuid() {
		new OrderContext("order-1", "patient-1", null, "CBC");
	}
}

package org.openmrs.module.orders.api.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class OrderPayloadTest {

	@Test
	public void shouldBeProcessableWhenNewAndNotVoidedAndNoPreviousOrder() {
		OrderPayload order = buildPayload("NEW", false, false);

		assertTrue(order.isProcessable());
	}

	@Test
	public void shouldNotBeProcessableWhenVoided() {
		OrderPayload order = buildPayload("NEW", true, false);

		assertFalse(order.isProcessable());
	}

	@Test
	public void shouldNotBeProcessableWhenActionIsRevise() {
		OrderPayload order = buildPayload("REVISE", false, true);

		assertFalse(order.isProcessable());
	}

	@Test
	public void shouldNotBeProcessableWhenActionIsDiscontinue() {
		OrderPayload order = buildPayload("DISCONTINUE", false, true);

		assertFalse(order.isProcessable());
	}

	@Test
	public void shouldNotBeProcessableWhenActionIsRenew() {
		OrderPayload order = buildPayload("RENEW", false, true);

		assertFalse(order.isProcessable());
	}

	@Test
	public void shouldNotBeProcessableWhenHasPreviousOrder() {
		OrderPayload order = buildPayload("NEW", false, true);

		assertFalse(order.isProcessable());
	}

	@Test
	public void shouldBeFreshWhenActionNewAndNoPreviousOrder() {
		OrderPayload order = buildPayload("NEW", false, false);

		assertTrue(order.isFresh());
	}

	@Test
	public void shouldNotBeFreshWhenHasPreviousOrder() {
		OrderPayload order = buildPayload("NEW", false, true);

		assertFalse(order.isFresh());
	}

	@Test
	public void shouldNotBeFreshWhenActionIsNotNew() {
		OrderPayload order = buildPayload("REVISE", false, false);

		assertFalse(order.isFresh());
	}

	@Test
	public void shouldConvertToOrderContext() {
		OrderPayload order = new OrderPayload("order-1", "NEW", false, false,
				"patient-1", "encounter-1", "CBC");

		OrderContext context = order.toOrderContext();

		assertEquals("order-1", context.getOrderUuid());
		assertEquals("patient-1", context.getPatientUuid());
		assertEquals("encounter-1", context.getEncounterUuid());
		assertEquals("CBC", context.getConceptDisplay());
	}

	@Test
	public void shouldHandleNullConceptDisplayInConversion() {
		OrderPayload order = new OrderPayload("order-1", "NEW", false, false,
				"patient-1", "encounter-1", null);

		OrderContext context = order.toOrderContext();

		assertNull(context.getConceptDisplay());
	}

	@Test
	public void shouldHandleNullAction() {
		OrderPayload order = buildPayload(null, false, false);

		assertFalse(order.isFresh());
		assertFalse(order.isProcessable());
	}

	private OrderPayload buildPayload(String action, boolean voided, boolean hasPreviousOrder) {
		return new OrderPayload("order-1", action, voided, hasPreviousOrder,
				"patient-1", "encounter-1", "CBC");
	}
}

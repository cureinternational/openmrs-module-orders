package org.openmrs.module.orders.api.events;

import org.bahmni.module.events.api.model.BahmniEventType;
import org.bahmni.module.events.api.model.Event;
import org.junit.Test;
import org.openmrs.module.orders.api.model.OrderPayload;

import java.util.*;

import static org.junit.Assert.*;

public class EncounterPayloadParserTest {

	private static final String ENCOUNTER_UUID = "encounter-uuid-123";

	@Test
	public void shouldExtractOrderFromPayload() {
		Map<String, Object> orderMap = buildOrderMap("order-1", "NEW", false, null, "patient-1", "CBC");
		Event event = buildEvent(Collections.singletonList(orderMap));

		List<OrderPayload> orders = EncounterPayloadParser.extractOrders(event);

		assertEquals(1, orders.size());
		OrderPayload order = orders.get(0);
		assertEquals("order-1", order.getUuid());
		assertEquals("NEW", order.getAction());
		assertFalse(order.isVoided());
		assertFalse(order.isHasPreviousOrder());
		assertEquals("patient-1", order.getPatientUuid());
		assertEquals(ENCOUNTER_UUID, order.getEncounterUuid());
		assertEquals("CBC", order.getConceptDisplay());
	}

	@Test
	public void shouldExtractMultipleOrders() {
		Map<String, Object> order1 = buildOrderMap("order-1", "NEW", false, null, "patient-1", "CBC");
		Map<String, Object> order2 = buildOrderMap("order-2", "REVISE", false, buildPreviousOrder(), "patient-1", "X-Ray");
		Event event = buildEvent(Arrays.asList(order1, order2));

		List<OrderPayload> orders = EncounterPayloadParser.extractOrders(event);

		assertEquals(2, orders.size());
		assertEquals("order-1", orders.get(0).getUuid());
		assertEquals("order-2", orders.get(1).getUuid());
	}

	@Test
	public void shouldReturnEmptyListWhenNoOrders() {
		Event event = buildEvent(Collections.emptyList());

		List<OrderPayload> orders = EncounterPayloadParser.extractOrders(event);

		assertTrue(orders.isEmpty());
	}

	@Test
	public void shouldReturnEmptyListWhenOrdersKeyIsMissing() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("uuid", ENCOUNTER_UUID);
		Event event = new Event(BahmniEventType.BAHMNI_ENCOUNTER_CREATED, payload, ENCOUNTER_UUID);

		List<OrderPayload> orders = EncounterPayloadParser.extractOrders(event);

		assertTrue(orders.isEmpty());
	}

	@Test
	public void shouldConvertNonStringActionUsingStringValueOf() {
		Map<String, Object> orderMap = new LinkedHashMap<>();
		orderMap.put("uuid", "order-1");
		orderMap.put("action", new Object() {
			@Override
			public String toString() {
				return "NEW";
			}
		});
		orderMap.put("previousOrder", null);
		addPatient(orderMap, "patient-1");
		addConcept(orderMap, "CBC");
		Event event = buildEvent(Collections.singletonList(orderMap));

		List<OrderPayload> orders = EncounterPayloadParser.extractOrders(event);

		assertEquals("NEW", orders.get(0).getAction());
		assertTrue(orders.get(0).isFresh());
	}

	@Test
	public void shouldDetectPreviousOrder() {
		Map<String, Object> orderMap = buildOrderMap("order-1", "REVISE", false, buildPreviousOrder(), "patient-1", "CBC");
		Event event = buildEvent(Collections.singletonList(orderMap));

		List<OrderPayload> orders = EncounterPayloadParser.extractOrders(event);

		assertTrue(orders.get(0).isHasPreviousOrder());
	}

	@Test
	public void shouldDetectVoidedOrder() {
		Map<String, Object> orderMap = buildOrderMap("order-1", "NEW", true, null, "patient-1", "CBC");
		Event event = buildEvent(Collections.singletonList(orderMap));

		List<OrderPayload> orders = EncounterPayloadParser.extractOrders(event);

		assertTrue(orders.get(0).isVoided());
	}

	@Test
	public void shouldTreatMissingVoidedFieldAsNotVoided() {
		Map<String, Object> orderMap = new LinkedHashMap<>();
		orderMap.put("uuid", "order-1");
		orderMap.put("action", "NEW");
		orderMap.put("previousOrder", null);
		addPatient(orderMap, "patient-1");
		addConcept(orderMap, "CBC");
		Event event = buildEvent(Collections.singletonList(orderMap));

		List<OrderPayload> orders = EncounterPayloadParser.extractOrders(event);

		assertFalse(orders.get(0).isVoided());
	}

	@Test
	public void shouldHandleMissingPatient() {
		Map<String, Object> orderMap = new LinkedHashMap<>();
		orderMap.put("uuid", "order-1");
		orderMap.put("action", "NEW");
		orderMap.put("previousOrder", null);
		addConcept(orderMap, "CBC");
		Event event = buildEvent(Collections.singletonList(orderMap));

		List<OrderPayload> orders = EncounterPayloadParser.extractOrders(event);

		assertNull(orders.get(0).getPatientUuid());
	}

	@Test
	public void shouldHandleMissingConcept() {
		Map<String, Object> orderMap = new LinkedHashMap<>();
		orderMap.put("uuid", "order-1");
		orderMap.put("action", "NEW");
		orderMap.put("previousOrder", null);
		addPatient(orderMap, "patient-1");
		Event event = buildEvent(Collections.singletonList(orderMap));

		List<OrderPayload> orders = EncounterPayloadParser.extractOrders(event);

		assertNull(orders.get(0).getConceptDisplay());
	}

	@Test
	public void shouldUseEncounterUuidFromEventPayloadId() {
		Map<String, Object> orderMap = buildOrderMap("order-1", "NEW", false, null, "patient-1", "CBC");
		String expectedEncounterUuid = "custom-encounter-uuid";
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("uuid", expectedEncounterUuid);
		payload.put("orders", Collections.singletonList(orderMap));
		Event event = new Event(BahmniEventType.BAHMNI_ENCOUNTER_CREATED, payload, expectedEncounterUuid);

		List<OrderPayload> orders = EncounterPayloadParser.extractOrders(event);

		assertEquals(expectedEncounterUuid, orders.get(0).getEncounterUuid());
	}

	private Event buildEvent(List<Map<String, Object>> orders) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("uuid", ENCOUNTER_UUID);
		if (orders != null) {
			payload.put("orders", orders);
		}
		return new Event(BahmniEventType.BAHMNI_ENCOUNTER_CREATED, payload, ENCOUNTER_UUID);
	}

	private Map<String, Object> buildOrderMap(String uuid, String action, boolean voided,
											  Map<String, Object> previousOrder, String patientUuid, String conceptDisplay) {
		Map<String, Object> orderMap = new LinkedHashMap<>();
		orderMap.put("uuid", uuid);
		orderMap.put("action", action);
		orderMap.put("voided", voided);
		orderMap.put("previousOrder", previousOrder);
		addPatient(orderMap, patientUuid);
		addConcept(orderMap, conceptDisplay);
		return orderMap;
	}

	private void addPatient(Map<String, Object> orderMap, String patientUuid) {
		Map<String, Object> patient = new LinkedHashMap<>();
		patient.put("uuid", patientUuid);
		orderMap.put("patient", patient);
	}

	private void addConcept(Map<String, Object> orderMap, String conceptDisplay) {
		Map<String, Object> concept = new LinkedHashMap<>();
		concept.put("display", conceptDisplay);
		orderMap.put("concept", concept);
	}

	private Map<String, Object> buildPreviousOrder() {
		Map<String, Object> prev = new LinkedHashMap<>();
		prev.put("uuid", "prev-order-uuid");
		return prev;
	}
}

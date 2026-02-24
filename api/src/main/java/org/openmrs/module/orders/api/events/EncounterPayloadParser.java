package org.openmrs.module.orders.api.events;

import org.bahmni.module.events.api.model.Event;
import org.openmrs.module.orders.api.model.OrderPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Parses raw encounter event payloads into typed {@link OrderPayload} objects.
 * Isolates all unchecked Map access and type coercions from the listener.
 */
public final class EncounterPayloadParser {

	private static final String KEY_ORDERS = "orders";
	private static final String KEY_UUID = "uuid";
	private static final String KEY_ACTION = "action";
	private static final String KEY_VOIDED = "voided";
	private static final String KEY_PREVIOUS_ORDER = "previousOrder";
	private static final String KEY_PATIENT = "patient";
	private static final String KEY_CONCEPT = "concept";
	private static final String KEY_DISPLAY = "display";

	private EncounterPayloadParser() {
	}

	public static List<OrderPayload> extractOrders(Event event) {
		String encounterUuid = event.payloadId;
		Map<String, Object> payload = asMap(event.payload);
		List<Map<String, Object>> orderMaps = getOrderMaps(payload);

		List<OrderPayload> orders = new ArrayList<>();
		for (Map<String, Object> orderMap : orderMaps) {
			orders.add(parseOrder(orderMap, encounterUuid));
		}
		return orders;
	}

	private static OrderPayload parseOrder(Map<String, Object> orderMap, String encounterUuid) {
		return new OrderPayload(
				getString(orderMap, KEY_UUID),
				getString(orderMap, KEY_ACTION),
				Boolean.TRUE.equals(orderMap.get(KEY_VOIDED)),
				orderMap.get(KEY_PREVIOUS_ORDER) != null,
				getNestedString(orderMap, KEY_PATIENT, KEY_UUID),
				encounterUuid,
				getNestedString(orderMap, KEY_CONCEPT, KEY_DISPLAY)
		);
	}

	private static String getString(Map<String, Object> map, String key) {
		Object value = map.get(key);
		return value != null ? String.valueOf(value) : null;
	}

	@SuppressWarnings("unchecked")
	private static String getNestedString(Map<String, Object> map, String parentKey, String childKey) {
		Object parent = map.get(parentKey);
		if (parent instanceof Map) {
			return getString((Map<String, Object>) parent, childKey);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object obj) {
		return obj instanceof Map ? (Map<String, Object>) obj : Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> getOrderMaps(Map<String, Object> payload) {
		Object orders = payload.get(KEY_ORDERS);
		return orders instanceof List ? (List<Map<String, Object>>) orders : Collections.emptyList();
	}
}

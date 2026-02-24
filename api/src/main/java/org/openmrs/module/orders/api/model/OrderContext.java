package org.openmrs.module.orders.api.model;

import lombok.Getter;

@Getter
public class OrderContext {

	private final String orderUuid;
	private final String patientUuid;
	private final String encounterUuid;
	private final String conceptDisplay;

	public OrderContext(String orderUuid, String patientUuid, String encounterUuid, String conceptDisplay) {
		if (orderUuid == null || orderUuid.isEmpty()) {
			throw new IllegalArgumentException("orderUuid must not be null or empty");
		}
		if (patientUuid == null || patientUuid.isEmpty()) {
			throw new IllegalArgumentException("patientUuid must not be null or empty");
		}
		if (encounterUuid == null || encounterUuid.isEmpty()) {
			throw new IllegalArgumentException("encounterUuid must not be null or empty");
		}
		this.orderUuid = orderUuid;
		this.patientUuid = patientUuid;
		this.encounterUuid = encounterUuid;
		this.conceptDisplay = conceptDisplay;
	}
}

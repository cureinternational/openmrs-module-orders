package org.openmrs.module.orders.api.model;

import lombok.Getter;

@Getter
public class OrderPayload {

	private static final String ACTION_NEW = "NEW";

	private final String uuid;
	private final String action;
	private final boolean voided;
	private final boolean hasPreviousOrder;
	private final String patientUuid;
	private final String encounterUuid;
	private final String conceptDisplay;

	public OrderPayload(String uuid, String action, boolean voided, boolean hasPreviousOrder,
						String patientUuid, String encounterUuid, String conceptDisplay) {
		this.uuid = uuid;
		this.action = action;
		this.voided = voided;
		this.hasPreviousOrder = hasPreviousOrder;
		this.patientUuid = patientUuid;
		this.encounterUuid = encounterUuid;
		this.conceptDisplay = conceptDisplay;
	}

	public boolean isFresh() {
		return ACTION_NEW.equals(action) && !hasPreviousOrder;
	}

	public boolean isProcessable() {
		return !voided && isFresh();
	}

	public OrderContext toOrderContext() {
		return new OrderContext(uuid, patientUuid, encounterUuid, conceptDisplay);
	}
}

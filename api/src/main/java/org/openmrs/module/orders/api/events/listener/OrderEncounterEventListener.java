package org.openmrs.module.orders.api.events.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bahmni.module.events.api.model.BahmniEventType;
import org.bahmni.module.events.api.model.Event;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.orders.api.events.EncounterPayloadParser;
import org.openmrs.module.orders.api.model.OrderPayload;
import org.openmrs.module.orders.api.service.OrderTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEncounterEventListener {

	private final Log log = LogFactory.getLog(this.getClass());

	@Autowired
	private OrderTaskService orderTaskService;

	@Autowired
	@Qualifier("orderTaskExecutor")
	private TaskExecutor taskExecutor;

	@EventListener
	public void onEvent(Event event) {
		if (!isEncounterEvent(event.eventType)) {
			return;
		}

		List<OrderPayload> orders = EncounterPayloadParser.extractOrders(event);
		if (orders.isEmpty()) {
			return;
		}

		UserContext userContext = Context.getUserContext();
		if (userContext == null) {
			log.warn("No user context available for order processing, encounterUuid=" + event.payloadId);
			return;
		}

		submitOrderProcessing(orders, event.payloadId, userContext);
	}

	private boolean isEncounterEvent(BahmniEventType eventType) {
		return eventType == BahmniEventType.BAHMNI_ENCOUNTER_CREATED
				|| eventType == BahmniEventType.BAHMNI_ENCOUNTER_UPDATED;
	}

	private void submitOrderProcessing(List<OrderPayload> orders, String encounterUuid, UserContext userContext) {
		try {
			taskExecutor.execute(() -> processOrders(orders, encounterUuid, userContext));
		} catch (Exception e) {
			log.error("Failed to submit order processing, encounterUuid=" + encounterUuid, e);
		}
	}

	private void processOrders(List<OrderPayload> orders, String encounterUuid, UserContext userContext) {
		try {
			Context.openSession();
			Context.setUserContext(userContext);

			for (OrderPayload order : orders) {
				processOrder(order);
			}
		} catch (Exception e) {
			log.error("Error processing orders, encounterUuid=" + encounterUuid, e);
		} finally {
			Context.closeSession();
		}
	}

	private void processOrder(OrderPayload order) {
		try {
			if (!order.isProcessable()) {
				log.debug("Skipping non-processable order: " + order.getUuid()
						+ " (action=" + order.getAction() + ", voided=" + order.isVoided() + ")");
				return;
			}

			orderTaskService.createTaskForOrderIfNotExists(order.toOrderContext());
			log.info("Created task for order: " + order.getUuid());
		} catch (Exception e) {
			log.error("Error creating task for order: " + order.getUuid(), e);
		}
	}
}

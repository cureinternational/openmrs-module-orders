package org.openmrs.module.orders.api.service;

import org.openmrs.module.orders.api.model.OrderContext;

public interface OrderTaskService {

	void createTaskForOrderIfNotExists(OrderContext orderContext);

	boolean taskExistsForOrder(String orderUuid);
}

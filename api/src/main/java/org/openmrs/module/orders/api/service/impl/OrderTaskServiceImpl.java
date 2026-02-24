package org.openmrs.module.orders.api.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.fhir2.model.FhirReference;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.orders.api.dao.OrderTaskDao;
import org.openmrs.module.orders.api.model.OrderContext;
import org.openmrs.module.orders.api.service.OrderTaskService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

public class OrderTaskServiceImpl implements OrderTaskService {

	private static final String DEFAULT_TASK_NAME = "Order Status Tracking Task";
	private static final String RESOURCE_PATIENT = "Patient";
	private static final String RESOURCE_SERVICE_REQUEST = "ServiceRequest";
	private static final String RESOURCE_ENCOUNTER = "Encounter";
	private static final String REFERENCE_FORMAT = "%s/%s";

	private final Log log = LogFactory.getLog(this.getClass());

	private OrderTaskDao orderTaskDao;

	public void setOrderTaskDao(OrderTaskDao orderTaskDao) {
		this.orderTaskDao = orderTaskDao;
	}

	@Override
	@Transactional
	public void createTaskForOrderIfNotExists(OrderContext orderContext) {
		String orderUuid = orderContext.getOrderUuid();
		if (taskExistsForOrder(orderUuid)) {
			log.debug("Task already exists for order: " + orderUuid);
			return;
		}

		FhirTask task = buildFhirTask(orderContext);
		orderTaskDao.saveTask(task);
		log.info("Created FHIR Task for order: " + orderUuid);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean taskExistsForOrder(String orderUuid) {
		return orderTaskDao.getTaskByOrderUuid(orderUuid) != null;
	}

	private FhirTask buildFhirTask(OrderContext orderContext) {
		FhirTask task = new FhirTask();
		task.setUuid(UUID.randomUUID().toString());
		task.setName(buildTaskName(orderContext));
		task.setStatus(FhirTask.TaskStatus.REQUESTED);
		task.setIntent(FhirTask.TaskIntent.ORDER);

		task.setForReference(buildReference(RESOURCE_PATIENT, orderContext.getPatientUuid()));
		task.setEncounterReference(buildReference(RESOURCE_ENCOUNTER, orderContext.getEncounterUuid()));
		task.setBasedOnReferences(Collections.singleton(
				buildReference(RESOURCE_SERVICE_REQUEST, orderContext.getOrderUuid())));

		return task;
	}

	private String buildTaskName(OrderContext orderContext) {
		String conceptDisplay = orderContext.getConceptDisplay();
		return conceptDisplay != null ? conceptDisplay : DEFAULT_TASK_NAME;
	}

	private FhirReference buildReference(String resourceType, String uuid) {
		FhirReference reference = new FhirReference();
		reference.setReference(String.format(REFERENCE_FORMAT, resourceType, uuid));
		return reference;
	}
}

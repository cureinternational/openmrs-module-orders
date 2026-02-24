package org.openmrs.module.orders.api.dao;

import org.openmrs.module.fhir2.model.FhirTask;

public interface OrderTaskDao {

	FhirTask getTaskByOrderUuid(String orderUuid);

	FhirTask saveTask(FhirTask task);
}

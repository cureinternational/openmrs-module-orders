package org.openmrs.module.orders.api.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.module.fhir2.model.FhirReference;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.orders.api.dao.OrderTaskDao;
import org.openmrs.module.orders.api.model.OrderContext;

import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OrderTaskServiceImplTest {

	private OrderTaskServiceImpl orderTaskService;

	@Mock
	private OrderTaskDao orderTaskDao;

	private static final String ORDER_UUID = "order-uuid-123";
	private static final String PATIENT_UUID = "patient-uuid-456";
	private static final String ENCOUNTER_UUID = "encounter-uuid-789";
	private static final String CONCEPT_DISPLAY = "Complete Blood Count";

	private OrderContext orderContext;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		orderTaskService = new OrderTaskServiceImpl();
		orderTaskService.setOrderTaskDao(orderTaskDao);

		when(orderTaskDao.saveTask(any(FhirTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

		orderContext = new OrderContext(ORDER_UUID, PATIENT_UUID, ENCOUNTER_UUID, CONCEPT_DISPLAY);
	}

	@Test
	public void shouldCreateTaskWhenNoTaskExistsForOrder() {
		when(orderTaskDao.getTaskByOrderUuid(ORDER_UUID)).thenReturn(null);

		orderTaskService.createTaskForOrderIfNotExists(orderContext);

		verify(orderTaskDao).saveTask(any(FhirTask.class));
	}

	@Test
	public void shouldNotCreateDuplicateTaskWhenTaskAlreadyExists() {
		when(orderTaskDao.getTaskByOrderUuid(ORDER_UUID)).thenReturn(new FhirTask());

		orderTaskService.createTaskForOrderIfNotExists(orderContext);

		verify(orderTaskDao, never()).saveTask(any(FhirTask.class));
	}

	@Test
	public void shouldSetTaskNameToConceptDisplay() {
		when(orderTaskDao.getTaskByOrderUuid(ORDER_UUID)).thenReturn(null);

		orderTaskService.createTaskForOrderIfNotExists(orderContext);

		FhirTask task = captureSavedTask();
		assertEquals(CONCEPT_DISPLAY, task.getName());
	}

	@Test
	public void shouldUseDefaultTaskNameWhenConceptDisplayIsNull() {
		when(orderTaskDao.getTaskByOrderUuid(ORDER_UUID)).thenReturn(null);
		OrderContext contextWithoutConcept = new OrderContext(ORDER_UUID, PATIENT_UUID, ENCOUNTER_UUID, null);

		orderTaskService.createTaskForOrderIfNotExists(contextWithoutConcept);

		FhirTask task = captureSavedTask();
		assertEquals("Order Status Tracking Task", task.getName());
	}

	@Test
	public void shouldSetTaskStatusToDraftOnNewOrder() {
		when(orderTaskDao.getTaskByOrderUuid(ORDER_UUID)).thenReturn(null);

		orderTaskService.createTaskForOrderIfNotExists(orderContext);

		FhirTask task = captureSavedTask();
		assertEquals(FhirTask.TaskStatus.DRAFT, task.getStatus());
	}

	@Test
	public void shouldSetTaskIntentToOrder() {
		when(orderTaskDao.getTaskByOrderUuid(ORDER_UUID)).thenReturn(null);

		orderTaskService.createTaskForOrderIfNotExists(orderContext);

		FhirTask task = captureSavedTask();
		assertEquals(FhirTask.TaskIntent.ORDER, task.getIntent());
	}

	@Test
	public void shouldSetForReferenceToPatient() {
		when(orderTaskDao.getTaskByOrderUuid(ORDER_UUID)).thenReturn(null);

		orderTaskService.createTaskForOrderIfNotExists(orderContext);

		FhirTask task = captureSavedTask();
		FhirReference forRef = task.getForReference();
		assertNotNull(forRef);
		assertEquals("Patient/" + PATIENT_UUID, forRef.getReference());
	}

	@Test
	public void shouldSetEncounterReference() {
		when(orderTaskDao.getTaskByOrderUuid(ORDER_UUID)).thenReturn(null);

		orderTaskService.createTaskForOrderIfNotExists(orderContext);

		FhirTask task = captureSavedTask();
		FhirReference encounterRef = task.getEncounterReference();
		assertNotNull(encounterRef);
		assertEquals("Encounter/" + ENCOUNTER_UUID, encounterRef.getReference());
	}

	@Test
	public void shouldSetBasedOnReferenceToServiceRequest() {
		when(orderTaskDao.getTaskByOrderUuid(ORDER_UUID)).thenReturn(null);

		orderTaskService.createTaskForOrderIfNotExists(orderContext);

		FhirTask task = captureSavedTask();
		Set<FhirReference> basedOn = task.getBasedOnReferences();
		assertNotNull(basedOn);
		assertEquals(1, basedOn.size());
		assertEquals("ServiceRequest/" + ORDER_UUID, basedOn.iterator().next().getReference());
	}

	@Test
	public void shouldGenerateUuidForNewTask() {
		when(orderTaskDao.getTaskByOrderUuid(ORDER_UUID)).thenReturn(null);

		orderTaskService.createTaskForOrderIfNotExists(orderContext);

		FhirTask task = captureSavedTask();
		assertNotNull(task.getUuid());
		assertFalse(task.getUuid().isEmpty());
	}

	@Test
	public void taskExistsForOrderShouldReturnTrueWhenTaskExists() {
		when(orderTaskDao.getTaskByOrderUuid(ORDER_UUID)).thenReturn(new FhirTask());

		assertTrue(orderTaskService.taskExistsForOrder(ORDER_UUID));
	}

	@Test
	public void taskExistsForOrderShouldReturnFalseWhenNoTaskExists() {
		when(orderTaskDao.getTaskByOrderUuid(ORDER_UUID)).thenReturn(null);

		assertFalse(orderTaskService.taskExistsForOrder(ORDER_UUID));
	}

	private FhirTask captureSavedTask() {
		ArgumentCaptor<FhirTask> captor = ArgumentCaptor.forClass(FhirTask.class);
		verify(orderTaskDao).saveTask(captor.capture());
		return captor.getValue();
	}
}

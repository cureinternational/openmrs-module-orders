package org.openmrs.module.orders.api.events.listener;

import org.bahmni.module.events.api.model.BahmniEventType;
import org.bahmni.module.events.api.model.Event;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.orders.api.model.OrderContext;
import org.openmrs.module.orders.api.service.OrderTaskService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Context.class})
@PowerMockIgnore({"javax.management.*"})
public class OrderEncounterEventListenerTest {

	private static final String ENCOUNTER_UUID = "encounter-uuid-123";
	private static final String ORDER_UUID = "order-uuid-456";
	private static final String PATIENT_UUID = "patient-uuid-789";
	private static final String CONCEPT_DISPLAY = "Complete Blood Count";

	@InjectMocks
	private OrderEncounterEventListener listener;

	@Mock
	private OrderTaskService orderTaskService;

	@Spy
	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	@Mock
	private UserContext userContext;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		PowerMockito.mockStatic(Context.class);
		when(Context.getUserContext()).thenReturn(userContext);
	}

	@Test
	public void shouldCreateTaskForNewOrderOnEncounterCreated() {
		Event event = buildEventWithOrders(BahmniEventType.BAHMNI_ENCOUNTER_CREATED,
				buildOrderMap(ORDER_UUID, "NEW", false, null));

		listener.onEvent(event);

		ArgumentCaptor<OrderContext> captor = ArgumentCaptor.forClass(OrderContext.class);
		verify(orderTaskService).createTaskForOrderIfNotExists(captor.capture());
		OrderContext captured = captor.getValue();
		assertEquals(ORDER_UUID, captured.getOrderUuid());
		assertEquals(PATIENT_UUID, captured.getPatientUuid());
		assertEquals(ENCOUNTER_UUID, captured.getEncounterUuid());
		assertEquals(CONCEPT_DISPLAY, captured.getConceptDisplay());
	}

	@Test
	public void shouldCreateTaskForNewOrderOnEncounterUpdated() {
		Event event = buildEventWithOrders(BahmniEventType.BAHMNI_ENCOUNTER_UPDATED,
				buildOrderMap(ORDER_UUID, "NEW", false, null));

		listener.onEvent(event);

		verify(orderTaskService).createTaskForOrderIfNotExists(any(OrderContext.class));
	}

	@Test
	public void shouldSkipReviseOrder() {
		Event event = buildEventWithOrders(BahmniEventType.BAHMNI_ENCOUNTER_CREATED,
				buildOrderMap(ORDER_UUID, "REVISE", false, buildPreviousOrder()));

		listener.onEvent(event);

		verify(orderTaskService, never()).createTaskForOrderIfNotExists(any());
	}

	@Test
	public void shouldSkipDiscontinueOrder() {
		Event event = buildEventWithOrders(BahmniEventType.BAHMNI_ENCOUNTER_CREATED,
				buildOrderMap(ORDER_UUID, "DISCONTINUE", false, buildPreviousOrder()));

		listener.onEvent(event);

		verify(orderTaskService, never()).createTaskForOrderIfNotExists(any());
	}

	@Test
	public void shouldSkipRenewOrder() {
		Event event = buildEventWithOrders(BahmniEventType.BAHMNI_ENCOUNTER_CREATED,
				buildOrderMap(ORDER_UUID, "RENEW", false, buildPreviousOrder()));

		listener.onEvent(event);

		verify(orderTaskService, never()).createTaskForOrderIfNotExists(any());
	}

	@Test
	public void shouldSkipVoidedOrders() {
		Event event = buildEventWithOrders(BahmniEventType.BAHMNI_ENCOUNTER_CREATED,
				buildOrderMap(ORDER_UUID, "NEW", true, null));

		listener.onEvent(event);

		verify(orderTaskService, never()).createTaskForOrderIfNotExists(any());
	}

	@Test
	public void shouldIgnoreNonEncounterEvents() {
		Event event = buildEventWithOrders(BahmniEventType.BAHMNI_PATIENT_CREATED,
				buildOrderMap(ORDER_UUID, "NEW", false, null));

		listener.onEvent(event);

		verify(orderTaskService, never()).createTaskForOrderIfNotExists(any());
	}

	@Test
	public void shouldSkipProcessingWhenUserContextIsNull() {
		when(Context.getUserContext()).thenReturn(null);
		Event event = buildEventWithOrders(BahmniEventType.BAHMNI_ENCOUNTER_CREATED,
				buildOrderMap(ORDER_UUID, "NEW", false, null));

		listener.onEvent(event);

		verify(orderTaskService, never()).createTaskForOrderIfNotExists(any());
	}

	@Test
	public void shouldOnlyCreateTaskForFreshOrderInMixedSet() {
		Map<String, Object> newOrder = buildOrderMap("new-order", "NEW", false, null);
		Map<String, Object> reviseOrder = buildOrderMap("revise-order", "REVISE", false, buildPreviousOrder());
		Map<String, Object> discontinueOrder = buildOrderMap("disc-order", "DISCONTINUE", false, buildPreviousOrder());
		Map<String, Object> voidedOrder = buildOrderMap("voided-order", "NEW", true, null);

		Event event = buildEvent(BahmniEventType.BAHMNI_ENCOUNTER_CREATED,
				Arrays.asList(newOrder, reviseOrder, discontinueOrder, voidedOrder));

		listener.onEvent(event);

		ArgumentCaptor<OrderContext> captor = ArgumentCaptor.forClass(OrderContext.class);
		verify(orderTaskService, times(1)).createTaskForOrderIfNotExists(captor.capture());
		assertEquals("new-order", captor.getValue().getOrderUuid());
	}

	@Test
	public void shouldProcessMultipleNewOrdersIndependently() {
		Map<String, Object> order1 = buildOrderMap("order-1", "NEW", false, null);
		Map<String, Object> order2 = buildOrderMap("order-2", "NEW", false, null);

		Event event = buildEvent(BahmniEventType.BAHMNI_ENCOUNTER_CREATED, Arrays.asList(order1, order2));

		listener.onEvent(event);

		verify(orderTaskService, times(2)).createTaskForOrderIfNotExists(any(OrderContext.class));
	}

	@Test
	public void shouldContinueProcessingAfterSingleOrderError() {
		Map<String, Object> order1 = buildOrderMap("order-1", "NEW", false, null);
		Map<String, Object> order2 = buildOrderMap("order-2", "NEW", false, null);

		doThrow(new RuntimeException("DB error"))
				.when(orderTaskService).createTaskForOrderIfNotExists(argThat(ctx -> "order-1".equals(ctx.getOrderUuid())));

		Event event = buildEvent(BahmniEventType.BAHMNI_ENCOUNTER_CREATED, Arrays.asList(order1, order2));

		listener.onEvent(event);

		verify(orderTaskService, times(2)).createTaskForOrderIfNotExists(any(OrderContext.class));
	}

	@Test
	public void shouldSkipEncounterWithNoOrders() {
		Event event = buildEvent(BahmniEventType.BAHMNI_ENCOUNTER_CREATED, Collections.emptyList());

		listener.onEvent(event);

		verify(orderTaskService, never()).createTaskForOrderIfNotExists(any());
	}

	@Test
	public void shouldSkipEncounterWithNullOrders() {
		Event event = buildEvent(BahmniEventType.BAHMNI_ENCOUNTER_CREATED, null);

		listener.onEvent(event);

		verify(orderTaskService, never()).createTaskForOrderIfNotExists(any());
	}

	@Test
	public void shouldOpenAndCloseSessionForAsyncProcessing() {
		Event event = buildEventWithOrders(BahmniEventType.BAHMNI_ENCOUNTER_CREATED,
				buildOrderMap(ORDER_UUID, "NEW", false, null));

		listener.onEvent(event);

		PowerMockito.verifyStatic(Context.class);
		Context.openSession();

		PowerMockito.verifyStatic(Context.class);
		Context.setUserContext(userContext);

		PowerMockito.verifyStatic(Context.class);
		Context.closeSession();
	}

	private Event buildEventWithOrders(BahmniEventType eventType, Map<String, Object> order) {
		return buildEvent(eventType, Collections.singletonList(order));
	}

	private Event buildEvent(BahmniEventType eventType, List<Map<String, Object>> orders) {
		Map<String, Object> encounterPayload = new LinkedHashMap<>();
		encounterPayload.put("uuid", ENCOUNTER_UUID);
		if (orders != null) {
			encounterPayload.put("orders", orders);
		}
		return new Event(eventType, encounterPayload, ENCOUNTER_UUID);
	}

	private Map<String, Object> buildOrderMap(String uuid, String action, boolean voided, Map<String, Object> previousOrder) {
		Map<String, Object> order = new LinkedHashMap<>();
		order.put("uuid", uuid);
		order.put("action", action);
		order.put("voided", voided);
		order.put("previousOrder", previousOrder);

		Map<String, Object> patient = new LinkedHashMap<>();
		patient.put("uuid", PATIENT_UUID);
		order.put("patient", patient);

		Map<String, Object> concept = new LinkedHashMap<>();
		concept.put("display", CONCEPT_DISPLAY);
		order.put("concept", concept);

		return order;
	}

	private Map<String, Object> buildPreviousOrder() {
		Map<String, Object> prev = new LinkedHashMap<>();
		prev.put("uuid", "prev-order-uuid");
		return prev;
	}
}

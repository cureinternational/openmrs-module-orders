package org.openmrs.module.orders.api.dao.impl;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.orders.api.dao.OrderTaskDao;

public class HibernateOrderTaskDao implements OrderTaskDao {

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public FhirTask getTaskByOrderUuid(String orderUuid) {
		Criteria criteria = sessionFactory.getCurrentSession()
				.createCriteria(FhirTask.class)
				.createAlias("basedOnReferences", "bor")
				.add(Restrictions.eq("bor.targetUuid", orderUuid))
				.setMaxResults(1);
		return (FhirTask) criteria.uniqueResult();
	}

	@Override
	public FhirTask saveTask(FhirTask task) {
		sessionFactory.getCurrentSession().saveOrUpdate(task);
		return task;
	}
}

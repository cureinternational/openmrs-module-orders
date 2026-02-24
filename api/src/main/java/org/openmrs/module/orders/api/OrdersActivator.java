package org.openmrs.module.orders.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.BaseModuleActivator;

public class OrdersActivator extends BaseModuleActivator {

	private final Log log = LogFactory.getLog(this.getClass());

	public void startup() {
		log.info("Starting Bahmni Orders Module");
	}

	public void shutdown() {
		log.info("Shutting down Bahmni Orders Module");
	}

}

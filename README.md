# openmrs-module-orders
OpenMRS module for FHIR Task creation on order events

OpenMRS Module Bahmni Orders Backend
=================================
This repository listens to encounter events and creates FHIR Tasks for order tracking.

## Packaging
```mvn clean package```

### Prerequisite
    JDK 1.8

## Deploy

Copy ```openmrs-module-orders/omod/target/bahmni-orders-1.0.0-SNAPSHOT.omod``` into OpenMRS modules directory and restart OpenMRS

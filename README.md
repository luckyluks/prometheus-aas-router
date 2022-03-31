# Prometheus AAS Router

This Router App pushes data from [Prometheus](https://prometheus.io/) to the [Asset Administation Shell](https://www.plattform-i40.de/IP/Redaktion/EN/Standardartikel/specification-administrationshell.html), the Digital Twin Solution for Industry 4.0.

It is based on the AAS solution of [Eclipse Basyx](https://www.eclipse.org/basyx/), the [Basyx AAS Updater](https://github.com/eclipse-basyx/basyx-java-components/tree/feature/updater/basyx.components/basyx.components.updater).

Note:
This app is currently using [a fork of said Basyx Updater](https://github.com/n14s/basyx-java-components/tree/n14s/feature/prometheus-updater/basyx.components/basyx.components.updater), where the Prometheus component is already integrated. The integration to the [upstream repository](https://github.com/eclipse-basyx/basyx-java-components/tree/feature/updater/basyx.components/basyx.components.updater) will follow soon.

The easiest way to try it out is by running the [docker-compose stack containing the Router App](https://github.com/n14s/prometheus-aas-docker).

## Goal

Integrate device metric data, eg. performance metrics, in its digital twin representation: the Asset Administation Shell.

## Requirements

- Java 8
- Maven
- Prometheus with Node exporter ([available as Docker image](https://prometheus.io/docs/prometheus/latest/installation/#using-docker))
- AAS Server ([eg. from Basyx, available as Docker image](https://wiki.eclipse.org/BaSyx_/_Documentation_/_Components_/_AAS_Server))
- local maven installation of the following modules of [this branch of the Basyx Updater fork](https://github.com/n14s/basyx-java-components/tree/feature/updater-provisional)  
  (A more convenient integration of those modules through mvn-repository will follow):
  - basyx.components.updater.core
  - basyx.components.updater.camel-aas
  - basyx.components.updater.camel-prometheus
  - basyx.components.updater.camel-timer
  - basyx.components.updater.transformer.camel-jsonata

## Configuration

The Prometheus component, the AAS component, the Jsonata transformer, the timer as well as the route can be configured using the config files in the resource folder.

A detailed description of the configuration can be found in the [project's docs](https://github.com/n14s/basyx-java-components/tree/feature/updater/basyx.components/basyx.components.updater).

## Running in IDE

If all requirements are met, the app can be launched within the prefered IDE.

## Building JAR

The aim of this project is to deploy the app within large environments of heterogeneous devices to monitor their current state.
Building a fat JAR (JAR with dependencies integrated) allows for easy deployment on a large variety of devices, only requiring a java 8 runtime environment.

A `mvn package` command results in a jar, built with all dependencies within.

## Running JAR

To run the JAR, place it inside a dir together with the config file folder.
Building this JAR results in such a setup within the target folder by default.
The JAR can be executed with  
`java -jar prometheus-updater-1.0-SNAPSHOT-jar-with-dependencies.jar`

A minimal dir-setup in deployment would look like this:

```
prometheus-aas-router-example/
├── config
│   ├── aasserver.json
│   ├── jsonataA.json
│   ├── jsonatatransformer.json
│   ├── logback.xml
│   ├── prometheus.json
│   ├── routes.json
│   └── timerconsumer.json
└── prometheus-aas-router-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Docker

Docker makes deployment even easier.

The Prometheus-AAS-Router app is available as Docker image on [Docker Hub](https://hub.docker.com/r/n14s/prometheus-aas-router).
Download the image using  
`docker pull n14s/prometheus-aas-router`

Run the image by moving into the dir, where your config folder resides, and executing  
`docker run --network="host" -v $(pwd)/config:/app/config prometheus-aas-router:1.0.0`

## Docker Compose

A ready to run Docker Compose stack including the Prometheus-AAS-Router, Prometheus, Node exporter and the AAS-server can be found under  
https://github.com/n14s/prometheus-aas-docker

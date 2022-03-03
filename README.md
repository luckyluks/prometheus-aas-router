# Prometheus AAS Router

This Router App pushes data from [Prometheus](https://prometheus.io/) to the [Asset Administation Shell](https://www.plattform-i40.de/IP/Redaktion/EN/Standardartikel/specification-administrationshell.html), the Digital Twin Solution for Industry 4.0.

It is based on the AAS solution of [Eclipse Basyx](https://www.eclipse.org/basyx/), the [Basyx AAS Updater](https://github.com/eclipse-basyx/basyx-java-components/tree/feature/updater/basyx.components/basyx.components.updater).

Note:
This app is currently using [a fork of said Basyx Updater](https://github.com/n14s/basyx-java-components/tree/n14s/feature/prometheus-updater/basyx.components/basyx.components.updater), where the Prometheus component is already integrated. The integration to the [upstream repository](https://github.com/eclipse-basyx/basyx-java-components/tree/feature/updater/basyx.components/basyx.components.updater) will follow soon.

## Goal

Integrate device data, eg. performance metrics, in its digital twin representation: the asset administation shell.

## Requirements

- Java 8
- Maven
- Prometheus with Nodeexporter ([available as Docker image](https://prometheus.io/docs/prometheus/latest/installation/#using-docker))
- AAS Server ([eg. from Basyx, available as Docker image](https://wiki.eclipse.org/BaSyx_/_Documentation_/_Components_/_AAS_Server))
- local maven installation of the following modules of [the Basyx Updater fork](https://github.com/n14s/basyx-java-components/tree/n14s/feature/prometheus-updater/basyx.components/basyx.components.updater)  
  (Amoreconvenientintegrationofthosemodulesthroughmvnrepository.comwillfollow):
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

## Building a JAR

The aim of this project is to deploy the app within large environments of heterogeneous devices to monitor their current state.
A built JAR allows for deployment on a large variety of devices.
A `mvn package` command results in a jar, built with all dependencies within.
Config files are placed outside of the jar, to be modified when needed.

## Docker

A Docker Image takes this though a step further.
This task is currently in progress...

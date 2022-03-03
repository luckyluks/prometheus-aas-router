package com.fraunhofer.ipa.prometheusaasrouter;

import basyx.components.updater.aas.configuration.factory.AASProducerDefaultConfigurationFactory;
import basyx.components.updater.camelprometheus.configuration.factory.PrometheusDefaultConfigurationFactory;
import basyx.components.updater.cameltimer.configuration.factory.TimerDefaultConfigurationFactory;
import basyx.components.updater.core.component.UpdaterComponent;
import basyx.components.updater.core.configuration.factory.DefaultRoutesConfigurationFactory;
import basyx.components.updater.core.configuration.route.RoutesConfiguration;
import basyx.components.updater.transformer.cameljsonata.configuration.factory.JsonataDefaultConfigurationFactory;

public class PrometheusAASRouter {

    private static UpdaterComponent updater;

    public static void main(String[] args) {
        ClassLoader loader = PrometheusAASRouter.class.getClassLoader();
        RoutesConfiguration configuration = new RoutesConfiguration();

        // Extend configuration for connections
        // DefaultRoutesConfigFac takes default routes.json as to config
        DefaultRoutesConfigurationFactory routesFactory = new DefaultRoutesConfigurationFactory("config/routes.json", loader);
        configuration.addRoutes(routesFactory.getRouteConfigurations());

        // Extend configuration for prometheus Source
        PrometheusDefaultConfigurationFactory prometheusConfigFactory = new PrometheusDefaultConfigurationFactory("config/prometheus.json", loader);
        configuration.addDatasinks(prometheusConfigFactory.getDataSinkConfigurations());

        // Extend configuration for AAS
        // DefaultRoutesConfigFactory takes default aasserver.json as to config
        AASProducerDefaultConfigurationFactory aasConfigFactory = new AASProducerDefaultConfigurationFactory("config/aasserver.json", loader);
        configuration.addDatasinks(aasConfigFactory.getDataSinkConfigurations());

        // Extend configuration for Jsonata
        JsonataDefaultConfigurationFactory jsonataConfigFactory = new JsonataDefaultConfigurationFactory("config/jsonatatransformer.json", loader);
        configuration.addTransformers(jsonataConfigFactory.getDataTransformerConfigurations());

        // Extend configuration for Timer
        TimerDefaultConfigurationFactory timerConfigFactory = new TimerDefaultConfigurationFactory("config/timerconsumer.json", loader);
        configuration.addDatasources(timerConfigFactory.getDataSourceConfigurations());


        updater = new UpdaterComponent(configuration);
        updater.startComponent();
        System.out.println("UPDATER STARTED");
        while(true) {}
    }
}

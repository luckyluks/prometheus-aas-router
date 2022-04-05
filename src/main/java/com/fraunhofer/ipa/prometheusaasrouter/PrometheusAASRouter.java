package com.fraunhofer.ipa.prometheusaasrouter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.basyx.components.aas.AASServerComponent;
import org.eclipse.basyx.components.aas.configuration.AASServerBackend;
import org.eclipse.basyx.components.aas.configuration.BaSyxAASServerConfiguration;
import org.eclipse.basyx.components.configuration.BaSyxContextConfiguration;
import org.eclipse.basyx.components.registry.RegistryComponent;
import org.eclipse.basyx.components.registry.configuration.BaSyxRegistryConfiguration;
import org.eclipse.basyx.components.registry.configuration.RegistryBackend;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.xml.sax.SAXException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.eclipse.basyx.aas.bundle.AASBundle;
import org.eclipse.basyx.aas.bundle.AASBundleHelper;
import org.eclipse.basyx.aas.factory.aasx.AASXToMetamodelConverter;
import org.eclipse.basyx.aas.manager.ConnectedAssetAdministrationShellManager;
import org.eclipse.basyx.aas.metamodel.map.AssetAdministrationShell;
import org.eclipse.basyx.aas.registration.proxy.AASRegistryProxy;
import org.eclipse.basyx.components.IComponent;

import basyx.components.updater.aas.configuration.factory.AASProducerDefaultConfigurationFactory;
import basyx.components.updater.camelprometheus.configuration.factory.PrometheusDefaultConfigurationFactory;
import basyx.components.updater.cameltimer.configuration.factory.TimerDefaultConfigurationFactory;
import basyx.components.updater.core.component.UpdaterComponent;
import basyx.components.updater.core.configuration.factory.DefaultRoutesConfigurationFactory;
import basyx.components.updater.core.configuration.route.RoutesConfiguration;
import basyx.components.updater.transformer.cameljsonata.configuration.factory.JsonataDefaultConfigurationFactory;

public class PrometheusAASRouter {
    
    public static final String REGISTRY_URL = "http://registry:4000/registry";
	public static final String SERVER_URL = "http://localhost:4001/aasServer";

    private static UpdaterComponent updater;
    private List<IComponent> startedComponents = new ArrayList<>(); 

    public static void main(String[] args) throws InvalidFormatException, IOException, ParserConfigurationException, SAXException {

        new PrometheusAASRouter();
        
        // while(true) {}
    }

    public PrometheusAASRouter() throws InvalidFormatException, IOException, ParserConfigurationException, SAXException {
        // startRegistry();
        startAASServer();

        System.out.println("loading manager");
        ConnectedAssetAdministrationShellManager manager =
				new ConnectedAssetAdministrationShellManager(new AASRegistryProxy(REGISTRY_URL));
		
		// Load Bundles from .aasx file
        System.out.println("loading package");
		AASXToMetamodelConverter packageManager = new AASXToMetamodelConverter("/usr/share/config/MVP-AAS.aasx");
        Set<AASBundle> bundles = packageManager.retrieveAASBundles();
        System.out.println("retrieved bundles");
		

		// Create the AAS on the server
		bundles.forEach((bundle) -> {
			// Get the ID of the AAS
			AssetAdministrationShell aas = (AssetAdministrationShell) bundle.getAAS();
			IIdentifier aasid = aas.getIdentification();
            System.out.println("found aasid: " + aasid.toString());



			// Create the AAS on the server
			manager.createAAS(aas, SERVER_URL);

			// create the Submodels
			bundle.getSubmodels().forEach(submodel -> {
				manager.createSubmodel(aasid, (Submodel) submodel);
			});

            System.out.println("registered: " + aasid.toString());

		});

        System.out.println("start registry proxy manager");
		// Get a RegistryProxy and register all Objects contained in the Bundles
		AASRegistryProxy proxy = new AASRegistryProxy(REGISTRY_URL);
		AASBundleHelper.register(proxy, bundles, SERVER_URL + "/shells");


        System.out.println("SLEEPING 5s");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        startUpdater();
    }

    /**
	 * Starts the updater
	 */
	private void startUpdater() {
		ClassLoader loader = PrometheusAASRouter.class.getClassLoader();
        RoutesConfiguration configuration = new RoutesConfiguration();

        // Extend configuration for connections
        // DefaultRoutesConfigFac takes default routes.json as to config
        DefaultRoutesConfigurationFactory routesFactory = new DefaultRoutesConfigurationFactory("./config/routes.json", loader);
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
        System.out.println("UPDATER STARTED - LUR LURLUR");
	}

    

    /**
	 * Starts an empty registry at "http://localhost:4000"
	 */
	private void startRegistry() {
		// Load a registry context configuration using a .properties file

        BaSyxContextConfiguration contextConfig = new BaSyxContextConfiguration(4000, "registry");
		BaSyxRegistryConfiguration registryConfig = new BaSyxRegistryConfiguration(RegistryBackend.INMEMORY);
		// contextConfig.loadFromResource("RegistryContext.properties");
		RegistryComponent registry = new RegistryComponent(contextConfig, registryConfig);
		registry.startComponent();
		startedComponents.add(registry);
	}

	/**
	 * Startup an empty server at "http://localhost:4001/"
	 */	
	private void startAASServer() {
		// Create a server at port 4001 with the endpoint ""
		BaSyxContextConfiguration contextConfig = new BaSyxContextConfiguration(4001, "/aasServer");
		
        // Create the AAS - Can alternatively also be loaded from a .property file
		BaSyxAASServerConfiguration aasServerConfig = new BaSyxAASServerConfiguration(AASServerBackend.INMEMORY, "/usr/share/config/MVP-AAS.aasx", "http://localhost:4000/registry");

        AASServerComponent aasServer = new AASServerComponent(contextConfig);
        // AASServerComponent aasServer = new AASServerComponent(contextConfig, aasServerConfig);


		
		// Start the created server
		aasServer.startComponent();
		startedComponents.add(aasServer);
	}
}

package com.fraunhofer.ipa.prometheusaasrouter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.basyx.components.aas.AASServerComponent;
import org.eclipse.basyx.components.configuration.BaSyxContextConfiguration;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IdentifierType;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.identifier.Identifier;
import org.eclipse.basyx.submodel.metamodel.map.reference.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.eclipse.basyx.aas.bundle.AASBundle;
import org.eclipse.basyx.aas.bundle.AASBundleHelper;
import org.eclipse.basyx.aas.factory.aasx.AASXToMetamodelConverter;
import org.eclipse.basyx.aas.manager.ConnectedAssetAdministrationShellManager;
import org.eclipse.basyx.aas.metamodel.api.parts.asset.AssetKind;
import org.eclipse.basyx.aas.metamodel.map.AssetAdministrationShell;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.basyx.aas.metamodel.map.parts.Asset;
import org.eclipse.basyx.aas.registration.proxy.AASRegistryProxy;
import org.eclipse.basyx.components.IComponent;

import basyx.components.updater.aas.configuration.AASDatasinkConfiguration;
import basyx.components.updater.aas.configuration.factory.AASProducerDefaultConfigurationFactory;
import basyx.components.updater.camelprometheus.configuration.factory.PrometheusDefaultConfigurationFactory;
import basyx.components.updater.cameltimer.configuration.factory.TimerDefaultConfigurationFactory;
import basyx.components.updater.core.component.UpdaterComponent;
import basyx.components.updater.core.configuration.DataSinkConfiguration;
import basyx.components.updater.core.configuration.factory.DefaultRoutesConfigurationFactory;
import basyx.components.updater.core.configuration.route.RoutesConfiguration;
import basyx.components.updater.transformer.cameljsonata.configuration.factory.JsonataDefaultConfigurationFactory;

public class PrometheusAASRouter {
    
    // Defined defaults, are used if not overwritten via environment variables
    public static final String REGISTRY_URL = "http://localhost:4000/registry";
    public static final String SERVER_URL = "http://localhost:4001/aasServer";
    public static final String HOSTPATH_URL = "http://localhost:4001/aasServer/shells";
    public static final String UPDATER_CONFIG_DIR = "./config";
    public static final String AASX_PATH = "/usr/share/config/AAS.aasx";
    public static final String ASSET_ID = "";

    private static Logger logger = LoggerFactory.getLogger(PrometheusAASRouter.class);
    private static List<IComponent> startedComponents = new ArrayList<>();
    private URL registryUrl;
    private URL serverUrl; 
    private URL hostpathUrl;
    private String updaterConfigDirectory;
    private String aasxFilepath;
    private String assetID;

    public static void main(String[] args) throws InvalidFormatException, IOException, ParserConfigurationException, SAXException {
        new PrometheusAASRouter();
    }

    public PrometheusAASRouter() throws InvalidFormatException, IOException, ParserConfigurationException, SAXException {

        // Fetch local environment if available
        this.registryUrl= new URL(System.getenv().getOrDefault("REGISTRY_URL", REGISTRY_URL));
        this.serverUrl = new URL(System.getenv().getOrDefault("SERVER_URL", SERVER_URL));
        this.hostpathUrl = new URL(System.getenv().getOrDefault("HOSTPATH_URL", HOSTPATH_URL));
        this.updaterConfigDirectory = System.getenv().getOrDefault("UPDATER_CONFIG_DIR", UPDATER_CONFIG_DIR);
        this.aasxFilepath = System.getenv().getOrDefault("AASX_PATH", AASX_PATH);
        this.assetID = System.getenv().getOrDefault("ASSET_ID", ASSET_ID);
        
        // Starting the server
        logger.info("Starting up AAS server ...");
        startAASServer();
        logger.info("AAS server started");

        // Register local AAS, if exists
        if ((new File(this.aasxFilepath)).exists()){
            loadAndRegisterAAS();
        } else {
            throw new FileNotFoundException(String.format("Provided path '%s' does not exists!", this.aasxFilepath));
        }

        // Finally start updater
        startUpdater();

        // Setup shutdown procedure
        Thread shutdownHookThread = initializeShutdownHookThread(this.registryUrl.toString(), this.assetID);
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);
    }


    /**
     * Load local AAS to server and register to registry
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws InvalidFormatException
     */
    private void loadAndRegisterAAS() throws InvalidFormatException, IOException, ParserConfigurationException, SAXException{

        logger.info("Configuring connected AAS manager with registry url: " + this.registryUrl);
        ConnectedAssetAdministrationShellManager manager =
				new ConnectedAssetAdministrationShellManager(new AASRegistryProxy(this.registryUrl.toString()));
		
		// Load Bundles from .aasx file
        logger.info("Loading packaged AASX bundle from: " + this.aasxFilepath);
		AASXToMetamodelConverter packageManager = new AASXToMetamodelConverter(this.aasxFilepath);
        Set<AASBundle> bundles = packageManager.retrieveAASBundles();
        logger.info("Retrieved '{}' bundles from AASX package", bundles.size());
		

		// Create the AAS on the server
		bundles.forEach((bundle) -> {
			// Get the ID of the AAS
			AssetAdministrationShell aas = (AssetAdministrationShell) bundle.getAAS();
            
            if(this.assetID != null && !this.assetID.isEmpty()){

                String assetIdOld = aas.getIdentification().toString();
                logger.info("Found assetID '{}' to overwrite", assetIdOld);
                aas.setIdentification(IdentifierType.CUSTOM, this.assetID); // alternatively use aasID
                
                // Additionally use asset - but this is not yet possible to be used in the url
                Asset newAsset = new Asset(this.assetID, new CustomId(this.assetID), AssetKind.INSTANCE);
                aas.setAsset(newAsset); 
                aas.setAssetReference((Reference) newAsset.getAssetIdentificationModel());
                aas.setIdShort(this.assetID);
            
                logger.info("AAS asset ID overwritten from '{}' to '{}'", assetIdOld, aas.getIdentification().toString());
            } else {
                logger.info("No external assetID found. Using '{}' from bundle", aas.getIdentification().toString());
            }

            // Create the AAS on the server
			IIdentifier aasid = aas.getIdentification();
            logger.info("Creating aas '{}' on server '{}'", aasid.toString(), this.serverUrl);
			manager.createAAS(aas, this.serverUrl.toString());
            logger.info("Created AAS: " + aasid.toString());

			// Create the Submodels
			bundle.getSubmodels().forEach(submodel -> {
				manager.createSubmodel(aasid, (Submodel) submodel);
                logger.info("Created submodel: " + submodel.getIdShort());
			});

		});

        // Get a RegistryProxy and register all Objects contained in the Bundles
        logger.info("Starting registry proxy for registry url '{}' bound to AAS server url '{}'", this.registryUrl, this.serverUrl);
		AASRegistryProxy proxy = new AASRegistryProxy(this.registryUrl.toString());
        AASBundleHelper.register(proxy, bundles, this.hostpathUrl.toString());
        
        logger.info("Finished registration successfully");
    };


    /**
	 * Starts the updater
	 */
	private void startUpdater() {
		ClassLoader loader = PrometheusAASRouter.class.getClassLoader();
        RoutesConfiguration configuration = new RoutesConfiguration();

        logger.info("Starting to configure the updater with config from directory: {}", this.updaterConfigDirectory);

        // Extend configuration for connections
        // DefaultRoutesConfigFac takes default routes.json as to config
        DefaultRoutesConfigurationFactory routesFactory = new DefaultRoutesConfigurationFactory(this.updaterConfigDirectory + "/routes.json", loader);
        configuration.addRoutes(routesFactory.getRouteConfigurations());

        // Extend configuration for prometheus Source
        PrometheusDefaultConfigurationFactory prometheusConfigFactory = new PrometheusDefaultConfigurationFactory(this.updaterConfigDirectory + "/prometheus.json", loader);
        configuration.addDatasinks(prometheusConfigFactory.getDataSinkConfigurations());

        // Extend configuration for AAS
        // DefaultRoutesConfigFactory takes default aasserver.json as to config
        AASProducerDefaultConfigurationFactory aasConfigFactory = new AASProducerDefaultConfigurationFactory(this.updaterConfigDirectory + "/aasserver.json", loader);
        configuration.addDatasinks(aasConfigFactory.getDataSinkConfigurations());
        // Adapt the assetID in loaded config
        configuration.setDatasinks(adaptAssetId(configuration.getDatasinks()));

        // Extend configuration for Jsonata
        JsonataDefaultConfigurationFactory jsonataConfigFactory = new JsonataDefaultConfigurationFactory(this.updaterConfigDirectory + "/jsonatatransformer.json", loader);
        configuration.addTransformers(jsonataConfigFactory.getDataTransformerConfigurations());

        // Extend configuration for Timer
        TimerDefaultConfigurationFactory timerConfigFactory = new TimerDefaultConfigurationFactory(this.updaterConfigDirectory + "/timerconsumer.json", loader);
        configuration.addDatasources(timerConfigFactory.getDataSourceConfigurations());
        
        // Instantiate updater
        UpdaterComponent updater = new UpdaterComponent(configuration);
        logger.info("Updater configuration done");

        // Start updater
        updater.startComponent();
        startedComponents.add((IComponent) updater);
        logger.info("Updater started successfully");
	}

    /**
     * Adapts the given datasinks in order to match the given assetID
     * @param datasinks
     * @return
     */
	private Map<String, DataSinkConfiguration> adaptAssetId(Map<String, DataSinkConfiguration> datasinks) {

        if (this.assetID.isEmpty()){
            throw new RuntimeException("Variable assetID cannot take in an empty String or null value! Consider to set environment variable 'ASSET_ID'");

        }

        for (Map.Entry<String, DataSinkConfiguration> datasink : datasinks.entrySet()) {
            if (datasink.getValue().getClass().equals(AASDatasinkConfiguration.class)){
                ((AASDatasinkConfiguration) datasink.getValue()).setEndpoint(((AASDatasinkConfiguration) datasink.getValue()).getEndpoint().replace("assetID", this.assetID));
                logger.info("Replaced 'assetID' with '{}' in endpointURL '{}'", this.assetID, ((AASDatasinkConfiguration) datasink.getValue()).getEndpoint());
            }
        }
        return datasinks;
    }


	/**
	 * Startup an local AAS server
	 * @throws MalformedURLException
	 */	
	private void startAASServer() throws MalformedURLException {
		
        // Parse URL to context config
		BaSyxContextConfiguration contextConfig = new BaSyxContextConfiguration(this.serverUrl.getPort(), this.serverUrl.getPath());
		
        // Create the AAS - Can alternatively also be loaded from a .property file
		// BaSyxAASServerConfiguration aasServerConfig = new BaSyxAASServerConfiguration(
        //     AASServerBackend.INMEMORY,
        //     "", // this.aasxFilepath,
        //     this.registryUrl.toString(),
        //     this.hostpathUrl.toString()
        // );
        // AASServerComponent aasServer = new AASServerComponent(contextConfig, aasServerConfig);
        AASServerComponent aasServer = new AASServerComponent(contextConfig);

		// Start the created server
		aasServer.startComponent();
		startedComponents.add(aasServer);
	}

    
    /**
     * Initialize Thread for shutdown hook, to deregister the AAS and shutdown components gracefully
     * @param registryUrl the registry url to deregister the AAS
     * @param assetID the ID of the asset, in this case the aas
     * @return the Thread object to be used
     */
    private Thread initializeShutdownHookThread(String registryUrl, String assetID) {
        return new Thread(
            new Runnable() {
                private String registryUrl;
                private String assetID;

                public Runnable init(String registryUrl, String assetID) {
                    this.registryUrl = registryUrl;
                    this.assetID = assetID;
                    return this;
                }

                @Override
                public void run() {
                    logger.warn("Shutdown Hook is started!");
                    logger.info("Configuring registry proxy to delete aas, with url: {}", this.registryUrl);
                    AASRegistryProxy proxy = new AASRegistryProxy(this.registryUrl);
                    proxy.delete(new Identifier(IdentifierType.CUSTOM, this.assetID));
                    logger.info("Deleted aas with id: {}", this.assetID);

                    startedComponents.forEach((component) -> {
                        logger.info("Stopping BaSyx IComponent: {}", component.toString());
                        component.stopComponent();
                    });
                    logger.info("Shutdown procedure completed!");
                    logger.warn("Shutdown Hook completed!");
                }
            }.init(this.registryUrl.toString(), this.assetID)
        );
    }

}

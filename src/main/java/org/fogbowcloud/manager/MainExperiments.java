package org.fogbowcloud.manager;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.BenchmarkingPlugin;
import org.fogbowcloud.manager.core.plugins.CapacityControllerPlugin;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.FederationMemberAuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.FederationMemberPickerPlugin;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.ImageStoragePlugin;
import org.fogbowcloud.manager.core.plugins.MapperPlugin;
import org.fogbowcloud.manager.core.plugins.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.PrioritizationPlugin;
import org.fogbowcloud.manager.core.plugins.StoragePlugin;
import org.fogbowcloud.manager.core.plugins.accounting.FCUAccountingPlugin;
import org.fogbowcloud.manager.core.plugins.accounting.SimpleStorageAccountingPlugin;
import org.fogbowcloud.manager.core.plugins.benchmarking.VanillaBenchmarkingPlugin;
import org.fogbowcloud.manager.core.plugins.capacitycontroller.satisfactiondriven.SatisfactionDrivenCapacityControllerPlugin;
import org.fogbowcloud.manager.core.plugins.imagestorage.http.HTTPDownloadImageStoragePlugin;
import org.fogbowcloud.manager.core.plugins.localcredentials.SingleMapperPlugin;
import org.fogbowcloud.manager.core.plugins.memberauthorization.DefaultMemberAuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.memberpicker.RoundRobinMemberPickerPlugin;
import org.fogbowcloud.manager.core.plugins.prioritization.TwoFoldPrioritizationPlugin;
import org.fogbowcloud.manager.occi.OCCIApplication;
import org.fogbowcloud.manager.xmpp.ManagerXmppComponent;
import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.engine.Engine;
import org.restlet.ext.slf4j.Slf4jLoggerFacade;
import org.restlet.util.Series;
import org.xmpp.component.ComponentException;

public class MainExperiments {

	private static final Logger LOGGER = Logger.getLogger(MainExperiments.class);

	public static void main(String[] args) throws Exception {
		MainHelper.configureLog4j();

		

		String managerConfgFilePath = args[0];
		String infrastructureConfgFilePath = args[1];
		String federationConfgFilePath = args[2];
		
		int numberOfCooperativeMembers = Integer.parseInt(args[3]);
		int numberOfFreeRiderMembers = Integer.parseInt(args[3]);
		
		File managerConfigFile = new File(managerConfgFilePath);
		File infrastructureConfgFile = new File(infrastructureConfgFilePath);
		File federationConfgFile = new File(federationConfgFilePath);
		
		if(!managerConfigFile.exists()){
			LOGGER.warn("Informed path to manager.conf file must be valid.");
			System.exit(MainHelper.EXIT_ERROR_CODE);
		}
		if(!infrastructureConfgFile.exists()){
			LOGGER.warn("Informed path to infrastructure.conf file must be valid.");
			System.exit(MainHelper.EXIT_ERROR_CODE);
		}
		if(!federationConfgFile.exists()){
			LOGGER.warn("Informed path to federation.conf file must be valid.");
			System.exit(MainHelper.EXIT_ERROR_CODE);
		}
		
		Properties properties = new Properties();
		
		Properties managerProperties = new Properties();
		FileInputStream input = new FileInputStream(managerConfigFile);
		managerProperties.load(input);
		Properties infraProperties = new Properties();
		FileInputStream infraInput = new FileInputStream(infrastructureConfgFile);
		infraProperties.load(infraInput);
		Properties fedProperties = new Properties();
		FileInputStream fedInput = new FileInputStream(federationConfgFile);
		fedProperties.load(fedInput);
		
		properties.putAll(managerProperties);
		properties.putAll(infraProperties);
		properties.putAll(fedProperties);
		
		List<Properties> propertiesList = new ArrayList<Properties>();
		
		boolean freeRider = false;
		for(int i = 1; i <= numberOfCooperativeMembers; i++)
			propertiesList.add(SimpleManagerFactory.adjustPropertiesManager(i, freeRider, properties));
		
		freeRider = true;
		for(int i = 1; i <= numberOfFreeRiderMembers; i++)
			propertiesList.add(SimpleManagerFactory.adjustPropertiesManager(i, freeRider, properties));
		
		
		//folder name default to datastores
		//DataStoreHelper.setDataStoreFolderExecution(DataStoreHelper.DATASTORES_FOLDER);
		
		List<ManagerController> fms = new ArrayList<ManagerController>();
		for(Properties prop : propertiesList)
			fms.add(SimpleManagerFactory.createFM(prop));
		
		
		String xmppHost = properties.getProperty(ConfigurationConstants.XMPP_HOST_KEY);
		String xmppJid = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
		
		if (xmppHost != null && xmppJid != null) {
			long timeout = MainHelper.getXMPPTimeout(properties);
			ManagerXmppComponent xmpp = new ManagerXmppComponent(
					xmppJid,
					properties.getProperty(ConfigurationConstants.XMPP_PASS_KEY),
					xmppHost,
					Integer.parseInt(properties.getProperty(ConfigurationConstants.XMPP_PORT_KEY)),
					facade, 
					timeout);
			xmpp.setRendezvousAddress(properties.getProperty(ConfigurationConstants.RENDEZVOUS_JID_KEY));
			try {
				xmpp.connect();			
			} catch (ComponentException e) {
				LOGGER.error("Conflict in the initialization of xmpp component.", e);
				System.exit(MainHelper.EXIT_ERROR_CODE);
			}
			xmpp.process(false);
			xmpp.init();
			facade.setPacketSender(xmpp);
		}

		OCCIApplication application = new OCCIApplication(facade);

		Slf4jLoggerFacade loggerFacade = new Slf4jLoggerFacade();
		Engine.getInstance().setLoggerFacade(loggerFacade);
		
		try {
			Component http = new Component();
			String httpPort = properties.getProperty(ConfigurationConstants.HTTP_PORT_KEY, 
					String.valueOf(MainHelper.DEFAULT_HTTP_PORT));
			String httpsPort = properties.getProperty(ConfigurationConstants.HTTPS_PORT_KEY, 
					String.valueOf(MainHelper.DEFAULT_HTTPS_PORT));
			String httpsKeystorePath = properties.getProperty(ConfigurationConstants.HTTPS_KEYSTORE_PATH);
			String httpsKeystorePassword = properties.getProperty(ConfigurationConstants.HTTPS_KEYSTORE_PASSWORD);
			String httpsKeyPassword = properties.getProperty(ConfigurationConstants.HTTPS_KEY_PASSWORD);
			String httpsKeystoreType = properties.getProperty(ConfigurationConstants.HTTPS_KEYSTORE_TYPE, "JKS");
			Boolean httpsEnabled = Boolean.valueOf(properties.getProperty(ConfigurationConstants.HTTPS_ENABLED, 
					String.valueOf(MainHelper.DEFAULT_HTTPS_ENABLED)));
			String requestHeaderSize = String.valueOf(Integer.parseInt(
					properties.getProperty(ConfigurationConstants.HTTP_REQUEST_HEADER_SIZE_KEY, 
					String.valueOf(MainHelper.DEFAULT_REQUEST_HEADER_SIZE))));
			String responseHeaderSize = String.valueOf(Integer.parseInt(properties.getProperty(
					ConfigurationConstants.HTTP_RESPONSE_HEADER_SIZE_KEY, 
					String.valueOf(MainHelper.DEFAULT_RESPONSE_HEADER_SIZE))));
			
			//Adding HTTP server
			Server httpServer = http.getServers().add(Protocol.HTTP, Integer.parseInt(httpPort));
			Series<Parameter> httpParameters = httpServer.getContext().getParameters();
			httpParameters.add("http.requestHeaderSize", requestHeaderSize);
			httpParameters.add("http.responseHeaderSize", responseHeaderSize);
			
			if (httpsEnabled) {
				//Adding HTTPS server
				Server httpsServer = http.getServers().add(Protocol.HTTPS, Integer.parseInt(httpsPort));
				
				@SuppressWarnings("rawtypes")
				Series parameters = httpsServer.getContext().getParameters();
				parameters.add("sslContextFactory", "org.restlet.engine.ssl.DefaultSslContextFactory");
				if (httpsKeystorePath != null) {
					parameters.add("keyStorePath", httpsKeystorePath);
				}
				if (httpsKeystorePassword != null) {
					parameters.add("keyStorePassword", httpsKeystorePassword);
				}
				if (httpsKeyPassword != null) {
					parameters.add("keyPassword", httpsKeyPassword);
				}
				if (httpsKeystoreType != null) {
					parameters.add("keyStoreType", httpsKeystoreType);
				}
				parameters.add("http.requestHeaderSize", requestHeaderSize);
				parameters.add("http.responseHeaderSize", responseHeaderSize);
			}
			
			http.getDefaultHost().attach(application);
			http.start();
		} catch (Exception e) {
			LOGGER.error("Conflict in the initialization of the HTTP component.", e);
			System.exit(MainHelper.EXIT_ERROR_CODE);
		}
	}
	
}

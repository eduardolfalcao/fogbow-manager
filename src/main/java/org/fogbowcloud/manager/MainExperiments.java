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

		

		String managerConfigFilePath = args[0];
		String infrastructureConfigFilePath = args[1];
		String federationConfigFilePath = args[2];
		
		int numberOfCooperativeMembers = Integer.parseInt(args[3]);
		int numberOfFreeRiderMembers = Integer.parseInt(args[4]);
		
		File managerConfigFile = new File(managerConfigFilePath);
		File infrastructureConfgFile = new File(infrastructureConfigFilePath);
		File federationConfgFile = new File(federationConfigFilePath);
		
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
		int id = 1;
		for(; id <= numberOfCooperativeMembers; id++)
			propertiesList.add(SimpleManagerFactory.adjustPropertiesManager(id, freeRider, properties));
		
		freeRider = true;
		for(; id <= numberOfCooperativeMembers+numberOfFreeRiderMembers; id++)
			propertiesList.add(SimpleManagerFactory.adjustPropertiesManager(id, freeRider, properties));
		
		List<ManagerController> fms = new ArrayList<ManagerController>();
		for(Properties prop : propertiesList)
			fms.add(SimpleManagerFactory.createFM(prop));
		
	}
	
}

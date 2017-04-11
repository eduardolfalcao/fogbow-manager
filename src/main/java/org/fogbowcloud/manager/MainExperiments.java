package org.fogbowcloud.manager;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ManagerController;

public class MainExperiments {

	private static final Logger LOGGER = Logger.getLogger(MainExperiments.class);

	public static void main(String[] args) throws Exception {
		
		//TODO
		//remover banco de dados
		
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

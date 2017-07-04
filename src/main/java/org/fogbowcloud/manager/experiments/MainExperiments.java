package org.fogbowcloud.manager.experiments;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.MainHelper;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.ManagerControllerHelper;
import org.fogbowcloud.manager.core.ManagerTimer;
import org.fogbowcloud.manager.experiments.data.MonitorPeerState;
import org.fogbowcloud.manager.experiments.scheduler.WorkloadScheduler;

public class MainExperiments {

	private static final Logger LOGGER = Logger.getLogger(MainExperiments.class);
	private static final ManagerTimer dataMonitoringTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static final ManagerTimer schedulerTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));

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
		
		if(JOptionPane.OK_OPTION==JOptionPane.showConfirmDialog(null, "Would you like to remove data from previous experiment?")){
			FileUtils.cleanDirectory(new File(SimpleManagerFactory.PATH_DATASTORES));
			FileUtils.cleanDirectory(new File(managerProperties.getProperty(MonitorPeerState.OUTPUT_FOLDER)));
		}
		
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
		
		WorkloadScheduler scheduler = new WorkloadScheduler(fms, properties);
		triggerWorkloadScheduler(scheduler, properties);
		
		//bootstrapping
		Thread.sleep(ManagerControllerHelper.getBootstrappingPeriod(managerProperties));
		MonitorPeerState monitorPeerState = new MonitorPeerState(fms);
		triggerDataMonitoring(monitorPeerState, properties);
		
		LOGGER.info("The federation is up!");
		
	}
	
	private static void triggerWorkloadScheduler(final WorkloadScheduler scheduler, final Properties prop) {
		final long schedulerPeriod = 1000;
		schedulerTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {	
				try {
					scheduler.checkAndSubmitTasks();
				} catch (Throwable e) {
					LOGGER.error("Error while scheduling workload", e);
				}
			}
		}, ManagerControllerHelper.getBootstrappingPeriod(prop), schedulerPeriod);
	}
	
	private static void triggerDataMonitoring(final MonitorPeerState monitorPeers, Properties prop) {
		final long dataMonitoringPeriod = ManagerControllerHelper.getPeerStateMonitoringPeriod(prop);

		dataMonitoringTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {	
				try {
					monitorPeers.savePeerState();
				} catch (Throwable e) {
					LOGGER.error("Error while monitoring peer states", e);
				}
			}
		}, 0, dataMonitoringPeriod);
	}
	
}

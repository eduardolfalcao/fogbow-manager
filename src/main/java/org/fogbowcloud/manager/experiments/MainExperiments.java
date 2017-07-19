package org.fogbowcloud.manager.experiments;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.MainHelper;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.ManagerControllerHelper;
import org.fogbowcloud.manager.core.ManagerTimer;
import org.fogbowcloud.manager.core.plugins.compute.fake.FakeCloudComputePlugin;
import org.fogbowcloud.manager.experiments.monitor.MonitorPeerStateSingleton;
import org.fogbowcloud.manager.experiments.monitor.MonitorPeerStateSingleton.MonitorPeerStateAssync;
import org.fogbowcloud.manager.experiments.scheduler.WorkloadScheduler;

public class MainExperiments {

	private static final Logger LOGGER = Logger.getLogger(MainExperiments.class);
	private static final ManagerTimer schedulerTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	
	private static final String LOG4J_CONF = "log4j";

	public static void main(String[] args) throws Exception {		

		LOGGER.setLevel(Level.INFO);
		String managerConfigFilePath = args[0];
		String infrastructureConfigFilePath = args[1];
		String federationConfigFilePath = args[2];
		
		File managerConfigFile = new File(managerConfigFilePath);
		File infrastructureConfgFile = new File(infrastructureConfigFilePath);
		File federationConfgFile = new File(federationConfigFilePath);
		
		if(!managerConfigFile.exists()){
			LOGGER.info("Informed path to manager.conf file must be valid.");
			System.exit(MainHelper.EXIT_ERROR_CODE);
		}
		if(!infrastructureConfgFile.exists()){
			LOGGER.info("Informed path to infrastructure.conf file must be valid.");
			System.exit(MainHelper.EXIT_ERROR_CODE);
		}
		if(!federationConfgFile.exists()){
			LOGGER.info("Informed path to federation.conf file must be valid.");
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
		
		int numberOfPeers = Integer.parseInt(args[3]);
		properties.put(FakeCloudComputePlugin.COMPUTE_FAKE_QUOTA, args[4]);	//compute_fake_quota
		boolean fdnof = args[5].equals("fdnof")?true:false;					//fdnof or sdnof
		properties.put(WorkloadScheduler.WORKLOAD_FOLDER, args[6]);			//workload_folder
		properties.put(MonitorPeerStateSingleton.OUTPUT_DATA_ENDING_TIME, args[7]);	//output_data_ending_time
		
		String outputfolder = "data/"+args[5]+"-"+numberOfPeers+"peers-"+args[4]+"capacity/";
//		String outputfolder = System.getProperty("user.dir")+"/experiments/data/"+args[5]+"-"+numberOfPeers+"peers-"+args[4]+"capacity/";	//execution on eclipse
		
		properties.put(MonitorPeerStateSingleton.OUTPUT_FOLDER, outputfolder);		
		
		MainHelper.configureLog4j(properties.getProperty(LOG4J_CONF));
		try{
			FileUtils.cleanDirectory(new File(SimpleManagerFactory.PATH_DATASTORES));
		}catch(Exception e){
			LOGGER.warn(e.getMessage());
		}
		try{
			FileUtils.cleanDirectory(new File(properties.getProperty(MonitorPeerStateSingleton.OUTPUT_FOLDER)));
		}catch(Exception e){
			LOGGER.warn(e.getMessage());
		}
		
		List<Properties> propertiesList = new ArrayList<Properties>();
		
		int id = 1;
		for(; id <= numberOfPeers; id++)
			propertiesList.add(SimpleManagerFactory.adjustPropertiesManager(id, fdnof, properties));
		
		List<ManagerController> fms = new ArrayList<ManagerController>();
		for(Properties prop : propertiesList)
			fms.add(SimpleManagerFactory.createFM(prop));			
		
		WorkloadScheduler scheduler = new WorkloadScheduler(fms, properties);
		triggerWorkloadScheduler(scheduler, properties);		
		
		Thread.sleep(ManagerControllerHelper.getBootstrappingPeriod(managerProperties));
		MonitorPeerStateSingleton.getInstance().init(fms); 		//starting peer state monitoring
		for(ManagerController fm : fms)
			fm.triggerWorkloadMonitor(MonitorPeerStateSingleton.getInstance().getMonitors().get(fm.getManagerId()));
		
		LOGGER.info("The federation is up!");
		
		Thread.sleep(Long.parseLong(properties.getProperty(MonitorPeerStateAssync.OUTPUT_DATA_ENDING_TIME))+1000);
		
		LOGGER.info("Ending experiment!");
		System.exit(0);		
	}
	
	private static void triggerWorkloadScheduler(final WorkloadScheduler scheduler, final Properties prop) {
		final long workloadSchedulerPeriod = ManagerControllerHelper.getWorkloadSchedulerPeriod(prop);
		schedulerTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {	
				try {
					scheduler.checkAndSubmitTasks();
				} catch (Throwable e) {
					LOGGER.error("Error while scheduling workload", e);
				}
			}
		}, ManagerControllerHelper.getBootstrappingPeriod(prop), workloadSchedulerPeriod);
	}
	
}

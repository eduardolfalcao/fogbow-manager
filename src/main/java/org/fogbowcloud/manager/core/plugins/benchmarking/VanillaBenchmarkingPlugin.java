package org.fogbowcloud.manager.core.plugins.benchmarking;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.MainHelper;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.plugins.BenchmarkingPlugin;
import org.fogbowcloud.manager.core.plugins.accounting.FCUAccountingPlugin;
import org.fogbowcloud.manager.occi.instance.Instance;

public class VanillaBenchmarkingPlugin implements BenchmarkingPlugin {
	
	private Map<String, Double> instanceToPower = new HashMap<String, Double>();
	
	private static final Logger LOGGER = Logger.getLogger(VanillaBenchmarkingPlugin.class);
	private String managerId;
	
	public VanillaBenchmarkingPlugin(Properties properties) {
		managerId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
	}

	@Override
	public void run(String globalInstanceId, Instance instance) {
		if (instance == null) {
			throw new IllegalArgumentException("<"+managerId+">: "+"Instance must not be null. (globalInstanceId: "+globalInstanceId+")");
		}
		LOGGER.info("<"+managerId+">: "+"Running benchmarking on instance: " + globalInstanceId);

		double power = UNDEFINED_POWER;
		try {
			String vcpuStr = instance.getAttributes().get("occi.compute.cores");
			String memStr = instance.getAttributes().get("occi.compute.memory");

			LOGGER.debug("<"+managerId+">: "+"Instance " + globalInstanceId + " has " + vcpuStr + " vCPU and " + memStr
					+ " GB of RAM.");
			
			double vcpu = parseDouble(vcpuStr);
			double mem = parseDouble(memStr);
			
			power = ((vcpu / 8d) + (mem / 16d)) / 2;
		} catch (Exception e) {
			LOGGER.error("<"+managerId+">: "+"Error while parsing attribute values to double.", e);
		}
		
		LOGGER.debug("<"+managerId+">: "+"Putting instanceId " + globalInstanceId + " and power " + power);

		synchronized(instanceToPower){
			instanceToPower.put(globalInstanceId, power);
		}
	}

	private double parseDouble(String str) {
		return Double.parseDouble(str.replaceAll("\"", ""));
	}

	@Override
	public double getPower(String globalInstanceId) {
		//LOGGER.debug("<"+managerId+">: "+"Getting power of instance " + globalInstanceId);
		//LOGGER.debug("<"+managerId+">: "+"Current instanceToPower=" + instanceToPower);
		synchronized(instanceToPower){
			if (instanceToPower.get(globalInstanceId) == null) {		
				return UNDEFINED_POWER;
			}
			return instanceToPower.get(globalInstanceId);
		}
	}

	@Override
	public void remove(String globalInstanceId) {
		LOGGER.debug("<"+managerId+">: "+"Removing instance: " + globalInstanceId + " from benchmarking map.");
		synchronized(instanceToPower){
			instanceToPower.remove(globalInstanceId);
		}		
	}
}

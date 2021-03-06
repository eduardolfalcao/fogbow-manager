package org.fogbowcloud.manager.core.plugins.capacitycontroller.fairnessdriven;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.MainHelper;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.model.DateUtils;
import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.CapacityControllerPlugin;
import org.fogbowcloud.manager.core.plugins.accounting.FCUAccountingPlugin;

public abstract class FairnessDrivenCapacityController implements CapacityControllerPlugin {
	
	private static final Logger LOGGER = Logger.getLogger(FairnessDrivenCapacityController.class);
	protected static final String CONTROLLER_MINIMUM_THRESHOLD = "controller_minimum_threshold";
	protected static final String CONTROLLER_MAXIMUM_THRESHOLD = "controller_maximum_threshold";
	private double INITIALIZE_LAST_MAXIMUM_CAPACITY_VALUE = Double.MAX_VALUE;
	protected static final String CONTROLLER_DELTA = "controller_delta";

	private double lastMaximumCapacity = INITIALIZE_LAST_MAXIMUM_CAPACITY_VALUE;
	protected AccountingPlugin accountingPlugin;
	protected Properties properties;
	protected DateUtils dateUtils; 
	
	private String managerId;
	
	public FairnessDrivenCapacityController(Properties properties, AccountingPlugin accountingPlugin) {
		this.properties = properties;
		this.accountingPlugin = accountingPlugin;
		this.dateUtils = new DateUtils();
		this.managerId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
	}
	
	public abstract double getCurrentFairness(FederationMember member);
	public abstract double getLastFairness(FederationMember member);
	
	public static double getFairness(double consumed, double donated){
		if(donated < 0 || consumed < 0) {
			throw new IllegalArgumentException("Donated and consumed can not be negative. "
					+ " Donated(" + donated + ") and Consumed(" + consumed + ")");			
		} else if(donated == 0) {
			return -1;		
		} else {
			return consumed/donated;
		}
	}
	
	public void setDateUtils(DateUtils dateUtils) {
		this.dateUtils = dateUtils;
	}
	
	protected double normalizeMaximumCapacity(double maximumCapacity) {
		if (maximumCapacity == CapacityControllerPlugin.MAXIMUM_CAPACITY_VALUE_ERROR) {
			maximumCapacity = this.lastMaximumCapacity;
		} else {
			this.lastMaximumCapacity = maximumCapacity;
		}
		LOGGER.debug("<"+managerId+">: "+"The maximum capacity is " + maximumCapacity 
				+ " and the last maximum capacity is " + this.lastMaximumCapacity);
		
		return maximumCapacity;
	}	
}

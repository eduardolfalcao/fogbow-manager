package org.fogbowcloud.manager.core.plugins.capacitycontroller.fairnessdriven;

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.accounting.AccountingInfo;
import org.fogbowcloud.manager.core.plugins.compute.fake.FakeCloudComputePlugin;

//TODO review all class
public class GlobalFairnessDrivenController extends FairnessDrivenCapacityController{
	
	private HillClimbingAlgorithm hillClimbingController;
	private String managerId;
	
	private final static Logger LOGGER = Logger.getLogger(GlobalFairnessDrivenController.class.getName());
	
	public GlobalFairnessDrivenController(Properties properties, AccountingPlugin accountingPlugin) {
		super(properties, accountingPlugin);
		
		double deltaC, minimumThreshold, maximumThreshold;
		deltaC = Double.parseDouble(properties.getProperty(CONTROLLER_DELTA));
		minimumThreshold = Double.parseDouble(properties.getProperty(CONTROLLER_MINIMUM_THRESHOLD));
		maximumThreshold = Double.parseDouble(properties.getProperty(CONTROLLER_MAXIMUM_THRESHOLD));
		this.hillClimbingController = new HillClimbingAlgorithm(deltaC, minimumThreshold, maximumThreshold);
		
		this.managerId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
	}

	public double getMaxCapacityToSupply(FederationMember member) {
		return this.hillClimbingController.getMaximumCapacityToSupply();	
	}
	
	public void updateCapacity(FederationMember member, double maximumCapacity) {
		LOGGER.info("<"+managerId+">: Running GlobalFairnessDrivenController ");		
		maximumCapacity = normalizeMaximumCapacity(maximumCapacity);
		updateFairness();
		this.hillClimbingController.updateCapacity(maximumCapacity);
	}
	
	protected void updateFairness() {
		this.hillClimbingController.setLastFairness(
				this.hillClimbingController.getCurrentFairness());
		double currentConsumed = 0;
		double currentDonated = 0;
		List<AccountingInfo> accountingList = accountingPlugin.getAccountingInfo();
		for(AccountingInfo accountingInfo : accountingList){
			if (accountingInfo.getProvidingMember().equals(properties			//if this fm is providing and the consumer is not the same fm, then, this fm is providing
					.getProperty(ConfigurationConstants.XMPP_JID_KEY)) &&
					!accountingInfo.getRequestingMember().equals(properties
							.getProperty(ConfigurationConstants.XMPP_JID_KEY))) {						
				currentDonated += accountingInfo.getUsage();
			} else if(!accountingInfo.getProvidingMember().equals(properties	//if it is another fm providing, then, this fm is consuming
					.getProperty(ConfigurationConstants.XMPP_JID_KEY))){
				currentConsumed += accountingInfo.getUsage();
			}
		}
		this.hillClimbingController.setCurrentFairness(
				getFairness(currentConsumed, currentDonated));
	}

	@Override
	public double getCurrentFairness(FederationMember member) {
		return hillClimbingController.getCurrentFairness();
	}

	@Override
	public double getLastFairness(FederationMember member) {
		return hillClimbingController.getLastFairness();
	}

}
package org.fogbowcloud.manager.core.plugins.capacitycontroller.fairnessdriven;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.CapacityControllerPlugin;
import org.fogbowcloud.manager.xmpp.ManagerPacketHelper;

public class TwoFoldCapacityController implements CapacityControllerPlugin {

	private FairnessDrivenCapacityController pairwiseController;
	private FairnessDrivenCapacityController globalController;
	private String managerId;
	
	private final static Logger LOGGER = Logger.getLogger(TwoFoldCapacityController.class.getName());
		
	protected TwoFoldCapacityController() {}
	
	public TwoFoldCapacityController(Properties properties, AccountingPlugin accountingPlugin) {
		this.pairwiseController = new PairwiseFairnessDrivenController(properties, accountingPlugin);
		this.globalController = new GlobalFairnessDrivenController(properties, accountingPlugin);
		this.managerId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
	}
	
	@Override
	public double getMaxCapacityToSupply(FederationMember member) {
		double maxCapacity;
		if (this.pairwiseController.getCurrentFairness(member) >= 0) {
			maxCapacity = this.pairwiseController.getMaxCapacityToSupply(member);
		} else {
			maxCapacity = this.globalController.getMaxCapacityToSupply(member);
		}
		LOGGER.info("<"+managerId+">: The max capacity for "+member.getId()+" is "+maxCapacity);
		return maxCapacity;
	}
	
	@Override
	public void updateCapacity(FederationMember member, double maximumCapacity) {
		if(member == null){
			LOGGER.info("<"+managerId+">: Running TwoFoldCapacityController (FDNOF - GlobalFairnessDrivenController)");
			this.globalController.updateCapacity(member, maximumCapacity);	
		}else{
			LOGGER.info("<"+managerId+">: Running TwoFoldCapacityController (FDNOF - PairwiseFairnessDrivenController) for member: "+member.getId());
			this.pairwiseController.updateCapacity(member, maximumCapacity);
		}		
		
	}
	
	protected void setGlobalController(
			FairnessDrivenCapacityController globalController) {
		this.globalController = globalController;
	}
	
	protected void setPairwiseController(
			FairnessDrivenCapacityController pairwiseController) {
		this.pairwiseController = pairwiseController;
	}

}

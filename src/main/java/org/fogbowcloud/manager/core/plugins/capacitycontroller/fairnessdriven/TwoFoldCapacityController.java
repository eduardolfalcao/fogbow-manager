package org.fogbowcloud.manager.core.plugins.capacitycontroller.fairnessdriven;

import java.util.Properties;

import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.CapacityControllerPlugin;

public class TwoFoldCapacityController implements CapacityControllerPlugin {

	private FairnessDrivenCapacityController pairwiseController;
	private FairnessDrivenCapacityController globalController;
	private AccountingPlugin accountingPlugin;
		
	protected TwoFoldCapacityController() {}
	
	public TwoFoldCapacityController(Properties properties, AccountingPlugin accountingPlugin) {
		this.accountingPlugin = accountingPlugin;
		this.pairwiseController = new PairwiseFairnessDrivenController(properties, accountingPlugin);
		this.globalController = new GlobalFairnessDrivenController(properties, accountingPlugin);
	}
	
	@Override
	public double getMaxCapacityToSupply(FederationMember member) {
		if (this.pairwiseController.getCurrentFairness(member) >= 0) {
			return this.pairwiseController.getMaxCapacityToSupply(member);
		} else {
			return this.globalController.getMaxCapacityToSupply(member);
		}
	}
	
	@Override
	public void updateCapacity(FederationMember member, double maximumCapacity) {
		this.pairwiseController.updateCapacity(member, maximumCapacity);		
		this.globalController.updateCapacity(member, maximumCapacity);
		this.accountingPlugin.update(member,getMaxCapacityToSupply(member));
	}
	
	protected void setGlobalController(
			FairnessDrivenCapacityController globalController) {
		this.globalController = globalController;
	}
	
	protected void setPairwiseController(
			FairnessDrivenCapacityController pairwiseController) {
		this.pairwiseController = pairwiseController;
	}
	
//	public void setDateUtils(DateUtils dateUtils){
//		this.pairwiseController.setDateUtils(dateUtils);
//		this.globalController.setDateUtils(dateUtils);
//	}

}

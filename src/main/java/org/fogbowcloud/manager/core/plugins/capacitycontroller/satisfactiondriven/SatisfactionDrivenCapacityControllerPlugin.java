package org.fogbowcloud.manager.core.plugins.capacitycontroller.satisfactiondriven;

import java.util.Properties;

import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.CapacityControllerPlugin;

public class SatisfactionDrivenCapacityControllerPlugin implements CapacityControllerPlugin {
	
	public SatisfactionDrivenCapacityControllerPlugin() {
		// TODO Auto-generated constructor stub
	}
	
	public SatisfactionDrivenCapacityControllerPlugin(Properties prop, AccountingPlugin ap) {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public double getMaxCapacityToSupply(FederationMember member) {
		return Double.MAX_VALUE;
	}
	
	@Override
	public void updateCapacity(FederationMember member, double maximumCapacity) {}

}

package org.fogbowcloud.manager.core.plugins.capacitycontroller.freerider;

import java.util.Properties;

import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.CapacityControllerPlugin;

public class FreeRiderCapacityControllerPlugin implements CapacityControllerPlugin{

	public FreeRiderCapacityControllerPlugin() {
		// TODO Auto-generated constructor stub
	}
	
	public FreeRiderCapacityControllerPlugin(Properties prop, AccountingPlugin ap) {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public double getMaxCapacityToSupply(FederationMember member) {
		return -1;
	}

	@Override
	public void updateCapacity(FederationMember member, double maximumCapacity) {}
	

}

package org.fogbowcloud.manager.core.plugins.capacitycontroller.freerider;

import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.CapacityControllerPlugin;

public class FreeRiderCapacityControllerPlugin implements CapacityControllerPlugin{

	@Override
	public double getMaxCapacityToSupply(FederationMember member) {
		return -1;
	}

	@Override
	public void updateCapacity(FederationMember member, double maximumCapacity) {}
	

}

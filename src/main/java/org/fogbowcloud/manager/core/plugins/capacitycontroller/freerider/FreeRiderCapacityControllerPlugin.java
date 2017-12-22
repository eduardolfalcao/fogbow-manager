package org.fogbowcloud.manager.core.plugins.capacitycontroller.freerider;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.CapacityControllerPlugin;
import org.fogbowcloud.manager.core.plugins.capacitycontroller.fairnessdriven.TwoFoldCapacityController;

public class FreeRiderCapacityControllerPlugin implements CapacityControllerPlugin{

	private final static Logger LOGGER = Logger.getLogger(FreeRiderCapacityControllerPlugin.class.getName());
	
	private String managerId;
	
	public FreeRiderCapacityControllerPlugin() {
		// TODO Auto-generated constructor stub
	}
	
	public FreeRiderCapacityControllerPlugin(Properties properties, AccountingPlugin ap) {
		this.managerId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
	}
	
	@Override
	public double getMaxCapacityToSupply(FederationMember member) {
		return -1;
	}

	@Override
	public void updateCapacity(FederationMember member, double maximumCapacity) {
		String memberChoosen = "null";
		if(member!=null)
			memberChoosen = member.getId();
		LOGGER.info("<"+managerId+">: Running FreeRiderCapacityControllerPlugin for member: "+memberChoosen);
	}
	

}

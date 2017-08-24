package org.fogbowcloud.manager.core.plugins;

import org.fogbowcloud.manager.core.model.FederationMember;

public interface AccountingPluginXP extends AccountingPlugin{

	public void update(FederationMember member, double capacity);
	
}

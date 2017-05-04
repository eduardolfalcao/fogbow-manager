package org.fogbowcloud.manager.core.plugins;

import java.util.List;

import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.accounting.AccountingInfo;
import org.fogbowcloud.manager.occi.order.Order;

public interface AccountingPlugin {
	
	public void update(List<Order> ordersWithInstance);
	
	public void update(FederationMember member, double capacity);

	public List<AccountingInfo> getAccountingInfo();
	
	public AccountingInfo getAccountingInfo(String user, String requestingMember, String providingMember);

}

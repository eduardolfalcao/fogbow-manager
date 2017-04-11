package org.fogbowcloud.manager.core.plugins.prioritization;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.MainHelper;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.PrioritizationPlugin;
import org.fogbowcloud.manager.occi.order.Order;

public class PriotizeRemoteOrderPlugin implements PrioritizationPlugin {

	private static Logger LOGGER;
	
	public PriotizeRemoteOrderPlugin(Properties properties, AccountingPlugin accountingPlugin){
		LOGGER = MainHelper.getLogger(PriotizeRemoteOrderPlugin.class.getName(), properties.getProperty(ConfigurationConstants.XMPP_JID_KEY));
	}
	
	@Override
	public Order takeFrom(Order newOrder, List<Order> ordersWithInstance) {
		LOGGER.debug("Choosing order to take instance from ordersWithInstance="
				+ ordersWithInstance + " for requestMember=" + newOrder.getRequestingMemberId());
		
		if (newOrder.isLocal()) {
			return null;
		}
		
		List<Order> federationUserOrders = filterOrdersFulfilledByFedUser(ordersWithInstance);
		LOGGER.debug("federationUserOrders=" + federationUserOrders);
		return getMostRecentOrder(federationUserOrders);
	}
	
	private List<Order> filterOrdersFulfilledByFedUser(List<Order> ordersWithInstance) {
		List<Order> federationUserOrders = new ArrayList<Order>();
		for (Order currentOrder : ordersWithInstance) {
			federationUserOrders.add(currentOrder);
		}
		return federationUserOrders;
	}

	private Order getMostRecentOrder(List<Order> orders) {
		if (orders.isEmpty()) {
			return null;
		}
		Order mostRecentOrder = orders.get(0);
		for (Order currentOrder : orders) {
			if (new Date(mostRecentOrder.getFulfilledTime()).compareTo(new Date(currentOrder
					.getFulfilledTime())) < 0) {
				mostRecentOrder = currentOrder;
			}
		}
		return mostRecentOrder;
	}
}

package org.fogbowcloud.manager.core.plugins.prioritization.nof;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.MainHelper;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.PrioritizationPlugin;
import org.fogbowcloud.manager.core.plugins.accounting.AccountingInfo;
import org.fogbowcloud.manager.core.plugins.accounting.FCUAccountingPlugin;
import org.fogbowcloud.manager.core.plugins.accounting.ResourceUsage;
import org.fogbowcloud.manager.core.plugins.prioritization.TwoFoldPrioritizationPlugin;
import org.fogbowcloud.manager.occi.order.Order;

public class NoFPrioritizationPlugin implements PrioritizationPlugin {

	private AccountingPlugin accountingPlugin;
	private String localMemberId;
	private boolean trustworthy = false;
	private boolean prioritizeLocal = true;
	
	private static final Logger LOGGER = Logger.getLogger(NoFPrioritizationPlugin.class);

	private String managerId;
	
	public NoFPrioritizationPlugin(Properties properties, AccountingPlugin accountingPlugin) {
		
		this.managerId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
		
		this.accountingPlugin = accountingPlugin;
		this.localMemberId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
		try {
			this.trustworthy = Boolean.valueOf(properties.getProperty("nof_trustworthy").trim());
		} catch (Exception e) {
			LOGGER.error("<"+managerId+">: "+
					"Error while getting boolean value for nof_trustworhty. The default value is false.",
					e);
		}

		try {
			this.prioritizeLocal = Boolean.valueOf(properties.getProperty("nof_prioritize_local")
					.trim());
		} catch (Exception e) {
			LOGGER.error("<"+managerId+">: "+
					"Error while getting boolean value for nof_prioritize_local. The default value is true.",
					e);
		}		
	}
	
	@Override
	public Order takeFrom(Order newOrder, List<Order> ordersWithInstance) {
		LOGGER.debug("<"+managerId+">: "+"Choosing order to take instance from ordersWithInstance="
				+ ordersWithInstance + " for requestMember=" + newOrder.getRequestingMemberId());
		if (ordersWithInstance == null) {			
			return null;
		}
		
		List<String> servedMemberIds = getServedMemberIds(ordersWithInstance);
		LOGGER.debug("<"+managerId+">: "+"Current servedMemberIds=" + servedMemberIds);
		
		List<AccountingInfo> accounting = accountingPlugin.getAccountingInfo();
		Map<String, ResourceUsage> membersUsage = NoFHelper.calculateMembersUsage(localMemberId, accounting);
		LOGGER.debug("<"+managerId+">: "+"Current membersUsage=" + membersUsage);		
		LinkedList<FederationMemberCredit> memberCredits = calctMemberCredits(servedMemberIds, membersUsage);
		if (memberCredits.isEmpty()) {
			LOGGER.debug("<"+managerId+">: "+"There are no member credits.");
			return null;
		}

		Collections.sort(memberCredits, new FederationMemberCreditComparator());
		LOGGER.debug("<"+managerId+">: "+"Current memberCredits=" + memberCredits);
		
		double requestingMemberCredit = calcCredit(membersUsage, newOrder.getRequestingMemberId());
		LOGGER.debug("<"+managerId+">: "+"Requesting member credit=" + requestingMemberCredit);
		
		FederationMemberCredit memberWithLowestCredit = memberCredits.getLast();
		if (memberWithLowestCredit.getCredit() < requestingMemberCredit) {
			String memberId = memberWithLowestCredit.getMember().getResourcesInfo().getId();
			List<Order> memberRequests = filterByRequestingMember(memberId, ordersWithInstance);
			return getMostRecentOrder(memberRequests);
		}
		return null;
	}

	private LinkedList<FederationMemberCredit> calctMemberCredits(List<String> servedMembers,
			Map<String, ResourceUsage> membersUsage) {
		LinkedList<FederationMemberCredit> memberCredits = new LinkedList<FederationMemberCredit>();
		for (String currentMemberId : servedMembers) {
			if (localMemberId.equals(currentMemberId)) {
				continue;
			}
			double credit = calcCredit(membersUsage, currentMemberId);
			memberCredits.add(new FederationMemberCredit(currentMemberId, credit));
		}
		return memberCredits;
	}

	private List<String> getServedMemberIds(List<Order> orders) {
		List<String> servedMemberIds = new LinkedList<String>();
		for (Order currentOrder : orders) {
			if (!servedMemberIds.contains(currentOrder.getRequestingMemberId())) {
				servedMemberIds.add(currentOrder.getRequestingMemberId());
			}
		}
		return servedMemberIds;
	}

	protected double calcCredit(Map<String, ResourceUsage> membersUsage, String memberId) {		
		double credit = 0;
		if (localMemberId.equals(memberId)) {
			if (prioritizeLocal) {
				return Double.MAX_VALUE;
			} else {
				return -1;
			}
		}

		if (membersUsage.containsKey(memberId)) {
			credit = membersUsage.get(memberId).getDonated()
					- membersUsage.get(memberId).getConsumed();
			if (!trustworthy) {
				credit = Math.max(0,
						credit + Math.sqrt(membersUsage.get(memberId).getDonated()));
			}
		}
		return credit;
	}

	private Order getMostRecentOrder(List<Order> memberorders) {
		if (memberorders.isEmpty()) {
			return null;
		}
		Order mostRecentOrder = memberorders.get(0);
		for (Order currentOrder : memberorders) {
			if (new Date(mostRecentOrder.getFulfilledTime()).compareTo(new Date(currentOrder.getFulfilledTime())) < 0) {
				mostRecentOrder = currentOrder;
			}
		}
		return mostRecentOrder;
	}

	private List<Order> filterByRequestingMember(String requestingMemberId, List<Order> orders) {
		List<Order> filteredOrders = new LinkedList<Order>();
		for (Order currentOrder : orders) {
			if (currentOrder.getRequestingMemberId().equals(requestingMemberId)){
				filteredOrders.add(currentOrder);
			}
		}
		return filteredOrders;
	}
}

package org.fogbowcloud.manager.core.plugins.prioritization.nof;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
		LOGGER.debug("<"+managerId+">: <Order("+newOrder.getId()+")> "+"Choosing order to take instance from ordersWithInstance="
				+ ordersWithInstance + " for requestMember=" + newOrder.getRequestingMemberId());
		if (ordersWithInstance == null) {			
			return null;
		}
		
		List<String> servedMemberIds = getServedMemberIds(ordersWithInstance, newOrder);
				
		List<AccountingInfo> accounting = accountingPlugin.getAccountingInfo();
		Map<String, ResourceUsage> membersUsage = NoFHelper.calculateMembersUsage(localMemberId, accounting);
		LOGGER.info("<"+managerId+">: "+"Current membersUsage=" + membersUsage);		
		LinkedList<FederationMemberCredit> memberCredits = calctMemberCredits(servedMemberIds, membersUsage);
		if (memberCredits.isEmpty()) {
			LOGGER.info("<"+managerId+">: "+"There are no member credits. <Order("+newOrder.getId()+")>");
			return null;
		}

		Collections.sort(memberCredits, new FederationMemberCreditComparator());
		LOGGER.info("<"+managerId+">: <Order("+newOrder.getId()+")> Current memberCredits=" + memberCredits);
		
		double requestingMemberCredit = calcCredit(membersUsage, newOrder.getRequestingMemberId(), newOrder.getId());
		FederationMemberCredit memberWithLowestCredit = memberCredits.getFirst();
		
		LOGGER.info("<"+managerId+">: <Order("+newOrder.getId()+")> Requesting member("+newOrder.getRequestingMemberId()+") credit: " + requestingMemberCredit+"; "
				+ "Member("+memberWithLowestCredit.getMember().getId()+") with lowest credit: " + memberWithLowestCredit.getCredit());		
		
		if (memberWithLowestCredit.getCredit() < requestingMemberCredit) {
			String memberId = memberWithLowestCredit.getMember().getResourcesInfo().getId();
			List<Order> memberRequests = filterByRequestingMember(memberId, ordersWithInstance);
			Order mostRecentOrder = getMostRecentOrder(memberRequests);
			
			if(mostRecentOrder!=null){
				LOGGER.info("EXP-DEBUG-LOG <"+managerId+">: <Order("+newOrder.getId()+")> Requesting member("+newOrder.getRequestingMemberId()+") credit: " + requestingMemberCredit+"; "
					+ "Member("+memberWithLowestCredit.getMember().getId()+") with lowest credit: " + memberWithLowestCredit.getCredit());
			}
			
			return mostRecentOrder;
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
			double credit = calcCredit(membersUsage, currentMemberId, "");
			memberCredits.add(new FederationMemberCredit(currentMemberId, credit));
		}
		return memberCredits;
	}

	private List<String> getServedMemberIds(List<Order> orders, Order o) {
		Map<String, Integer> amountOfOrdersPerMember = new HashMap<String, Integer>();
		List<String> servedMemberIds = new LinkedList<String>();
		for (Order currentOrder : orders) {
			if (!servedMemberIds.contains(currentOrder.getRequestingMemberId())) {
				servedMemberIds.add(currentOrder.getRequestingMemberId());
			}
			if(!amountOfOrdersPerMember.containsKey(currentOrder.getRequestingMemberId())){
				amountOfOrdersPerMember.put(currentOrder.getRequestingMemberId(), 0);
			}else{
				amountOfOrdersPerMember.put(currentOrder.getRequestingMemberId(), amountOfOrdersPerMember.get(currentOrder.getRequestingMemberId())+1);
			}
		}		
		
		LOGGER.info("EXP-DEBUG-LOG <"+managerId+">: Current servedMemberIds and amount of orders=" + amountOfOrdersPerMember);
		
		return servedMemberIds;
	}

	protected double calcCredit(Map<String, ResourceUsage> membersUsage, String memberId, String orderId) {		
		double credit = 0;
		if (localMemberId.equals(memberId)) {
			if (prioritizeLocal) {
				return Double.MAX_VALUE;
			} else {
				return -1;
			}
		}

		if (membersUsage.containsKey(memberId)) {
			credit = membersUsage.get(memberId).getConsumed()		
					- membersUsage.get(memberId).getDonated();		
			if (!trustworthy) {
				credit = Math.max(0,
						credit + Math.log(membersUsage.get(memberId).getConsumed()));	//TODO create test to address this case
			}
			
			LOGGER.info("<"+managerId+">: "+memberId+" donated to "+managerId+" " + membersUsage.get(memberId).getConsumed()+"; "
					+ memberId+" consumed from "+managerId+" "+membersUsage.get(memberId).getDonated()+"; "
					+ "credit of "+memberId+" on "+managerId+" perspective: "+credit);
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

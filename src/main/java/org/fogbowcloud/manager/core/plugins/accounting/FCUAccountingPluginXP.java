package org.fogbowcloud.manager.core.plugins.accounting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.MainHelper;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.model.DateUtils;
import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.AccountingPluginXP;
import org.fogbowcloud.manager.core.plugins.BenchmarkingPlugin;
import org.fogbowcloud.manager.occi.order.Order;

public class FCUAccountingPluginXP extends FCUAccountingPlugin implements AccountingPluginXP {

	private String managerId;

	private static final Logger LOGGER = Logger.getLogger(FCUAccountingPluginXP.class);
	public static final String ACCOUNTING_DATASTORE_URL = "fcu_accounting_datastore_url";

	public FCUAccountingPluginXP(Properties properties, BenchmarkingPlugin benchmarkingPlugin) {
		this(properties, benchmarkingPlugin, new DateUtils());
	}
	
	public FCUAccountingPluginXP(Properties properties,
			BenchmarkingPlugin benchmarkingPlugin, DateUtils dateUtils) {		
		this.benchmarkingPlugin = benchmarkingPlugin;
		this.dateUtils = dateUtils;
		this.lastUpdate = dateUtils.currentTimeMillis();

		properties.put(AccountingDataStore.ACCOUNTING_DATASTORE_URL, 
				properties.getProperty(getDataStoreUrl()));		
		db = new AccountingDataStoreXPInMemory(properties);		
		managerId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
	}

	@Override
	public void update(List<Order> ordersWithInstance) {
		LOGGER.debug("<"+managerId+">: "+"Updating account with orders=" + ordersWithInstance);
		long now = dateUtils.currentTimeMillis();
		double updatingInterval = ((double) TimeUnit.MILLISECONDS.toSeconds(now - lastUpdate) / 60);
		LOGGER.debug("<"+managerId+">: "+"updating interval=" + updatingInterval);

		Map<AccountingEntryKey, AccountingInfo> usage = new HashMap<AccountingEntryKey, AccountingInfo>();

		for (Order order : ordersWithInstance) {

			if (order.getRequestingMemberId() == null || order.getProvidingMemberId() == null
					|| order.getGlobalInstanceId() == null) {
				continue;
			}
			
			double consumptionInterval = ((double) TimeUnit.MILLISECONDS.toSeconds(now
					- order.getFulfilledTime()) / 60);
			final double ACCEPTABLE_ERROR = 0.000001;
			consumptionInterval = consumptionInterval<ACCEPTABLE_ERROR?0:consumptionInterval;
			
			String userId = order.getFederationToken().getUser().getId();
			AccountingEntryKey current = new AccountingEntryKey(userId,
					order.getRequestingMemberId(), order.getProvidingMemberId());

			if (!usage.keySet().contains(current)) {
				AccountingInfo accountingInfo = new AccountingInfo(current.getUser(),
						current.getRequestingMember(), current.getProvidingMember());
				usage.put(current, accountingInfo);
			} 			

			double instanceUsage = getUsage(order, updatingInterval, consumptionInterval);
			
			usage.get(current).incrementCurrentInstances();		//every time we have to count again by the number of orders with instance
			usage.get(current).addConsumption(instanceUsage);
		}

		LOGGER.debug("<"+managerId+">: "+"current usage=" + usage);

		if ((usage.isEmpty()) || db.update(new ArrayList<AccountingInfo>(usage.values()))) {
			this.lastUpdate = now;
			LOGGER.debug("<"+managerId+">: "+"Updating lastUpdate to " + this.lastUpdate);
		}
	}
	
	@Override
	public void update(FederationMember member, double capacity){
		((AccountingDataStoreXP)db).updateQuota(member, capacity);
	}
	
}

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
import org.fogbowcloud.manager.core.plugins.BenchmarkingPlugin;
import org.fogbowcloud.manager.occi.order.Order;

public class FCUAccountingPlugin implements AccountingPlugin {

	private BenchmarkingPlugin benchmarkingPlugin;
	private AccountingDataStore db;
	private DateUtils dateUtils;
	private long lastUpdate;
	private String managerId;

	private static Logger LOGGER;
	public static final String ACCOUNTING_DATASTORE_URL = "fcu_accounting_datastore_url";

	public FCUAccountingPlugin(Properties properties, BenchmarkingPlugin benchmarkingPlugin) {
		this(properties, benchmarkingPlugin, new DateUtils());
	}
	
	public FCUAccountingPlugin(Properties properties,
			BenchmarkingPlugin benchmarkingPlugin, DateUtils dateUtils) {
		this.benchmarkingPlugin = benchmarkingPlugin;
		this.dateUtils = dateUtils;
		this.lastUpdate = dateUtils.currentTimeMillis();

		properties.put(AccountingDataStore.ACCOUNTING_DATASTORE_URL, 
				properties.getProperty(getDataStoreUrl()));
		db = new AccountingDataStore(properties);
		
		LOGGER = MainHelper.getLogger(FCUAccountingPlugin.class.getName(),properties.getProperty(ConfigurationConstants.XMPP_JID_KEY));
		managerId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
	}

	@Override
	public void update(List<Order> ordersWithInstance) {
		LOGGER.debug("Updating account with orders=" + ordersWithInstance);
		long now = dateUtils.currentTimeMillis();
		double updatingInterval = ((double) TimeUnit.MILLISECONDS.toSeconds(now - lastUpdate) / 60);
		LOGGER.debug("updating interval=" + updatingInterval);

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
			
			//added by Eduardo for debugging purposes
			usage.get(current).incrementCurrentInstances();
			usage.get(current).addConsumption(instanceUsage);
		}

		LOGGER.debug("current usage=" + usage);

		if ((usage.isEmpty()) || db.update(new ArrayList<AccountingInfo>(usage.values()))) {
			this.lastUpdate = now;
			LOGGER.debug("Updating lastUpdate to " + this.lastUpdate);
		}
	}
	
	@Override
	public void update(FederationMember member, double capacity){
		db.updateQuota(member, capacity);
	}

	private double getUsage(Order order, double updatingInterval , double consumptionInterval) {
		double instancePower = benchmarkingPlugin.getPower(order.getGlobalInstanceId());
		return instancePower * Math.min(consumptionInterval, updatingInterval);
	}	
	
	protected String getDataStoreUrl() {
		return ACCOUNTING_DATASTORE_URL;
	}	
	
	@Override
	public List<AccountingInfo> getAccountingInfo() {
		return db.getAccountingInfo();
	}

	@Override
	public AccountingInfo getAccountingInfo(String user, String requestingMember,
			String providingMember) {
		return db.getAccountingInfo(user, requestingMember, providingMember);
	}
	
}

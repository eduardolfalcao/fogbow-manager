package org.fogbowcloud.manager.core.plugins.accounting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.ManagerControllerHelper;
import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.occi.DataStoreHelper;
import org.fogbowcloud.manager.occi.order.Order;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

public class AccountingDataStoreXPInMemory extends AccountingDataStore{
	
	protected static final String INSTANCES_COL = "instances";
	protected static final String VIRTUAL_QUOTA_COL = "virtual_quota";	

	private String managerId;
	private Properties properties;
	
	private Map<AccountingEntryKey, AccountingInfo> database;

	private static final Logger LOGGER = Logger.getLogger(AccountingDataStoreXPInMemory.class);
	
	public AccountingDataStoreXPInMemory(Properties properties) {		
		this.properties = properties;		
		managerId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
		database = new HashMap<AccountingEntryKey, AccountingInfo>();
	}
	
	private static final String UPDATE_MEMBER_USAGE_SQL = "UPDATE " + USAGE_TABLE_NAME
			+ " SET usage = usage + ?, instances = ? WHERE user = ? AND requesting_member = ? AND providing_member = ?";
	
	private static final String INSERT_MEMBER_USAGE_SQL = "INSERT INTO " + USAGE_TABLE_NAME
			+ " VALUES(?, ?, ?, ?, ?, ?)";
	
	@Override
	public synchronized boolean update(List<AccountingInfo> usage) {
		
		setInstancesCountToZero();
		
		LOGGER.debug("<"+managerId+">: "+"Updating usage into database.");
		LOGGER.debug("<"+managerId+">: "+"Usage=" + usage);

		if (usage == null) {
			LOGGER.warn("<"+managerId+">: "+"Members and users must not be null.");
			return false;
		}
		
		for(AccountingInfo info : usage){
			if(info.getProvidingMember()==null || info.getRequestingMember()==null ||
					info.getUser()==null){
				LOGGER.warn("<"+managerId+">: Providing, requesting member and users must not be null. Exception");
				return false;
			}
			AccountingEntryKey key = new AccountingEntryKey(info.getUser(), info.getRequestingMember(), info.getProvidingMember());
			if(database.containsKey(key)){
				database.get(key).addConsumption(info.getUsage());
				database.get(key).setCurrentInstances(info.getCurrentInstances());
			} else{
				database.put(key, new AccountingInfo(info));
			}
		}
		
		return true;
	}
	
	private void setInstancesCountToZero() {
		LOGGER.debug("<"+managerId+">: "+"Updating instance count into database to ZERO.");		
		for (Map.Entry<AccountingEntryKey, AccountingInfo> entry : database.entrySet()){
			entry.getValue().setCurrentInstances(0);
		}
	}
	
	public boolean updateQuota(FederationMember member, double capacity) {
		
		LOGGER.debug("<"+managerId+">: "+"Updating quota into database.");
		LOGGER.debug("<"+managerId+">: "+"Member=" + member+", quota="+capacity);

		if (member == null) {
			LOGGER.warn("<"+managerId+">: "+"Member must not be null.");
			return false;
		}
		
		for(Map.Entry<AccountingEntryKey, AccountingInfo> entry : database.entrySet()){
			if(entry.getKey().getProvidingMember().equals(managerId) &&
					entry.getKey().getRequestingMember().equals(member.getId())){
				entry.getValue().setQuota(capacity);
			}
		}
		return true;
	} 
	

	@Override
	protected AccountingInfo getAccountingInfo(AccountingEntryKey key) {
		LOGGER.debug("<"+managerId+">: "+"Getting accountingInfo from " + key);		
		return database.get(key);
	}

	
	@Override
	public List<AccountingInfo> getAccountingInfo() {		
		List<AccountingInfo> accounting = new ArrayList<AccountingInfo>();
		for(Map.Entry<AccountingEntryKey, AccountingInfo> entry : database.entrySet()){
			accounting.add(entry.getValue());
		}
		return accounting;
	}
}

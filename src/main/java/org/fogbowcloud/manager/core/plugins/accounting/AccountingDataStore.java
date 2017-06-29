package org.fogbowcloud.manager.core.plugins.accounting;

import java.sql.Connection;
import java.sql.DriverManager;
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
import org.fogbowcloud.manager.MainHelper;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.ManagerControllerHelper;
import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.occi.DataStoreHelper;

public class AccountingDataStore {

	public static final String ACCOUNTING_DATASTORE_URL = "accounting_datastore_url";
	public static final String ACCOUNTING_DATASTORE_SQLITE_DRIVER = "org.sqlite.JDBC";
	
	protected static final String USAGE_TABLE_NAME = "usage";
	protected static final String USER_COL = "user";
	protected static final String REQUESTING_MEMBER_COL = "requesting_member";
	protected static final String PROVIDING_MEMBER_COL = "providing_member";
	protected static final String USAGE_COL = "usage";
	protected static final String INSTANCES_COL = "instances";
	private static final String DEFAULT_DATASTORE_NAME = "datastore_accounting.slite";
	protected static final String ERROR_WHILE_INITIALIZING_THE_DATA_STORE = 
			"Error while initializing the Accouting DataStore.";

	private String dataStoreURL;
	private String managerId;

	private static final Logger LOGGER = Logger.getLogger(AccountingDataStore.class);
	
	public AccountingDataStore(Properties properties) {
		
		managerId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);		
		String dataStoreURLProperties = properties.getProperty(ACCOUNTING_DATASTORE_URL);
		this.dataStoreURL = DataStoreHelper.getDataStoreUrl(dataStoreURLProperties,
				DEFAULT_DATASTORE_NAME);

		Statement statement = null;
		Connection connection = null;
		try {
			LOGGER.debug("DatastoreURL: " + dataStoreURL);

			Class.forName(ACCOUNTING_DATASTORE_SQLITE_DRIVER);

			connection = getConnection();
			statement = connection.createStatement();
			statement
					.execute("CREATE TABLE IF NOT EXISTS usage("
							+ "user VARCHAR(255) NOT NULL, "
							+ "requesting_member VARCHAR(255) NOT NULL, "
							+ "providing_member VARCHAR(255) NOT NULL, "
							+ "usage DOUBLE,"
							+ "instances INTEGER,"
							+ "virtual_quota DOUBLE,"
							+ "PRIMARY KEY (user, requesting_member, providing_member)"
							+ ")");
			statement.close();
		} catch (Exception e) {
			LOGGER.error("<"+managerId+">: "+ERROR_WHILE_INITIALIZING_THE_DATA_STORE, e);
			throw new Error(ERROR_WHILE_INITIALIZING_THE_DATA_STORE, e);
		} finally {
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(statement);
			close(statements, connection);
		}
	}
	
	private static final String UPDATE_MEMBER_USAGE_SQL = "UPDATE " + USAGE_TABLE_NAME
			+ " SET usage = usage + ?, instances = ? WHERE user = ? AND requesting_member = ? AND providing_member = ?";
	
	private static final String INSERT_MEMBER_USAGE_SQL = "INSERT INTO " + USAGE_TABLE_NAME
			+ " VALUES(?, ?, ?, ?, ?, ?)";
	
	public boolean update(List<AccountingInfo> usage) {
		
		setInstancesCountToZero();
		
		LOGGER.debug("<"+managerId+">: "+"Updating usage into database.");
		LOGGER.debug("<"+managerId+">: "+"Usage=" + usage);

		if (usage == null) {
			LOGGER.warn("<"+managerId+">: "+"Members and users must not be null.");
			return false;
		}
		
		PreparedStatement updateMemberStatement = null;
		PreparedStatement insertMemberStatement = null;
		
		Connection connection = null;

		try {
			connection = getConnection();
			connection.setAutoCommit(false);

			insertMemberStatement = connection.prepareStatement(INSERT_MEMBER_USAGE_SQL);
			updateMemberStatement = connection.prepareStatement(UPDATE_MEMBER_USAGE_SQL);
		
			addMemberStatements(usage, updateMemberStatement, insertMemberStatement);

			if (hasBatchExecutionError(insertMemberStatement.executeBatch())
					| hasBatchExecutionError(updateMemberStatement.executeBatch())) {
				LOGGER.debug("<"+managerId+">: "+"Rollback will be executed.");
				connection.rollback();
				return false;
			}

			connection.commit();
			return true;
		} catch (SQLException e) {
			LOGGER.error("<"+managerId+">: "+"Couldn't account usage.", e);
			try {
				if (connection != null) {
					connection.rollback();
				}
			} catch (SQLException e1) {
				LOGGER.error("<"+managerId+">: "+"Couldn't rollback transaction.", e1);
			}
			return false;
		} finally {
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(updateMemberStatement);
			statements.add(insertMemberStatement);
			close(statements, connection);
		}
	}
	
	
	//TODO for debugging purposes
	private static final String SET_INSTANCES_COUNT_ZERO_SQL = "UPDATE " + USAGE_TABLE_NAME + " SET instances = 0";	
	private boolean setInstancesCountToZero() {
		LOGGER.debug("<"+managerId+">: "+"Updating instance count into database to ZERO.");

		Statement updateStatement = null;		
		Connection connection = null;

		try {
			connection = getConnection();
			updateStatement = connection.createStatement();
			updateStatement.execute(SET_INSTANCES_COUNT_ZERO_SQL);
			return true;
		} catch (SQLException e) {
			LOGGER.error("<"+managerId+">: "+"Couldn't set instances count to zero.", e);
			try {
				if (connection != null) {
					connection.rollback();
				}
			} catch (SQLException e1) {
				LOGGER.error("<"+managerId+">: "+"Couldn't rollback transaction.", e1);
			}
			return false;
		} finally {
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(updateStatement);
			close(statements, connection);
		}
	}
	
	private static final String UPDATE_QUOTA_SQL = "UPDATE " + USAGE_TABLE_NAME
			+ " SET virtual_quota = ? WHERE providing_member = ? AND requesting_member = ?";
	
	public boolean updateQuota(FederationMember member, double capacity) {
		
		LOGGER.debug("<"+managerId+">: "+"Updating quota into database.");
		LOGGER.debug("<"+managerId+">: "+"Member=" + member+", quota="+capacity);

		if (member == null) {
			LOGGER.warn("<"+managerId+">: "+"Member must not be null.");
			return false;
		}
		
		PreparedStatement updateQuotaStatement = null;		
		Connection connection = null;

		try {
			connection = getConnection();
			connection.setAutoCommit(false);

			updateQuotaStatement = connection.prepareStatement(UPDATE_QUOTA_SQL);
			updateQuotaStatement.setDouble(1, capacity);
			updateQuotaStatement.setString(2, managerId);
			updateQuotaStatement.setString(3, member.getId());
			updateQuotaStatement.execute();
		
			connection.commit();
			return true;
		} catch (SQLException e) {
			LOGGER.error("<"+managerId+">: "+"Couldn't update quotas.", e);
			try {
				if (connection != null) {
					connection.rollback();
				}
			} catch (SQLException e1) {
				LOGGER.error("Couldn't rollback transaction.", e1);
			}
			return false;
		} finally {
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(updateQuotaStatement);
			close(statements, connection);
		}
	} 
	
	
	
	private boolean hasBatchExecutionError(int[] executeBatch) {
		for (int i : executeBatch) {
			if (i == PreparedStatement.EXECUTE_FAILED) {
				return true;
			}
		}
		return false;
	}
	
	private void addMemberStatements(List<AccountingInfo> usage,
			PreparedStatement updateMemberStatement, PreparedStatement insertMemberStatement)
			throws SQLException {
		
		List<AccountingEntryKey> entryKeys = getEntryKeys();

		LOGGER.debug("<"+managerId+">: "+"Database entry keys=" + entryKeys);
		
		// preprocessing data
		Map<AccountingEntryKey, AccountingInfo> processedUsage = new HashMap<AccountingEntryKey, AccountingInfo>();
		
		for (AccountingInfo accountingInfo : usage) {
			// insert operation
			AccountingEntryKey currentKey = new AccountingEntryKey(accountingInfo.getUser(), accountingInfo
					.getRequestingMember(), accountingInfo.getProvidingMember());
			if (!entryKeys.contains(currentKey)) {

				if (processedUsage.containsKey(currentKey)) {
					processedUsage.get(currentKey).addConsumption(accountingInfo.getUsage());
				} else {
					processedUsage.put(currentKey, accountingInfo);
				}				
			} else { // update operation
				processedUsage.put(currentKey, accountingInfo);
			}
		}
		
		// creating statements
		for (AccountingEntryKey currentKey : processedUsage.keySet()) {
			AccountingInfo accountingEntry = processedUsage.get(currentKey);
			// inserting new usage entry
			if (!entryKeys.contains(currentKey)) {

				LOGGER.debug("<"+managerId+">: "+"New accountingEntry=" + accountingEntry);
				insertMemberStatement.setString(1, accountingEntry.getUser());
				insertMemberStatement.setString(2, accountingEntry.getRequestingMember());
				insertMemberStatement.setString(3, accountingEntry.getProvidingMember());
				insertMemberStatement.setDouble(4, accountingEntry.getUsage());
				insertMemberStatement.setInt(5, accountingEntry.getCurrentInstances());
				insertMemberStatement.setDouble(6, -1.0);
				insertMemberStatement.addBatch();
				
			} else { // updating an existing entry
				LOGGER.debug("<"+managerId+">: "+"Existing accountingEntry=" + accountingEntry);
				updateMemberStatement.setDouble(1, accountingEntry.getUsage());
				updateMemberStatement.setInt(2, accountingEntry.getCurrentInstances());
				updateMemberStatement.setString(3, accountingEntry.getUser());
				updateMemberStatement.setString(4, accountingEntry.getRequestingMember());
				updateMemberStatement.setString(5, accountingEntry.getProvidingMember());				
				updateMemberStatement.addBatch();
			}
		}
	}

	private static final String SELECT_ALL_USAGE_SQL = "SELECT * FROM " + USAGE_TABLE_NAME;
		
	private List<AccountingEntryKey> getEntryKeys() {
		LOGGER.debug("<"+managerId+">: "+"Getting database keys.");

		Statement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.createStatement();

			statement.execute(SELECT_ALL_USAGE_SQL);
			ResultSet rs = statement.getResultSet();

			List<AccountingEntryKey> dbKeys = new ArrayList<AccountingEntryKey>();
			while (rs.next()) {
				dbKeys.add(new AccountingEntryKey(rs.getString(USER_COL), rs
						.getString(REQUESTING_MEMBER_COL), rs.getString(PROVIDING_MEMBER_COL)));
			}
			return dbKeys;
		} catch (SQLException e) {
			LOGGER.error("<"+managerId+">: "+"Couldn't get keys from DB.", e);
			return null;
		}
	}

	public List<AccountingInfo> getAccountingInfo() {
		LOGGER.debug("<"+managerId+">: "+"Getting AccountingInfo...");
		Statement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.createStatement();

			statement.execute(SELECT_ALL_USAGE_SQL);
			return createAccounting(statement.getResultSet());
		} catch (SQLException e) {
			LOGGER.error("<"+managerId+">: "+"Couldn't get keys from DB.", e);
			return null;
		}	
	}

	private static final String SELECT_SPECIFIC_USAGE_SQL = "SELECT * FROM " + USAGE_TABLE_NAME
			+ " WHERE user = ? AND requesting_member = ? AND providing_member = ?";
	
	private AccountingInfo getAccountingInfo(AccountingEntryKey key) {
		LOGGER.debug("<"+managerId+">: "+"Getting accountingInfo to " + key);

		AccountingEntryKey entryKey = (AccountingEntryKey) key;

		PreparedStatement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(SELECT_SPECIFIC_USAGE_SQL);
			statement.setString(1, entryKey.getUser());
			statement.setString(2, entryKey.getRequestingMember());
			statement.setString(3, entryKey.getProvidingMember());
			statement.execute();

			ResultSet rs = statement.getResultSet();

			if (rs.next()) {
				String user = rs.getString(USER_COL);
				String requestingMember = rs.getString(REQUESTING_MEMBER_COL);
				String providingMember = rs.getString(PROVIDING_MEMBER_COL);
				double usage = rs.getDouble(USAGE_COL);
				int instances = rs.getInt(INSTANCES_COL);

				AccountingInfo accountingInfo = new AccountingInfo(user, requestingMember,
						providingMember);
				accountingInfo.addConsumption(usage);
				accountingInfo.setCurrentInstances(instances);
				return accountingInfo;
			}
		} catch (SQLException e) {
			LOGGER.error("<"+managerId+">: "+"Couldn't get keys from DB.", e);
			return null;
		} finally {
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(statement);
			close(statements, conn);
		}

		return null;
	}

	/**
	 * @return the connection
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		try {
			return DriverManager.getConnection(this.dataStoreURL);
		} catch (SQLException e) {
			LOGGER.error("<"+managerId+">: "+"Error while getting a new connection from the connection pool.", e);
			throw e;
		}
	}
	
	private void close(List<Statement> statements, Connection conn) {
		if (statements != null) {
			for(Statement s : statements){
				try {
					if (!s.isClosed())
						s.close();//
				} catch (SQLException e) {
					LOGGER.error("<"+managerId+">: "+"Couldn't close statement"+s, e);
				}
			}
		}

		if (conn != null) {
			try {
				if (!conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				LOGGER.error("<"+managerId+">: "+"Couldn't close connection", e);
			}
		}
	}

	protected List<AccountingInfo> createAccounting(ResultSet rs) {
		List<AccountingInfo> accounting = new ArrayList<AccountingInfo>();
		try {
			while (rs.next()) {
				String user = rs.getString(USER_COL);
				String requestingMember = rs.getString(REQUESTING_MEMBER_COL);
				String providingMember = rs.getString(PROVIDING_MEMBER_COL);
				AccountingInfo userAccounting = new AccountingInfo(user, requestingMember,
						providingMember);
				userAccounting.addConsumption(rs.getDouble(USAGE_COL));
				userAccounting.setCurrentInstances(rs.getInt(INSTANCES_COL));
				accounting.add(userAccounting);				
			}
		} catch (SQLException e) {
			LOGGER.error("<"+managerId+">: "+"Error while creating accounting from ResultSet.", e);
			return null;
		}
		return accounting;
	}

	public AccountingInfo getAccountingInfo(String user, String requestingMember,
			String providingMember) {
		return getAccountingInfo(new AccountingEntryKey(user, requestingMember, providingMember));
	}
}

class AccountingEntryKey {	
	private String user;
	private String requestingMember;
	private String providingMember;
	
	public AccountingEntryKey(String user, String requestingMember, String providingMember) {
		this.user = user;
		this.requestingMember = requestingMember;
		this.providingMember = providingMember;
	}
	
	public String getUser() {
		return user;
	}

	public String getRequestingMember() {
		return requestingMember;
	}

	public String getProvidingMember() {
		return providingMember;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AccountingEntryKey) {
			AccountingEntryKey other = (AccountingEntryKey) obj;
			return getUser().equals(other.getUser())
					&& getProvidingMember().equals(other.getProvidingMember())
					&& getRequestingMember().equals(other.getRequestingMember());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public String toString() {
		return "user=" + user + "; requestigMember=" + requestingMember + "; providingMember="
				+ providingMember;
	}
}

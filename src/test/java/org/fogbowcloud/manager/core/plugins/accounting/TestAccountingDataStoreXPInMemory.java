package org.fogbowcloud.manager.core.plugins.accounting;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.model.FederationMember;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestAccountingDataStoreXPInMemory {

	private static final Logger LOGGER = Logger.getLogger(TestAccountingDataStoreXPInMemory.class);

	private final double ACCEPTABLE_ERROR = 0.000001;

	Properties properties = null;
	AccountingDataStoreXPInMemory db = null;

	@Before
	public void initialize() {
		LOGGER.debug("Creating data store.");
		properties = new Properties();
		properties.put(ConfigurationConstants.XMPP_JID_KEY, "managerId");

		db = new AccountingDataStoreXPInMemory(properties);
	}	

	@After
	public void tearDown() throws IOException {
		db = null;
	}	

	@Test
	public void testUpdateInsertingInvalidUser() throws SQLException {
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();
		usage.add(new AccountingInfo(null, "requestingMember", "providingMember"));

		Assert.assertFalse(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		Assert.assertEquals(0, accounting.size());
	}

	@Test
	public void testUpdateInsertingInvalidRequestingMember() throws SQLException {
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();
		usage.add(new AccountingInfo("user", null, "providingMember"));

		Assert.assertFalse(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		Assert.assertEquals(0, accounting.size());
	}

	@Test
	public void testUpdateInsertingInvalidProvidingMember() throws SQLException {
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();
		usage.add(new AccountingInfo("user", "requestingMember", null));

		Assert.assertFalse(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		Assert.assertEquals(0, accounting.size());
	}

	@Test
	public void testUpdateInserting() throws SQLException {
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();
		usage.add(new AccountingInfo("user", "requestingMember", "providingMember"));

		Assert.assertTrue(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		Assert.assertEquals(1, accounting.size());
		Assert.assertEquals("user", accounting.get(0).getUser());
		Assert.assertEquals("requestingMember", accounting.get(0).getRequestingMember());
		Assert.assertEquals("providingMember", accounting.get(0).getProvidingMember());
		Assert.assertEquals(0, accounting.get(0).getUsage(), ACCEPTABLE_ERROR);
		Assert.assertEquals(0, accounting.get(0).getCurrentInstances(), ACCEPTABLE_ERROR);
	}

	@Test
	public void testUpdateUpdating() throws SQLException {
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();
		usage.add(new AccountingInfo("user", "requestingMember", "providingMember"));

		Assert.assertTrue(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		// checking initial accounting
		Assert.assertEquals(1, accounting.size());
		Assert.assertEquals("user", accounting.get(0).getUser());
		Assert.assertEquals("requestingMember", accounting.get(0).getRequestingMember());
		Assert.assertEquals("providingMember", accounting.get(0).getProvidingMember());
		Assert.assertEquals(0, accounting.get(0).getUsage(), ACCEPTABLE_ERROR);
		Assert.assertEquals(0, accounting.get(0).getCurrentInstances(), ACCEPTABLE_ERROR);

		// adding consuption
		usage.get(0).addConsumption(20);

		Assert.assertTrue(db.update(usage));

		accounting = db.getAccountingInfo();

		// checking accounting was updated
		Assert.assertEquals(1, accounting.size());
		Assert.assertEquals("user", accounting.get(0).getUser());
		Assert.assertEquals("requestingMember", accounting.get(0).getRequestingMember());
		Assert.assertEquals("providingMember", accounting.get(0).getProvidingMember());
		Assert.assertEquals(20, accounting.get(0).getUsage(), ACCEPTABLE_ERROR);
		Assert.assertEquals(0, accounting.get(0).getCurrentInstances(), ACCEPTABLE_ERROR);
		
		// adding instance, and 20 of consumption
		usage.get(0).incrementCurrentInstances();
		
		Assert.assertTrue(db.update(usage));
		accounting = db.getAccountingInfo();
		
		// checking accounting was updated
		Assert.assertEquals(1, accounting.size());
		Assert.assertEquals("user", accounting.get(0).getUser());
		Assert.assertEquals("requestingMember", accounting.get(0).getRequestingMember());
		Assert.assertEquals("providingMember", accounting.get(0).getProvidingMember());
		Assert.assertEquals(40, accounting.get(0).getUsage(), ACCEPTABLE_ERROR);
		Assert.assertEquals(1, accounting.get(0).getCurrentInstances(), ACCEPTABLE_ERROR);
	}

	@Test
	public void testUpdateInsertingAndUpdating() throws SQLException {
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();
		AccountingInfo accountingInfo1 = new AccountingInfo("user1", "requestingMember1",
				"providingMember1");
		int initialUsage1 = 10;
		accountingInfo1.addConsumption(initialUsage1);
		accountingInfo1.incrementCurrentInstances();
		usage.add(accountingInfo1);

		Assert.assertTrue(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		// checking initial accounting
		Assert.assertEquals(1, accounting.size());
		Assert.assertEquals("user1", accounting.get(0).getUser());
		Assert.assertEquals("requestingMember1", accounting.get(0).getRequestingMember());
		Assert.assertEquals("providingMember1", accounting.get(0).getProvidingMember());
		Assert.assertEquals(initialUsage1, accounting.get(0).getUsage(), ACCEPTABLE_ERROR);
		Assert.assertEquals(1, accounting.get(0).getCurrentInstances(), ACCEPTABLE_ERROR);

		// new usage and consuption to existing user
		usage = new ArrayList<AccountingInfo>();
		accountingInfo1 = new AccountingInfo("user1", "requestingMember1", "providingMember1");
		int finalUsage1 = 30;
		accountingInfo1.addConsumption(finalUsage1);
		accountingInfo1.incrementCurrentInstances();
		accountingInfo1.incrementCurrentInstances();

		AccountingInfo accountingInfo2 = new AccountingInfo("user2", "requestingMember2",
				"providingMember2");
		int usage2 = 20;
		accountingInfo2.addConsumption(usage2);
		accountingInfo2.incrementCurrentInstances();
		usage.add(accountingInfo1);
		usage.add(accountingInfo2);

		Assert.assertTrue(db.update(usage));

		accounting = db.getAccountingInfo();

		// checking accounting was updated
		Assert.assertEquals(2, accounting.size());
		if (accounting.get(0).getUser().equals("user1")) {
			accountingInfo1 = accounting.get(0);
			accountingInfo2 = accounting.get(1);
		} else {
			accountingInfo1 = accounting.get(1);
			accountingInfo2 = accounting.get(0);
		}

		// checking
		Assert.assertEquals("user1", accountingInfo1.getUser());
		Assert.assertEquals("requestingMember1", accountingInfo1.getRequestingMember());
		Assert.assertEquals("providingMember1", accountingInfo1.getProvidingMember());
		Assert.assertEquals(initialUsage1 + finalUsage1, accountingInfo1.getUsage(),
				ACCEPTABLE_ERROR);
		Assert.assertEquals(2, accountingInfo1.getCurrentInstances(), ACCEPTABLE_ERROR);

		Assert.assertEquals("user2", accountingInfo2.getUser());
		Assert.assertEquals("requestingMember2", accountingInfo2.getRequestingMember());
		Assert.assertEquals("providingMember2", accountingInfo2.getProvidingMember());
		Assert.assertEquals(usage2, accountingInfo2.getUsage(), ACCEPTABLE_ERROR);
		Assert.assertEquals(1, accountingInfo2.getCurrentInstances(), ACCEPTABLE_ERROR);
	}
	
	@Test
	public void testUpdateInsertingSameUserMoreInstances() throws SQLException {
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();
		AccountingInfo accountingInfo1 = new AccountingInfo("user1", "requestingMember1",
				"providingMember1");
		int initialUsage1 = 10;
		accountingInfo1.addConsumption(initialUsage1);
		usage.add(accountingInfo1);
		
		accountingInfo1 = new AccountingInfo("user1", "requestingMember1",
				"providingMember1");
		int initialUsage2 = 20;
		accountingInfo1.addConsumption(initialUsage2);
		usage.add(accountingInfo1);

		Assert.assertTrue(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		Assert.assertEquals(1, accounting.size());
		Assert.assertEquals("user1", accounting.get(0).getUser());
		Assert.assertEquals("requestingMember1", accounting.get(0).getRequestingMember());
		Assert.assertEquals("providingMember1", accounting.get(0).getProvidingMember());
		Assert.assertEquals(initialUsage1 + initialUsage2, accounting.get(0).getUsage(), ACCEPTABLE_ERROR);
	}

	@Test
	public void testUpdateInsertingSameUserMoreInstancesAndUpdateExisting() throws SQLException {
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();
		AccountingInfo accountingInfo1 = new AccountingInfo("user1", "requestingMember1",
				"providingMember1");
		int initialUsage1 = 10;
		accountingInfo1.addConsumption(initialUsage1);
		usage.add(accountingInfo1);

		Assert.assertTrue(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		// checking initial accounting
		Assert.assertEquals(1, accounting.size());
		Assert.assertEquals("user1", accounting.get(0).getUser());
		Assert.assertEquals("requestingMember1", accounting.get(0).getRequestingMember());
		Assert.assertEquals("providingMember1", accounting.get(0).getProvidingMember());
		Assert.assertEquals(initialUsage1, accounting.get(0).getUsage(), ACCEPTABLE_ERROR);

		// new usage and consuption to existing user
		usage = new ArrayList<AccountingInfo>();
		accountingInfo1 = new AccountingInfo("user1", "requestingMember1", "providingMember1");
		int finalUsage1 = 30;
		accountingInfo1.addConsumption(finalUsage1);
		usage.add(accountingInfo1);

		AccountingInfo accountingInfo2 = new AccountingInfo("user2", "requestingMember2",
				"providingMember2");
		int initialUsage2 = 20;
		accountingInfo2.addConsumption(initialUsage2);
		usage.add(accountingInfo2);

		accountingInfo2 = new AccountingInfo("user2", "requestingMember2",
				"providingMember2");
		int finalUsage2 = 20;
		accountingInfo2.addConsumption(finalUsage2);
		usage.add(accountingInfo2);
		
		Assert.assertTrue(db.update(usage));

		accounting = db.getAccountingInfo();

		// checking accounting was updated
		Assert.assertEquals(2, accounting.size());
		if (accounting.get(0).getUser().equals("user1")) {
			accountingInfo1 = accounting.get(0);
			accountingInfo2 = accounting.get(1);
		} else {
			accountingInfo1 = accounting.get(1);
			accountingInfo2 = accounting.get(0);
		}

		// checking
		Assert.assertEquals("user1", accountingInfo1.getUser());
		Assert.assertEquals("requestingMember1", accountingInfo1.getRequestingMember());
		Assert.assertEquals("providingMember1", accountingInfo1.getProvidingMember());
		Assert.assertEquals(initialUsage1 + finalUsage1, accountingInfo1.getUsage(),
				ACCEPTABLE_ERROR);

		Assert.assertEquals("user2", accountingInfo2.getUser());
		Assert.assertEquals("requestingMember2", accountingInfo2.getRequestingMember());
		Assert.assertEquals("providingMember2", accountingInfo2.getProvidingMember());
		Assert.assertEquals(initialUsage2 + finalUsage2, accountingInfo2.getUsage(), ACCEPTABLE_ERROR);
	}
	
	@Test
	public void testEqualsAccountingEntryKey(){
		AccountingEntryKey entryKey1 = new AccountingEntryKey("user1", "member1", "member2");
		AccountingEntryKey entryKey2 = new AccountingEntryKey("user1", "member1", "member2");
		Assert.assertEquals(entryKey1, entryKey2);
	}

	@Test
	public void testGetSpecificAccountInfo() throws SQLException {
		// populating DB
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();
		AccountingInfo expectedAccountingInfo = new AccountingInfo("user", "requestingMember",
				"providingMember");
		usage.add(expectedAccountingInfo);

		Assert.assertTrue(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		Assert.assertEquals(1, accounting.size());

		// checking getAccountingInfo method
		AccountingInfo returnedInfo = db.getAccountingInfo("user", "requestingMember",
				"providingMember");
		Assert.assertEquals(expectedAccountingInfo.getUser(), returnedInfo.getUser());
		Assert.assertEquals(expectedAccountingInfo.getRequestingMember(),
				returnedInfo.getRequestingMember());
		Assert.assertEquals(expectedAccountingInfo.getProvidingMember(),
				returnedInfo.getProvidingMember());
		Assert.assertEquals(expectedAccountingInfo.getUsage(), returnedInfo.getUsage(),
				ACCEPTABLE_ERROR);
	}

	@Test
	public void testGetSpecificAccountInfoMoreEntries() throws SQLException {
		// populating DB
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();
		AccountingInfo expectedAccountingInfo1 = new AccountingInfo("user1", "requestingMember1",
				"providingMember1");
		expectedAccountingInfo1.addConsumption(50);
		usage.add(expectedAccountingInfo1);

		AccountingInfo expectedAccountingInfo2 = new AccountingInfo("user2", "requestingMember2",
				"providingMember2");
		expectedAccountingInfo2.addConsumption(100);
		usage.add(expectedAccountingInfo2);

		for (int i = 3; i < 53; i++) {
			usage.add(new AccountingInfo("user" + i, "requestingMember" + i, "providingMember" + i));
		}

		Assert.assertTrue(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		Assert.assertEquals(52, accounting.size());

		// checking accountingInfo1
		AccountingInfo returnedInfo = db.getAccountingInfo("user1", "requestingMember1",
				"providingMember1");
		Assert.assertEquals(expectedAccountingInfo1.getUser(), returnedInfo.getUser());
		Assert.assertEquals(expectedAccountingInfo1.getRequestingMember(),
				returnedInfo.getRequestingMember());
		Assert.assertEquals(expectedAccountingInfo1.getProvidingMember(),
				returnedInfo.getProvidingMember());
		Assert.assertEquals(expectedAccountingInfo1.getUsage(), returnedInfo.getUsage(),
				ACCEPTABLE_ERROR);

		// checking accountingInfo2
		returnedInfo = db.getAccountingInfo("user2", "requestingMember2", "providingMember2");
		Assert.assertEquals(expectedAccountingInfo2.getUser(), returnedInfo.getUser());
		Assert.assertEquals(expectedAccountingInfo2.getRequestingMember(),
				returnedInfo.getRequestingMember());
		Assert.assertEquals(expectedAccountingInfo2.getProvidingMember(),
				returnedInfo.getProvidingMember());
		Assert.assertEquals(expectedAccountingInfo2.getUsage(), returnedInfo.getUsage(),
				ACCEPTABLE_ERROR);
	}

	@Test
	public void testGetInvalidSpecificAccountInfo() throws SQLException {
		// populating DB
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();
		AccountingInfo expectedAccountingInfo = new AccountingInfo("user", "requestingMember",
				"providingMember");
		usage.add(expectedAccountingInfo);

		Assert.assertTrue(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		Assert.assertEquals(1, accounting.size());

		// checking
		Assert.assertNull(db.getAccountingInfo("invalid", "invalid", "invalid"));
	}

	@Test
	public void testGetInvalidSpecificAccountInfo2() throws SQLException {
		// populating DB
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();
		AccountingInfo expectedAccountingInfo = new AccountingInfo("user", "requestingMember",
				"providingMember");
		usage.add(expectedAccountingInfo);

		Assert.assertTrue(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		Assert.assertEquals(1, accounting.size());

		// checking
		Assert.assertNull(db.getAccountingInfo(null, null, null));
	}

	@Test
	public void testFullAccountingInfo() throws SQLException {
		// populating DB
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();

		for (int i = 0; i < 100; i++) {
			AccountingInfo accountingInfo = new AccountingInfo("user" + i, "requestingMember" + i,
					"providingMember" + i);
			accountingInfo.addConsumption(i);
			usage.add(accountingInfo);
		}

		Assert.assertTrue(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		Assert.assertEquals(100, accounting.size());

		// checking get full accountingInfo
		List<AccountingInfo> returnedAccounting = db.getAccountingInfo();

		for (int i = 0; i < 100; i++) {
			AccountingInfo expectedAccountingInfo = new AccountingInfo("user" + i,
					"requestingMember" + i, "providingMember" + i);
			expectedAccountingInfo.addConsumption(i);

			Assert.assertTrue(returnedAccounting.contains(expectedAccountingInfo));
		}
	}
	
	@Test
	public void testUpdateInsertingAndUpdatingQuota() throws SQLException {
		List<AccountingInfo> usage = new ArrayList<AccountingInfo>();
		AccountingInfo accountingInfo1 = new AccountingInfo("user1", "requestingMember1",
				"managerId");
		int initialUsage1 = 10;
		accountingInfo1.addConsumption(initialUsage1);
		accountingInfo1.incrementCurrentInstances();
		usage.add(accountingInfo1);

		Assert.assertTrue(db.update(usage));

		List<AccountingInfo> accounting = db.getAccountingInfo();

		// checking initial accounting
		Assert.assertEquals(1, accounting.size());
		Assert.assertEquals("user1", accounting.get(0).getUser());
		Assert.assertEquals("requestingMember1", accounting.get(0).getRequestingMember());
		Assert.assertEquals("managerId", accounting.get(0).getProvidingMember());
		Assert.assertEquals(initialUsage1, accounting.get(0).getUsage(), ACCEPTABLE_ERROR);
		Assert.assertEquals(1, accounting.get(0).getCurrentInstances(), ACCEPTABLE_ERROR);

		Assert.assertTrue(db.updateQuota(new FederationMember("requestingMember1"), 10.0));
		
		accounting = db.getAccountingInfo();
		
		// checking if only quota has changed
		Assert.assertEquals(1, accounting.size());
		Assert.assertEquals("user1", accounting.get(0).getUser());
		Assert.assertEquals("requestingMember1", accounting.get(0).getRequestingMember());
		Assert.assertEquals("managerId", accounting.get(0).getProvidingMember());
		Assert.assertEquals(initialUsage1, accounting.get(0).getUsage(), ACCEPTABLE_ERROR);
		Assert.assertEquals(1, accounting.get(0).getCurrentInstances(), ACCEPTABLE_ERROR);
		Assert.assertEquals(10.0, accounting.get(0).getQuota(), ACCEPTABLE_ERROR);
	}
}

package org.fogbowcloud.manager.core.plugins.memberpicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.model.ResourcesInfo;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.accounting.AccountingInfo;
import org.fogbowcloud.manager.core.util.DefaultDataTestHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestRandomizedNoFMemberPicker {

	private AccountingPlugin accountingPlugin;
	private Properties properties;
	private ManagerController facade;

	@Before
	public void setUp(){
		properties = new Properties();
		properties.put(ConfigurationConstants.XMPP_JID_KEY,
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		properties.put("nof_trustworthy", "false");
		
		accountingPlugin = Mockito.mock(AccountingPlugin.class);
		
		facade = Mockito.mock(ManagerController.class);		
	}
	
	@Test
	public void testGetNumberBetweenMinAndMax() {		
		RandomizedNoFMemberPickerPlugin randNofPicker = new RandomizedNoFMemberPickerPlugin(properties, accountingPlugin);		
		Assert.assertEquals(0,randNofPicker.getNumberBetweenMinAndMax(0, 0));
		Assert.assertEquals(1,randNofPicker.getNumberBetweenMinAndMax(1, 1));
		for(int i = 0; i <= 1000000; i++){
			int drawNumber = randNofPicker.getNumberBetweenMinAndMax(1, 10);
			Assert.assertTrue(drawNumber>=1 && drawNumber<=10);
		}
	}
	
	@Test
	public void testChooseQuantiles() {
		RandomizedNoFMemberPickerPlugin randNofPicker = new RandomizedNoFMemberPickerPlugin(properties, accountingPlugin);
		
		double [] frequency = new double[]{0,0,0,0};
		for(int i = 0; i <= 10000000; i++){
			int choseQuantile = randNofPicker.chooseQuantile(randNofPicker.getQuantiles());
			Assert.assertTrue(choseQuantile>=1 && choseQuantile<=randNofPicker.getQuantiles());
			frequency[choseQuantile-1]++;
		}
		
		for(int i = 0; i < frequency.length; i++)
			System.out.println("Quantile ("+i+"): "+frequency[i]/10000000);
	}
	
	@Test
	public void testGetMemberSortByDebt() {
		// mocking facade
		FederationMember localMember = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL, "", "", "", "", "", ""));
		FederationMember remoteMember1 = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "1", "", "", "", "", "", ""));
		FederationMember remoteMember2 = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "2", "", "", "", "", "", ""));
		ArrayList<FederationMember> membersToReturn = new ArrayList<FederationMember>();
		membersToReturn.add(localMember);
		membersToReturn.add(remoteMember1);
		membersToReturn.add(remoteMember2);
		Mockito.when(facade.getRendezvousMembers()).thenReturn(membersToReturn);
		
		// mocking accounting	
		//debt of remote1: approx. 12
		//local consumed 16 from remote1
		List<AccountingInfo> accounting = new ArrayList<AccountingInfo>();
		AccountingInfo accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL,
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "1");
		accountingEntry.addConsumption(16);
		accounting.add(accountingEntry);
		
		//remote1 consumed 4 from local
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "1",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		accountingEntry.addConsumption(4);
		accounting.add(accountingEntry);
		
		//debt of remote2: approx. 6
		//local consumed 16 from remote2
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL,
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "2");
		accountingEntry.addConsumption(16);
		accounting.add(accountingEntry);
		
		//remote2 consumed 10 from local
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "2",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		accountingEntry.addConsumption(10);
		accounting.add(accountingEntry);
		
		Mockito.when(accountingPlugin.getAccountingInfo()).thenReturn(
				accounting);
		
		RandomizedNoFMemberPickerPlugin randNofPicker = new RandomizedNoFMemberPickerPlugin(properties, accountingPlugin);
		Assert.assertEquals(remoteMember1, randNofPicker.getMembersSortByDebt(facade.getRendezvousMembers()).get(0));
		
		FederationMember remoteMember3 = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "3", "", "", "", "", "", ""));
		membersToReturn.add(remoteMember3);
		Mockito.when(facade.getRendezvousMembers()).thenReturn(membersToReturn);
		
		//debt of remote3: approx. 19
		//local consumed 20 from to remote3
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL,
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "3");
		accountingEntry.addConsumption(20);
		accounting.add(accountingEntry);
		
		//remote3 consumed 1 from local
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "3",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		accountingEntry.addConsumption(1);
		accounting.add(accountingEntry);
		
		Mockito.when(accountingPlugin.getAccountingInfo()).thenReturn(
				accounting);
		Assert.assertEquals(remoteMember3, randNofPicker.getMembersSortByDebt(facade.getRendezvousMembers()).get(0));
		Assert.assertEquals(remoteMember1, randNofPicker.getMembersSortByDebt(facade.getRendezvousMembers()).get(1));
	}
	
	//TODO
	@Test
	public void testPickZeroMember(){}
	
	//TODO
	@Test
	public void testPickOneMember(){}
	
	//TODO
	@Test
	public void testPickTwoMembers(){}

	//TODO
	@Test
	public void testPickFourMember(){}

	//TODO
	@Test
	public void testPickTenMembers(){}

	
	/*@Test
	public void testEmptyMembers() {
		// mocking
		Mockito.when(facade.getRendezvousMembers()).thenReturn(new ArrayList<FederationMember>());
		Mockito.when(accountingPlugin.getAccountingInfo()).thenReturn(
				new ArrayList<AccountingInfo>());
		
		NoFMemberPickerPlugin nofPicker = new NoFMemberPickerPlugin(properties, accountingPlugin);
		Assert.assertNull(nofPicker.pick(facade.getRendezvousMembers()));
		Assert.assertFalse(nofPicker.getTrustworthy());
	}

	@Test
	public void testOnlyLocalMember() {
		// mocking
		FederationMember localMember = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL, "", "", "", "", "", ""));
		ArrayList<FederationMember> membersToReturn = new ArrayList<FederationMember>();
		membersToReturn.add(localMember);
		
		Mockito.when(facade.getRendezvousMembers()).thenReturn(membersToReturn);
		Mockito.when(accountingPlugin.getAccountingInfo()).thenReturn(
				new ArrayList<AccountingInfo>());
		
		NoFMemberPickerPlugin nofPicker = new NoFMemberPickerPlugin(properties, accountingPlugin);
		Assert.assertNull(nofPicker.pick(facade.getRendezvousMembers()));
		Assert.assertFalse(nofPicker.getTrustworthy());
	}
	
	@Test
	public void testOneRemoteMember() {
		// mocking facade
		FederationMember localMember = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL, "", "", "", "", "", ""));
		FederationMember remoteMember = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL, "", "", "", "", "", ""));
		ArrayList<FederationMember> membersToReturn = new ArrayList<FederationMember>();
		membersToReturn.add(localMember);
		membersToReturn.add(remoteMember);
		Mockito.when(facade.getRendezvousMembers()).thenReturn(membersToReturn);
		
		// mocking accounting		
		List<AccountingInfo> accounting = new ArrayList<AccountingInfo>();
		AccountingInfo accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL,
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL);
		accountingEntry.addConsumption(4);
		accounting.add(accountingEntry);
		
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL,
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		accountingEntry.addConsumption(16);
		accounting.add(accountingEntry);
		
		Mockito.when(accountingPlugin.getAccountingInfo()).thenReturn(
				accounting);
		
		NoFMemberPickerPlugin nofPicker = new NoFMemberPickerPlugin(properties, accountingPlugin);
		Assert.assertEquals(remoteMember, nofPicker.pick(facade.getRendezvousMembers()));
		Assert.assertFalse(nofPicker.getTrustworthy());
	}
	
	@Test
	public void testTwoRemoteMembers() {
		// mocking facade
		FederationMember localMember = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL, "", "", "", "", "", ""));
		FederationMember remoteMember1 = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "1", "", "", "", "", "", ""));
		FederationMember remoteMember2 = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "2", "", "", "", "", "", ""));
		ArrayList<FederationMember> membersToReturn = new ArrayList<FederationMember>();
		membersToReturn.add(localMember);
		membersToReturn.add(remoteMember1);
		membersToReturn.add(remoteMember2);
		Mockito.when(facade.getRendezvousMembers()).thenReturn(membersToReturn);
		
		// mocking accounting		
		List<AccountingInfo> accounting = new ArrayList<AccountingInfo>();
		AccountingInfo accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL,
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "1");
		accountingEntry.addConsumption(4);
		accounting.add(accountingEntry);
		
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "1",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		accountingEntry.addConsumption(16);
		accounting.add(accountingEntry);
		
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL,
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "2");
		accountingEntry.addConsumption(10);
		accounting.add(accountingEntry);
		
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "2",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		accountingEntry.addConsumption(16);
		accounting.add(accountingEntry);
		
		Mockito.when(accountingPlugin.getAccountingInfo()).thenReturn(
				accounting);
		
		NoFMemberPickerPlugin nofPicker = new NoFMemberPickerPlugin(properties, accountingPlugin);
		Assert.assertEquals(remoteMember1, nofPicker.pick(facade.getRendezvousMembers()));
		Assert.assertEquals(remoteMember1, nofPicker.pick(facade.getRendezvousMembers()));
		Assert.assertEquals(remoteMember1, nofPicker.pick(facade.getRendezvousMembers()));
		Assert.assertEquals(remoteMember1, nofPicker.pick(facade.getRendezvousMembers()));
		Assert.assertEquals(remoteMember1, nofPicker.pick(facade.getRendezvousMembers()));
		Assert.assertFalse(nofPicker.getTrustworthy());
	}*/
}

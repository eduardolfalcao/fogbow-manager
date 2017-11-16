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
	
	@Test
	public void testPickZeroMember(){
		// mocking facade
		FederationMember localMember = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL, "", "", "", "", "", ""));
		ArrayList<FederationMember> membersToReturn = new ArrayList<FederationMember>();
		membersToReturn.add(localMember);
		Mockito.when(facade.getRendezvousMembers()).thenReturn(membersToReturn);
		
		List<AccountingInfo> accounting = new ArrayList<AccountingInfo>();
		
		Mockito.when(accountingPlugin.getAccountingInfo()).thenReturn(
				accounting);
		
		RandomizedNoFMemberPickerPlugin randNofPicker = new RandomizedNoFMemberPickerPlugin(properties, accountingPlugin);
		Assert.assertNull(randNofPicker.pick(facade.getRendezvousMembers()));
		
	}
	
	@Test
	public void testPickOneMember(){
		// mocking facade
		FederationMember localMember = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL, "", "", "", "", "", ""));
		FederationMember remoteMember1 = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "1", "", "", "", "", "", ""));
		ArrayList<FederationMember> membersToReturn = new ArrayList<FederationMember>();
		membersToReturn.add(localMember);
		membersToReturn.add(remoteMember1);
		Mockito.when(facade.getRendezvousMembers()).thenReturn(membersToReturn);
				
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
				
		Mockito.when(accountingPlugin.getAccountingInfo()).thenReturn(
				accounting);
				
		RandomizedNoFMemberPickerPlugin randNofPicker = new RandomizedNoFMemberPickerPlugin(properties, accountingPlugin);
		Assert.assertEquals(remoteMember1,randNofPicker.pick(facade.getRendezvousMembers()));
	}
	
	@Test
	public void testPickTwoMembers(){
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
		
		double [] frequency = new double[]{0,0};
		int executions = 100000;
		for(int i = 0; i <= executions; i++){
			FederationMember chosenMember = randNofPicker.pick(facade.getRendezvousMembers());
			if(chosenMember==remoteMember1)
				frequency[0]++;
			else if(chosenMember==remoteMember2)
				frequency[1]++;
			
			Assert.assertTrue(chosenMember==remoteMember1 || chosenMember==remoteMember2);
		}
		
		for(int i = 0; i < frequency.length; i++)
			System.out.println("Frequency of chosing member "+(i+1)+"): "+frequency[i]/executions);
	}

	@Test
	public void testPickFourMember(){
		// mocking facade
		FederationMember localMember = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL, "", "", "", "", "", ""));
		FederationMember remoteMember1 = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "1", "", "", "", "", "", ""));
		FederationMember remoteMember2 = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "2", "", "", "", "", "", ""));
		FederationMember remoteMember3 = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "3", "", "", "", "", "", ""));
		FederationMember remoteMember4 = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "4", "", "", "", "", "", ""));
		
		ArrayList<FederationMember> membersToReturn = new ArrayList<FederationMember>();
		membersToReturn.add(localMember);
		membersToReturn.add(remoteMember1);
		membersToReturn.add(remoteMember2);
		membersToReturn.add(remoteMember3);
		membersToReturn.add(remoteMember4);
		Mockito.when(facade.getRendezvousMembers()).thenReturn(membersToReturn);
						
		List<AccountingInfo> accounting = new ArrayList<AccountingInfo>();
		
		AccountingInfo accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL,
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "1");
		accountingEntry.addConsumption(16);
		accounting.add(accountingEntry);
				
		//remote1 debt: 12
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "1",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		accountingEntry.addConsumption(4);
		accounting.add(accountingEntry);
		
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL,
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "2");
		accountingEntry.addConsumption(16);
		accounting.add(accountingEntry);
		
		//remote2 debt: 6
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "2",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		accountingEntry.addConsumption(10);
		accounting.add(accountingEntry);
		
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL,
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "3");
		accountingEntry.addConsumption(25);
		accounting.add(accountingEntry);
		
		//remote3 debt: 20
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "3",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		accountingEntry.addConsumption(5);
		accounting.add(accountingEntry);
		
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL,
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "4");
		accountingEntry.addConsumption(20);
		accounting.add(accountingEntry);
		
		//remote4 debt: 0
		accountingEntry = new AccountingInfo("user",
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + "4",
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		accountingEntry.addConsumption(20);
		accounting.add(accountingEntry);
						
		Mockito.when(accountingPlugin.getAccountingInfo()).thenReturn(
				accounting);
						
		RandomizedNoFMemberPickerPlugin randNofPicker = new RandomizedNoFMemberPickerPlugin(properties, accountingPlugin);
		
		double [] frequency = new double[]{0,0,0,0};
		int executions = 100000;
		for(int i = 0; i <= executions; i++){
			FederationMember chosenMember = randNofPicker.pick(facade.getRendezvousMembers());
			if(chosenMember==remoteMember1)
				frequency[0]++;
			else if(chosenMember==remoteMember2)
				frequency[1]++;
			else if(chosenMember==remoteMember3)
				frequency[2]++;
			else if(chosenMember==remoteMember4)
				frequency[3]++;
			
			Assert.assertTrue(chosenMember==remoteMember1 || chosenMember==remoteMember2
					|| chosenMember==remoteMember3 || chosenMember==remoteMember4);
		}
		
		//remote3:40%
		//remote1:30%
		//remote2:20%
		//remote4:10%
		for(int i = 0; i < frequency.length; i++)
			System.out.println("Frequency of chosing member "+(i+1)+"): "+frequency[i]/executions);
	}

	@Test
	public void testPickSevenMembers(){
		// mocking facade
		FederationMember localMember = new FederationMember(new ResourcesInfo(
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL, "", "", "", "", "", ""));
		
		ArrayList<FederationMember> membersToReturn = new ArrayList<FederationMember>();
		membersToReturn.add(localMember);
		
		FederationMember remoteMembers[] = new FederationMember[7];
		for(int i = 0; i < remoteMembers.length; i++){
			remoteMembers[i] = new FederationMember(new ResourcesInfo(
					DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL + (i+1), "", "", "", "", "", ""));
			membersToReturn.add(remoteMembers[i]);
		}
		Mockito.when(facade.getRendezvousMembers()).thenReturn(membersToReturn);
						
		List<AccountingInfo> accounting = new ArrayList<AccountingInfo>();
		
		int [] localConsumed = {100,100,100,100,100,100,100};
		int [] localDonated = {100,90,80,70,60,50,40};
		for(int i = 0; i < remoteMembers.length; i++){
			AccountingInfo accountingEntry = new AccountingInfo("user",
					DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL,
					remoteMembers[i].getId());
			accountingEntry.addConsumption(localConsumed[i]);
			accounting.add(accountingEntry);

			accountingEntry = new AccountingInfo("user",
					remoteMembers[i].getId(),
					DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
			accountingEntry.addConsumption(localDonated[i]);
			accounting.add(accountingEntry);
		}
						
		Mockito.when(accountingPlugin.getAccountingInfo()).thenReturn(
				accounting);
						
		RandomizedNoFMemberPickerPlugin randNofPicker = new RandomizedNoFMemberPickerPlugin(properties, accountingPlugin);
		
		double [] frequency = new double[]{0,0,0,0,0,0,0};
		int executions = 100000;
		for(int i = 0; i <= executions; i++){
			FederationMember chosenMember = randNofPicker.pick(facade.getRendezvousMembers());
			if(chosenMember==remoteMembers[0])
				frequency[0]++;
			else if(chosenMember==remoteMembers[1])
				frequency[1]++;
			else if(chosenMember==remoteMembers[2])
				frequency[2]++;
			else if(chosenMember==remoteMembers[3])
				frequency[3]++;
			else if(chosenMember==remoteMembers[4])
				frequency[4]++;
			else if(chosenMember==remoteMembers[5])
				frequency[5]++;
			else if(chosenMember==remoteMembers[6])
				frequency[6]++;
			
			Assert.assertTrue(chosenMember==remoteMembers[0] || chosenMember==remoteMembers[1]
					|| chosenMember==remoteMembers[2] || chosenMember==remoteMembers[3]
					|| chosenMember==remoteMembers[4] || chosenMember==remoteMembers[5]
					|| chosenMember==remoteMembers[6]);
		}
		
		//remote1:2.5%
		//remote2:2.5%
		//remote3:2.5%
		//remote4:2.5%
		//remote5:20%
		//remote6:30%
		//remote7:40%
		for(int i = 0; i < frequency.length; i++)
			System.out.println("Frequency of chosing member "+(i+1)+"): "+frequency[i]/executions);
	}

}

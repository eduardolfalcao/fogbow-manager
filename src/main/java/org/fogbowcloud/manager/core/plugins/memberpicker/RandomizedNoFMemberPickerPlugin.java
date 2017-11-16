package org.fogbowcloud.manager.core.plugins.memberpicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.FederationMemberPickerPlugin;
import org.fogbowcloud.manager.core.plugins.accounting.AccountingInfo;
import org.fogbowcloud.manager.core.plugins.accounting.ResourceUsage;
import org.fogbowcloud.manager.core.plugins.prioritization.nof.FederationMemberDebt;
import org.fogbowcloud.manager.core.plugins.prioritization.nof.NoFHelper;

public class RandomizedNoFMemberPickerPlugin implements FederationMemberPickerPlugin {
		
	private AccountingPlugin accoutingPlugin;
	private String localMemberId;
	private boolean trustworthy = false;
	
	private int quantiles;

	private static final Logger LOGGER = Logger.getLogger(RandomizedNoFMemberPickerPlugin.class);
	
	public RandomizedNoFMemberPickerPlugin(Properties properties, AccountingPlugin accoutingPlugin) {		
		this.accoutingPlugin = accoutingPlugin;
		this.localMemberId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
		try {
			this.trustworthy = Boolean.valueOf(properties.getProperty("nof_trustworthy"));			
		} catch (Exception e) {
			LOGGER.error("Error while getting boolean value for nof_trustworhty. The default value is false.", e);
		}
		try {
			this.quantiles = Integer.valueOf(properties.getProperty("quantiles"));			
		} catch (Exception e) {
			LOGGER.error("Error while getting int value for qunatiles. The default value is 4.", e);
			this.quantiles = 4;
		}
	}
	
	@Override
	public FederationMember pick(List<FederationMember> members) {
		
		int actualQuantiles = quantiles;
		if(members.size()<quantiles){
			actualQuantiles = members.size();
			for(FederationMember m : members){
				if(m.getId().equals(localMemberId))
					actualQuantiles--;
			}
		}
		
		if(actualQuantiles<1)
			return null;
		
		int chosenQuantile = chooseQuantile(actualQuantiles);
		
		List<FederationMember> sortedMembers =  getMembersSortByDebt(members);
		
		if(sortedMembers==null)
			return null;
		
		int numPeersInterval = sortedMembers.size()/actualQuantiles;
		int min = (chosenQuantile-1)*numPeersInterval;
		int max;
		if(chosenQuantile==actualQuantiles)
			max = sortedMembers.size()-1;
		else
			 max = (chosenQuantile*numPeersInterval)-1;
		
		int drawnPeer = getNumberBetweenMinAndMax(min, max);
		
		return sortedMembers.get(drawnPeer);
	}
	
	protected List<FederationMember>  getMembersSortByDebt(List<FederationMember> members){
		List<AccountingInfo> accounting = accoutingPlugin.getAccountingInfo();
		Map<String, ResourceUsage> membersUsage = NoFHelper.calculateMembersUsage(localMemberId,
				accounting);
		LinkedList<FederationMemberDebt> reputableMembers = new LinkedList<FederationMemberDebt>();

		for (FederationMember currentMember : members) {				
			String memberId = currentMember.getId();
			if (localMemberId.equals(memberId)) {
				continue;
			}
			
			double debt = 0d;
			if (membersUsage.containsKey(memberId)) {
				debt = membersUsage.get(memberId).getConsumed()
						- membersUsage.get(memberId).getDonated();
				if (!trustworthy) {
					debt = Math.max(0,
							debt + Math.sqrt(membersUsage.get(memberId).getDonated()));
				}
			}
			reputableMembers.add(new FederationMemberDebt(currentMember, debt));
		}
		
		if (reputableMembers.isEmpty()) {
			return null;
		}
		
		Collections.sort(reputableMembers, new FederationMemberDescendingDebtComparator());
		
		List<FederationMember> sortedMembers = new ArrayList<FederationMember>();
		for(FederationMemberDebt m : reputableMembers){
			sortedMembers.add(m.getMember());
		}
		
		return sortedMembers;
	}
	
	protected int chooseQuantile(int actualQuantiles){
		//sortear um número entre 1 somatório(1:actualQuantiles)
		int max = 0;
		for(int i = 1; i <= actualQuantiles;i++)
			max += i;
		
		int drawNumber = getNumberBetweenMinAndMax(1, max);
		
		int choosedQuantile = 1;
		
		int intervalMax = max, intervalMin = max - actualQuantiles;
		while(!(drawNumber<=intervalMax && drawNumber>intervalMin)){
			choosedQuantile++;
			intervalMax = intervalMin;
			actualQuantiles--;
			intervalMin -= actualQuantiles;
		}
		
		return choosedQuantile;
	}
	
	protected int getNumberBetweenMinAndMax(int min, int max){
		return new Random().nextInt(max - min + 1) + min;
	}
	
	public boolean getTrustworthy() {
		return trustworthy;
	}
	
	public int getQuantiles() {
		return quantiles;
	}
	
	private void setQu() {
		// TODO Auto-generated method stub

	}
}


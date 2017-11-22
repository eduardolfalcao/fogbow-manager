package org.fogbowcloud.manager.core.plugins.memberpicker;

import java.util.Comparator;

import org.fogbowcloud.manager.core.plugins.prioritization.nof.FederationMemberCredit;

public class FederationMemberDescendingDebtComparator implements Comparator<FederationMemberCredit> {
	@Override
	public int compare(FederationMemberCredit firstMemberDebt,
			FederationMemberCredit secondMemberDebt) {
		
		return new Double(secondMemberDebt.getCredit()).compareTo(new Double(firstMemberDebt.getCredit()));
	}
}
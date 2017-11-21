package org.fogbowcloud.manager.core.plugins.memberpicker;

import java.util.Comparator;

import org.fogbowcloud.manager.core.plugins.prioritization.nof.FederationMemberDebt;

public class FederationMemberDescendingDebtComparator implements Comparator<FederationMemberDebt> {
	@Override
	public int compare(FederationMemberDebt firstMemberDebt,
			FederationMemberDebt secondMemberDebt) {
		
		return new Double(secondMemberDebt.getDebt()).compareTo(new Double(firstMemberDebt.getDebt()));
	}
}
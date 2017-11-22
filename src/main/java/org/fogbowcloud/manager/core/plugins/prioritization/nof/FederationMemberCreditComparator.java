package org.fogbowcloud.manager.core.plugins.prioritization.nof;

import java.util.Comparator;

public class FederationMemberCreditComparator implements Comparator<FederationMemberCredit> {
	@Override
	public int compare(FederationMemberCredit firstMemberDebt,
			FederationMemberCredit secondMemberDebt) {
		
		return new Double(firstMemberDebt.getCredit()).compareTo(new Double(
				secondMemberDebt.getCredit()));
	}
}
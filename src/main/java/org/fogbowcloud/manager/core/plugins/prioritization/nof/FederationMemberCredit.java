package org.fogbowcloud.manager.core.plugins.prioritization.nof;

import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.model.ResourcesInfo;

public class FederationMemberCredit {

	private FederationMember member;
	private double credit;

	public FederationMemberCredit(String memberId, double debt) {
		this(new FederationMember(new ResourcesInfo(memberId, "", "", "", "", "", "")), debt);
	}

	public FederationMemberCredit(FederationMember member, double credit) {
		this.member = member;
		this.credit = credit;
	}

	public FederationMember getMember() {
		return member;
	}

	public double getCredit() {
		return credit;
	}
	
	public String toString() {
		return "memberId=" + member.getResourcesInfo().getId() + ", credit=" + credit;
	}
}
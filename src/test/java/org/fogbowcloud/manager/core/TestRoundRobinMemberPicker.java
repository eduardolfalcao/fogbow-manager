package org.fogbowcloud.manager.core;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.model.ResourcesInfo;
import org.junit.Assert;
import org.junit.Test;

public class TestRoundRobinMemberPicker {

	@Test
	public void testListMembersNull() {
		RoundRobinMemberPicker roundRobinMemberPicker = new RoundRobinMemberPicker();
		Assert.assertNull(roundRobinMemberPicker.pick(null));
		
	}
	
	@Test
	public void testPick() {
		RoundRobinMemberPicker roundRobinMemberPicker = new RoundRobinMemberPicker();
		List<FederationMember> members = new ArrayList<FederationMember>();
		ResourcesInfo resourcesInfoD = new ResourcesInfo("d", "", "", "", "", null, null);
		ResourcesInfo resourcesInfoA = new ResourcesInfo("a", "", "", "", "", null, null);
		ResourcesInfo resourcesInfoCA = new ResourcesInfo("ca", "", "", "", "", null, null);
		ResourcesInfo resourcesInfoCB = new ResourcesInfo("cb", "", "", "", "", null, null);
		ResourcesInfo resourcesInfoB = new ResourcesInfo("b", "", "", "", "", null, null);
		members.add(new FederationMember(resourcesInfoA));
		members.add(new FederationMember(resourcesInfoCB));
		members.add(new FederationMember(resourcesInfoCA));
		members.add(new FederationMember(resourcesInfoD));
		members.add(new FederationMember(resourcesInfoB));

		FederationMember federationMember = roundRobinMemberPicker.pick(members);
		Assert.assertEquals("a", federationMember.getResourcesInfo().getId());

		federationMember = roundRobinMemberPicker.pick(members);
		Assert.assertEquals("b", federationMember.getResourcesInfo().getId());

		federationMember = roundRobinMemberPicker.pick(members);
		Assert.assertEquals("ca", federationMember.getResourcesInfo().getId());

		members.clear();
		members.add(new FederationMember(resourcesInfoB));
		members.add(new FederationMember(resourcesInfoCA));

		federationMember = roundRobinMemberPicker.pick(members);
		Assert.assertEquals("b", federationMember.getResourcesInfo().getId());

		members.clear();
		members.add(new FederationMember(resourcesInfoA));
		members.add(new FederationMember(resourcesInfoCB));
		members.add(new FederationMember(resourcesInfoCA));
		members.add(new FederationMember(resourcesInfoD));
		members.add(new FederationMember(resourcesInfoB));

		federationMember = roundRobinMemberPicker.pick(members);
		Assert.assertEquals("ca", federationMember.getResourcesInfo().getId());

		federationMember = roundRobinMemberPicker.pick(members);
		Assert.assertEquals("cb", federationMember.getResourcesInfo().getId());

		federationMember = roundRobinMemberPicker.pick(members);
		Assert.assertEquals("d", federationMember.getResourcesInfo().getId());

		federationMember = roundRobinMemberPicker.pick(members);
		Assert.assertEquals("a", federationMember.getResourcesInfo().getId());
	}

	@Test
	public void testPickListMemberWithoutLastMember() {
		RoundRobinMemberPicker roundRobinMemberPicker = new RoundRobinMemberPicker();
		List<FederationMember> members = new ArrayList<FederationMember>();
		ResourcesInfo resourcesInfoD = new ResourcesInfo("d", "", "", "", "", null, null);
		ResourcesInfo resourcesInfoA = new ResourcesInfo("a", "", "", "", "", null, null);
		ResourcesInfo resourcesInfoCA = new ResourcesInfo("ca", "", "", "", "", null, null);
		ResourcesInfo resourcesInfoCB = new ResourcesInfo("cb", "", "", "", "", null, null);
		ResourcesInfo resourcesInfoB = new ResourcesInfo("b", "", "", "", "", null, null);
		members.add(new FederationMember(resourcesInfoA));
		members.add(new FederationMember(resourcesInfoCB));
		members.add(new FederationMember(resourcesInfoCA));
		members.add(new FederationMember(resourcesInfoD));
		members.add(new FederationMember(resourcesInfoB));

		FederationMember federationMember = roundRobinMemberPicker.pick(members);
		Assert.assertEquals("a", federationMember.getResourcesInfo().getId());

		federationMember = roundRobinMemberPicker.pick(members);
		Assert.assertEquals("b", federationMember.getResourcesInfo().getId());

		members.clear();
		members.add(new FederationMember(resourcesInfoA));
		members.add(new FederationMember(resourcesInfoD));

		federationMember = roundRobinMemberPicker.pick(members);
		Assert.assertEquals("d", federationMember.getResourcesInfo().getId());
	}

}

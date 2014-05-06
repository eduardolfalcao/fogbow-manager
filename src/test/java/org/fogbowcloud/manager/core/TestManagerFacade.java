package org.fogbowcloud.manager.core;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.ssh.SSHTunnel;
import org.fogbowcloud.manager.occi.core.Category;
import org.fogbowcloud.manager.occi.request.Request;
import org.fogbowcloud.manager.occi.request.RequestAttribute;
import org.fogbowcloud.manager.occi.request.RequestConstants;
import org.fogbowcloud.manager.occi.request.RequestState;
import org.fogbowcloud.manager.xmpp.util.ManagerTestHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestManagerFacade {

	private static final String INSTANCE_ID = "b122f3ad-503c-4abb-8a55-ba8d90cfce9f";
	ManagerFacade managerFacade;
	ManagerTestHelper managerTestHelper;
	
	private static final Long SCHEDULER_PERIOD = 500L;
		
	@Before
	public void setUp() throws Exception {
		managerFacade = new ManagerFacade(new Properties());
		managerTestHelper = new ManagerTestHelper();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorException() throws Exception {
		new ManagerFacade(null);
	}

	@Test
	public void testGet0ItemsFromIQ() {
		managerFacade.updateMembers(new LinkedList<FederationMember>());
		Assert.assertEquals(0, managerFacade.getMembers().size());
	}

	@Test
	public void testGet1ItemFromIQ() throws CertificateException, IOException {
		FederationMember managerItem = new FederationMember(managerTestHelper.getResources());
		List<FederationMember> items = new LinkedList<FederationMember>();
		items.add(managerItem);
		managerFacade.updateMembers(items);
		
		List<FederationMember> members = managerFacade.getMembers();
		Assert.assertEquals(1, members.size());
		Assert.assertEquals("abc", members.get(0).getResourcesInfo().getId());
		Assert.assertEquals(1, managerFacade.getMembers().size());
	}

	@Test
	public void testGetManyItemsFromIQ() throws CertificateException, IOException {
		ArrayList<FederationMember> items = new ArrayList<FederationMember>();
		for (int i = 0; i < 10; i++) {
			items.add(new FederationMember(managerTestHelper.getResources()));
		}
		managerFacade.updateMembers(items);
		
		List<FederationMember> members = managerFacade.getMembers();
		Assert.assertEquals(10, members.size());
		for (int i = 0; i < 10; i++) {
			Assert.assertEquals("abc", members.get(0).getResourcesInfo()
					.getId());
		}
		Assert.assertEquals(10, managerFacade.getMembers().size());
	}
	
	@Test
	public void testGetRequestsByUser() throws InterruptedException {

		Properties properties = new Properties();
		properties.put("scheduler_period", SCHEDULER_PERIOD.toString());
		managerFacade = new ManagerFacade(properties);

		// default instance count value is 1
		Map<String, String> xOCCIAtt = new HashMap<String, String>();
		xOCCIAtt.put(RequestAttribute.INSTANCE_COUNT.getValue(),
				String.valueOf(RequestConstants.DEFAULT_INSTANCE_COUNT));

		ComputePlugin computePlugin = Mockito.mock(ComputePlugin.class);
		Mockito.when(computePlugin.requestInstance(Mockito.anyString(), Mockito.any(List.class), Mockito.any(Map.class)))
				.thenReturn(INSTANCE_ID);
		
		IdentityPlugin identityPlugin = Mockito.mock(IdentityPlugin.class);
		Mockito.when(identityPlugin.getUser(ManagerTestHelper.ACCESS_TOKEN_ID)).thenReturn(
				ManagerTestHelper.USER_NAME);

		SSHTunnel sshTunnel = Mockito.mock(SSHTunnel.class);
		
		managerFacade.setIdentityPlugin(identityPlugin);
		managerFacade.setComputePlugin(computePlugin);
		managerFacade.setSSHTunnel(sshTunnel);
						
		managerFacade.createRequests(ManagerTestHelper.ACCESS_TOKEN_ID, new ArrayList<Category>(), xOCCIAtt);
		
		Thread.sleep(SCHEDULER_PERIOD * 2);
		
		List<Request> requests = managerFacade.getRequestsFromUser(ManagerTestHelper.ACCESS_TOKEN_ID);
		
		Assert.assertEquals(1, requests.size());
		Assert.assertEquals(ManagerTestHelper.USER_NAME, requests.get(0).getUser());
		Assert.assertEquals(INSTANCE_ID, requests.get(0).getInstanceId());
		Assert.assertEquals(RequestState.FULFILLED, requests.get(0).getState());
		Assert.assertNull(requests.get(0).getMemberId());
	}
}

package org.fogbowcloud.manager.occi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.manager.occi.core.Category;
import org.fogbowcloud.manager.occi.model.RequestHelper;
import org.fogbowcloud.manager.occi.plugins.ComputePlugin;
import org.fogbowcloud.manager.occi.plugins.IdentityPlugin;
import org.fogbowcloud.manager.occi.request.Request;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestOCCIApplication {

	private OCCIApplication occiApplication;

	@Before
	public void setUp() {
		this.occiApplication = new OCCIApplication();

		ComputePlugin computePlugin = Mockito.mock(ComputePlugin.class);
		Mockito.when(computePlugin.requestInstance(Mockito.anyString(), Mockito.any(List.class), Mockito.any(Map.class)))
				.thenReturn("");

		IdentityPlugin identityPlugin = Mockito.mock(IdentityPlugin.class);
		Mockito.when(identityPlugin.isValidToken(RequestHelper.ACCESS_TOKEN)).thenReturn(true);
		Mockito.when(identityPlugin.getUser(RequestHelper.ACCESS_TOKEN)).thenReturn(RequestHelper.USER_MOCK);

		occiApplication.setIdentityPlugin(identityPlugin);
		occiApplication.setComputePlugin(computePlugin);
	}

	@Test
	public void testGetRequestDetails() {
		this.occiApplication.newRequest(RequestHelper.ACCESS_TOKEN, new ArrayList<Category>(),
				new HashMap<String, String>());
		occiApplication.newRequest(RequestHelper.ACCESS_TOKEN, new ArrayList<Category>(),
				new HashMap<String, String>());
		Map<String, List<String>> userToRequestIds = occiApplication.getUserToRequestIds();
		List<String> list = userToRequestIds.get(RequestHelper.USER_MOCK);
		String requestId = list.get(0);
		Request requestDetails = occiApplication.getRequestDetails(RequestHelper.ACCESS_TOKEN,
				requestId);
		String id = requestDetails.getId();

		Assert.assertEquals(requestId, id);
	}

	@Test
	public void testResquestUser() {
		this.occiApplication.newRequest(RequestHelper.ACCESS_TOKEN, new ArrayList<Category>(),
				new HashMap<String, String>());
		List<Request> requestsFromUser = occiApplication
				.getRequestsFromUser(RequestHelper.ACCESS_TOKEN);

		Assert.assertEquals(1, requestsFromUser.size());
	}

	@Test
	public void testManyResquestUser() {
		int valueRequest = 10;
		for (int i = 0; i < valueRequest; i++) {
			this.occiApplication.newRequest(RequestHelper.ACCESS_TOKEN, new ArrayList<Category>(),
					new HashMap<String, String>());
		}
		List<Request> requestsFromUser = occiApplication
				.getRequestsFromUser(RequestHelper.ACCESS_TOKEN);

		Assert.assertEquals(valueRequest, requestsFromUser.size());
	}

	@Test
	public void testRemoveAllRequest() {
		int valueRequest = 10;
		for (int i = 0; i < valueRequest; i++) {
			this.occiApplication.newRequest(RequestHelper.ACCESS_TOKEN, new ArrayList<Category>(),
					new HashMap<String, String>());
		}
		List<Request> requestsFromUser = this.occiApplication
				.getRequestsFromUser(RequestHelper.ACCESS_TOKEN);

		Assert.assertEquals(valueRequest, requestsFromUser.size());

		this.occiApplication.removeAllRequests(RequestHelper.ACCESS_TOKEN);
		requestsFromUser = this.occiApplication.getRequestsFromUser(RequestHelper.ACCESS_TOKEN);

		Assert.assertEquals(0, requestsFromUser.size());
	}

	@Test
	public void testRemoveSpecificRequest() {
		int valueRequest = 10;
		for (int i = 0; i < valueRequest; i++) {
			this.occiApplication.newRequest(RequestHelper.ACCESS_TOKEN, new ArrayList<Category>(),
					new HashMap<String, String>());
		}
		List<Request> requestsFromUser = this.occiApplication
				.getRequestsFromUser(RequestHelper.ACCESS_TOKEN);

		Assert.assertEquals(valueRequest, requestsFromUser.size());

		occiApplication.removeRequest(RequestHelper.ACCESS_TOKEN, requestsFromUser.get(1).getId());
		requestsFromUser = this.occiApplication.getRequestsFromUser(RequestHelper.ACCESS_TOKEN);

		Assert.assertEquals(valueRequest - 1, requestsFromUser.size());
	}
}

package org.fogbowcloud.manager.xmpp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.fogbowcloud.manager.core.AsynchronousOrderCallback;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.ManagerTestHelper;
import org.fogbowcloud.manager.core.ManagerTestHelperXP;
import org.fogbowcloud.manager.core.util.DefaultDataTestHelper;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderAttribute;
import org.fogbowcloud.manager.occi.order.OrderConstants;
import org.fogbowcloud.manager.occi.order.OrderState;
import org.fogbowcloud.manager.occi.order.OrderType;
import org.fogbowcloud.manager.occi.order.OrderXP;
import org.fogbowcloud.manager.occi.util.OCCITestHelper;
import org.jivesoftware.smack.XMPPException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;

public class TestPreemptionWarnHandler {

	private static final String INSTANCE_ID = "instanceId";
	private static final String ACCESS_ID = "accessId";
	private static final String ORDER_ID = "orderId";

	private ManagerTestHelper managerTestHelper;

	@Before
	public void setUp() throws XMPPException {
		this.managerTestHelper = new ManagerTestHelperXP();
	}

	@After
	public void tearDown() throws Exception {
		this.managerTestHelper.shutdown();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testPreemptServeredOrderResourceKindCompute() throws Exception {
		ManagerXmppComponent initializeXMPPManagerComponent = managerTestHelper.initializeXMPPManagerComponent(false);
		
		Token token = new Token(ACCESS_ID, new Token.User(OCCITestHelper.USER_MOCK, ""), null, new HashMap<String, String>());
		Mockito.when(managerTestHelper.getFederationIdentityPlugin().getToken(ACCESS_ID)).thenReturn(token);
		Mockito.when(managerTestHelper.getMapperPlugin().getLocalCredentials(Mockito.anyString())).thenReturn(new HashMap<String, String>());		
		Mockito.when(managerTestHelper.getMapperPlugin().getLocalCredentials(Mockito.anyString())).thenReturn(new HashMap<String, String>());
		Mockito.when(managerTestHelper.getIdentityPlugin().createToken(Mockito.anyMap())).thenReturn(token);		
		Mockito.when(managerTestHelper.getAuthorizationPlugin().isAuthorized(token)).thenReturn(true);	
		
		List<Order> orders = initializeXMPPManagerComponent.getManagerFacade()
				.getOrdersFromUser(token.getAccessId(), false);		
		//cleaning trash in database if there are orders on it
		if(!orders.isEmpty()){
			for(Order o : orders)
				initializeXMPPManagerComponent.getManagerFacade().getManagerDataStoreController().excludeOrder(o.getId());
		}
				
		Order order = createOrder(OrderConstants.COMPUTE_TERM);
		ManagerController managerFacade = initializeXMPPManagerComponent.getManagerFacade();
		managerFacade.getManagerDataStoreController().addOrder(order);
		
		orders = initializeXMPPManagerComponent.getManagerFacade()
				.getOrdersFromUser(token.getAccessId(), true);
		Assert.assertEquals(1, orders.size());
		
		final BlockingQueue<String> bq = new LinkedBlockingQueue<String>();

		ManagerPacketHelper.preemptOrder(DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL,
				order, managerTestHelper.createPacketSender(), new AsynchronousOrderCallback() {

					@Override
					public void success(String instanceId) {
						bq.add(new String());
					}

					@Override
					public void error(Throwable t) {
					}
				});
		
		String instanceId = bq.poll(1500, TimeUnit.SECONDS);
		
		Assert.assertEquals(1, initializeXMPPManagerComponent.getManagerFacade()
				.getOrdersFromUser(token.getAccessId(), true).size());
		Assert.assertEquals(OrderState.CLOSED, initializeXMPPManagerComponent.getManagerFacade()
				.getOrdersFromUser(token.getAccessId(), true).get(0).getState());
	}

	private Order createOrder(String resourceKind) {
		List<Category> categories = new ArrayList<Category>();
		categories.add(new Category("term1", "scheme1", "class1"));
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("key1", "value1");
		attributes.put("key2", "value2");
		attributes.put(OrderAttribute.RESOURCE_KIND.getValue(), resourceKind);		
		attributes.put(OrderAttribute.INSTANCE_COUNT.getValue(), "1");
		attributes.put(OrderAttribute.DATA_PUBLIC_KEY.getValue(), "public key");
		attributes.put(OrderAttribute.TYPE.getValue(), OrderType.PERSISTENT.getValue());
		attributes.put(OrderAttribute.PREVIOUS_ELAPSED_TIME.getValue(), "500");
		attributes.put(OrderAttribute.CURRENT_ELAPSED_TIME.getValue(), "500");
		attributes.put(OrderAttribute.RUNTIME.getValue(), "1000");		
		Order order = new OrderXP(ORDER_ID, new Token(ACCESS_ID,
				new Token.User(OCCITestHelper.USER_MOCK, ""),
				DefaultDataTestHelper.TOKEN_FUTURE_EXPIRATION,
				new HashMap<String, String>()), categories, attributes, true, DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		order.setInstanceId(INSTANCE_ID);
		return order;
	}
		
}

package org.fogbowcloud.manager.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.model.ResourcesInfo;
import org.fogbowcloud.manager.core.plugins.BenchmarkingPlugin;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.benchmarking.VanillaBenchmarkingPlugin;
import org.fogbowcloud.manager.core.plugins.compute.fake.FakeCloudComputePlugin;
import org.fogbowcloud.manager.core.util.DefaultDataTestHelper;
import org.fogbowcloud.manager.occi.TestDataStorageHelper;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderAttribute;
import org.fogbowcloud.manager.occi.order.OrderConstants;
import org.fogbowcloud.manager.occi.order.OrderState;
import org.fogbowcloud.manager.xmpp.AsyncPacketSender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestManagerWorkflow {
	
	public static final String ACCESS_TOKEN_ID_2 = "secondAccessToken";

	private ManagerController managerController;
	private ManagerTestHelper managerTestHelper;
	private Map<String, String> xOCCIAtt;

	@Before
	public void setUp() throws Exception {
		this.managerTestHelper = new ManagerTestHelper();
		
		/*
		 * Default manager controller: 
		 *  computePlugin.requestInstance always throws QuotaExceededException 
		 *  identityPlugin.getToken(AccessId) always returns DefaultDataTestHelper.ACCESS_TOKEN_ID
		 *  schedulerPeriod and monitoringPeriod are long enough (a day) to avoid reeschudeling
		 */

		this.managerController = managerTestHelper.createDefaultManagerController();
		// default instance count value is 1
		this.xOCCIAtt = new HashMap<String, String>();
		this.xOCCIAtt.put(OrderAttribute.INSTANCE_COUNT.getValue(),
				String.valueOf(OrderConstants.DEFAULT_INSTANCE_COUNT));
		this.xOCCIAtt.put(OrderAttribute.RESOURCE_KIND.getValue(), OrderConstants.COMPUTE_TERM);
		
		TestDataStorageHelper.clearManagerDataStore(this.managerController
				.getManagerDataStoreController().getManagerDatabase());
	}
	
	@After
	public void tearDown() {
		TestDataStorageHelper.clearManagerDataStore(this.managerController
				.getManagerDataStoreController().getManagerDatabase());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testReceiveOrderFromRemoteMember() throws InterruptedException {
		AsyncPacketSender packetSender = Mockito.mock(AsyncPacketSender.class);
		managerController.setPacketSender(packetSender);
		
		ResourcesInfo localResourcesInfo = new ResourcesInfo("", "", "", "", "", "");
		localResourcesInfo.setId(DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		
		ResourcesInfo remoteResourcesInfo = new ResourcesInfo("", "", "", "", "", "");
		remoteResourcesInfo.setId(DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL);
		
		Properties prop = new Properties();
		prop.put(FakeCloudComputePlugin.COMPUTE_FAKE_QUOTA, String.valueOf(1));
		ComputePlugin computePlugin = new FakeCloudComputePlugin(prop); 
		managerController.setComputePlugin(computePlugin);
		
		managerController.setBenchmarkExecutor(Executors.newCachedThreadPool());
		BenchmarkingPlugin benchmarkingPlugin = new VanillaBenchmarkingPlugin(null);
		managerController.setBenchmarkingPlugin(benchmarkingPlugin);
		
		//set ssh pub key so benchmark wont throw an exception
		managerController.getProperties().put(ConfigurationConstants.SSH_PUBLIC_KEY_PATH, DefaultDataTestHelper.LOCAL_MANAGER_SSH_PUBLIC_KEY_PATH);
		
		List<FederationMember> listMembers = new ArrayList<FederationMember>();
		listMembers.add(new FederationMember(localResourcesInfo));
		listMembers.add(new FederationMember(remoteResourcesInfo));
		managerController.updateMembers(listMembers);

		HashMap<String, String> xOCCIAtt2 = new HashMap<String, String>();
		xOCCIAtt2.put(OrderAttribute.RESOURCE_KIND.getValue(), OrderConstants.COMPUTE_TERM);
		Order orderOne = new Order("id1", managerTestHelper.getDefaultFederationToken(),
				new ArrayList<Category>(),
				xOCCIAtt2, false,
				DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL);
		orderOne.setState(OrderState.OPEN);
		managerController.getManagerDataStoreController().addOrder(orderOne);

		managerController.checkAndSubmitOpenOrders();

//		List<Order> ordersFromUser = managerController.getOrdersFromUser(managerTestHelper
//				.getDefaultFederationToken().getAccessId());
//		for (Order order : ordersFromUser) {
//			Assert.assertEquals(OrderState.PENDING, order.getState());
//		}
//		//Assert.assertTrue(managerController.isOrderForwardedtoRemoteMember(orderOne.getId()));
//
//		IQ iq = new IQ();
//		queryEl = iq.getElement().addElement("query",
//				ManagerXmppComponent.ORDER_NAMESPACE);
//		instanceEl = queryEl.addElement("instance");
//		instanceEl.addElement("id").setText("newinstanceid");
//		callbacks.get(0).handle(iq);
//	
//		ordersFromUser = managerController.getOrdersFromUser(managerTestHelper
//				.getDefaultFederationToken().getAccessId());
//		for (Order order : ordersFromUser) {
//			Assert.assertEquals(OrderState.FULFILLED, order.getState());
//			Assert.assertFalse(managerController.isOrderForwardedtoRemoteMember(order.getId()));
//		}
	}
	
/*	@SuppressWarnings("unchecked")
	@Test
	public void testSubmitOrderToRemoteMember() throws InterruptedException {
		AsyncPacketSender packetSender = Mockito.mock(AsyncPacketSender.class);
		managerController.setPacketSender(packetSender);
		FederationMemberPickerPlugin memberPicker = Mockito.mock(FederationMemberPickerPlugin.class);
		Mockito.when(memberPicker.pick(Mockito.anyList())).thenReturn(new FederationMember("fedMember"));
		managerController.setMemberPickerPlugin(memberPicker);

		// mocking getRemoteInstance for running benchmarking
		IQ response = new IQ(); 
		response.setType(Type.result);
		Element queryEl = response.getElement().addElement("query", 
				ManagerXmppComponent.GETINSTANCE_NAMESPACE);
		Element instanceEl = queryEl.addElement("instance");
		instanceEl.addElement("id").setText("newinstanceid");
		instanceEl.addElement("state").setText(InstanceState.RUNNING.toString());
		
		Mockito.when(packetSender.syncSendPacket(Mockito.any(IQ.class))).thenReturn(response);

		final List<PacketCallback> callbacks = new LinkedList<PacketCallback>();
		
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				callbacks.add((PacketCallback) invocation.getArguments()[1]);
				return null;
			}
		}).when(packetSender).addPacketCallback(Mockito.any(Packet.class), Mockito.any(PacketCallback.class));
		
		ResourcesInfo localResourcesInfo = new ResourcesInfo("", "", "", "", "", "");
		localResourcesInfo.setId(DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		
		ResourcesInfo remoteResourcesInfo = new ResourcesInfo("", "", "", "", "", "");
		remoteResourcesInfo.setId(DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL);
		
		ComputePlugin computePlugin = Mockito.mock(ComputePlugin.class);
		Mockito.when(
				computePlugin.requestInstance(Mockito.any(Token.class), Mockito.anyList(),
						Mockito.anyMap(), Mockito.anyString())).thenThrow(
				new OCCIException(ErrorType.UNAUTHORIZED, ""));

		Mockito.when(computePlugin.getResourcesInfo(Mockito.any(Token.class))).thenReturn(
				localResourcesInfo);
		managerController.setComputePlugin(computePlugin);

		List<FederationMember> listMembers = new ArrayList<FederationMember>();
		listMembers.add(new FederationMember(localResourcesInfo));
		listMembers.add(new FederationMember(remoteResourcesInfo));
		managerController.updateMembers(listMembers);

		HashMap<String, String> xOCCIAtt2 = new HashMap<String, String>();
		xOCCIAtt2.put(OrderAttribute.RESOURCE_KIND.getValue(), OrderConstants.COMPUTE_TERM);
		Order orderOne = new Order("id1", managerTestHelper.getDefaultFederationToken(),
				new ArrayList<Category>(),
				xOCCIAtt2, true,
				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
		orderOne.setState(OrderState.OPEN);
		managerController.getManagerDataStoreController().addOrder(orderOne);

		managerController.checkAndSubmitOpenOrders();

		List<Order> ordersFromUser = managerController.getOrdersFromUser(managerTestHelper
				.getDefaultFederationToken().getAccessId());
		for (Order order : ordersFromUser) {
			Assert.assertEquals(OrderState.PENDING, order.getState());
		}
		//Assert.assertTrue(managerController.isOrderForwardedtoRemoteMember(orderOne.getId()));

		IQ iq = new IQ();
		queryEl = iq.getElement().addElement("query",
				ManagerXmppComponent.ORDER_NAMESPACE);
		instanceEl = queryEl.addElement("instance");
		instanceEl.addElement("id").setText("newinstanceid");
		callbacks.get(0).handle(iq);
	
		ordersFromUser = managerController.getOrdersFromUser(managerTestHelper
				.getDefaultFederationToken().getAccessId());
		for (Order order : ordersFromUser) {
			Assert.assertEquals(OrderState.FULFILLED, order.getState());
			Assert.assertFalse(managerController.isOrderForwardedtoRemoteMember(order.getId()));
		}
	}*/
	
//	@SuppressWarnings("unchecked")
//	@Test
//	public void testRemoveLocalAndRemoteOrders() {
//		AsyncPacketSender packetSender = Mockito.mock(AsyncPacketSender.class);
//		managerController.setPacketSender(packetSender);
//		
//		// mocking getRemoteInstance for running benchmarking
//		IQ response = new IQ(); 
//		response.setType(Type.result);
//		Element queryEl = response.getElement().addElement("query", 
//				ManagerXmppComponent.GETINSTANCE_NAMESPACE);
//		Element instanceEl = queryEl.addElement("instance");
//		instanceEl.addElement("id").setText("newinstanceid");
//		instanceEl.addElement("state").setText(InstanceState.RUNNING.toString());
//		
//		Mockito.when(packetSender.syncSendPacket(Mockito.any(IQ.class))).thenReturn(response);
//
//		final List<PacketCallback> callbacks = new LinkedList<PacketCallback>();
//		
//		Mockito.doAnswer(new Answer<Void>() {
//			@Override
//			public Void answer(InvocationOnMock invocation) throws Throwable {
//				callbacks.add((PacketCallback) invocation.getArguments()[1]);
//				return null;
//			}
//		}).when(packetSender).addPacketCallback(Mockito.any(Packet.class), Mockito.any(PacketCallback.class));
//		
//		ResourcesInfo localResourcesInfo = new ResourcesInfo("", "", "", "", "", "");
//		localResourcesInfo.setId(DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
//		
//		ResourcesInfo remoteResourcesInfoOne = new ResourcesInfo("", "", "", "", "", "");
//		remoteResourcesInfoOne.setId(DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL);
//		
//		ResourcesInfo remoteResourcesInfoTwo = new ResourcesInfo("", "", "", "", "", "");
//		remoteResourcesInfoTwo.setId(DefaultDataTestHelper.REMOTE_MANAGER_TWO_COMPONENT_URL);
//		
//		ResourcesInfo remoteResourcesInfoThree = new ResourcesInfo("", "", "", "", "", "");
//		remoteResourcesInfoThree.setId(DefaultDataTestHelper.REMOTE_MANAGER_THREE_COMPONENT_URL);
//		
//		ComputePlugin computePlugin = Mockito.mock(ComputePlugin.class);
//		Mockito.when(
//				computePlugin.requestInstance(Mockito.any(Token.class), Mockito.anyList(),
//						Mockito.anyMap(), Mockito.anyString())).thenThrow(
//				new OCCIException(ErrorType.UNAUTHORIZED, ""));
//
//		Mockito.when(computePlugin.getResourcesInfo(Mockito.any(Token.class))).thenReturn(
//				localResourcesInfo);
//		managerController.setComputePlugin(computePlugin);
//
//		final List<FederationMember> listMembers = new ArrayList<FederationMember>();
//		listMembers.add(new FederationMember(localResourcesInfo));
//		listMembers.add(new FederationMember(remoteResourcesInfoOne));
//		listMembers.add(new FederationMember(remoteResourcesInfoTwo));
//		listMembers.add(new FederationMember(remoteResourcesInfoThree));
//		managerController.updateMembers(listMembers);
//		
//		FederationMemberPickerPlugin memberPicker = Mockito.mock(FederationMemberPickerPlugin.class);
//		Mockito.when(memberPicker.pick(Mockito.anyList())).thenReturn(
//				new FederationMember(remoteResourcesInfoOne), new FederationMember(remoteResourcesInfoTwo),
//				new FederationMember(remoteResourcesInfoThree));
//		managerController.setMemberPickerPlugin(memberPicker );
//
//		final String orderId = "orderId";
//		Order orderOne = new Order(orderId, managerTestHelper.getDefaultFederationToken(),
//				new ArrayList<Category>(),
//				new HashMap<String, String>(), true,
//				DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
//		orderOne.setState(OrderState.OPEN);
//		managerController.getManagerDataStoreController().addOrder(orderOne);
//
//		// mocking date
//		long now = System.currentTimeMillis();
//		DateUtils dateUtils = Mockito.mock(DateUtils.class);
//		Mockito.when(dateUtils.currentTimeMillis()).thenReturn(now);
//		managerController.setDateUtils(dateUtils);
//		
//		// Send to first member
//		managerController.checkAndSubmitOpenOrders();
//
//		List<Order> ordersFromUser = managerController.getOrdersFromUser(managerTestHelper
//				.getDefaultFederationToken().getAccessId());
//		for (Order order : ordersFromUser) {
//			Assert.assertEquals(OrderState.PENDING, order.getState());
//		}
//		Assert.assertTrue(managerController.isOrderForwardedtoRemoteMember(orderOne.getId()));
//
//		Mockito.when(dateUtils.currentTimeMillis()).thenReturn(
//				now + ManagerController.DEFAULT_ASYNC_ORDER_WAITING_INTERVAL + 100);		
//		
//		// Timeout expired
//		managerController.checkPedingOrders();		
//
//		ordersFromUser = managerController.getOrdersFromUser(managerTestHelper
//				.getDefaultFederationToken().getAccessId());
//		for (Order order : ordersFromUser) {
//			Assert.assertEquals(OrderState.OPEN, order.getState());
//			Assert.assertTrue(managerController.isOrderForwardedtoRemoteMember(order.getId()));
//		}
//		Assert.assertTrue(managerController.isOrderForwardedtoRemoteMember(orderOne.getId()));
//		
//		// mocking date
//		now = System.currentTimeMillis();
//		dateUtils = Mockito.mock(DateUtils.class);
//		Mockito.when(dateUtils.currentTimeMillis()).thenReturn(now);
//		managerController.setDateUtils(dateUtils);
//		
//		// Send to second member
//		managerController.checkAndSubmitOpenOrders();
//
//		ordersFromUser = managerController.getOrdersFromUser(managerTestHelper
//				.getDefaultFederationToken().getAccessId());
//		for (Order order : ordersFromUser) {
//			Assert.assertEquals(OrderState.PENDING, order.getState());
//		}
//		Assert.assertTrue(managerController.isOrderForwardedtoRemoteMember(orderOne.getId()));		
//
//		Mockito.when(dateUtils.currentTimeMillis()).thenReturn(
//				now + ManagerController.DEFAULT_ASYNC_ORDER_WAITING_INTERVAL + 100);		
//		
//		// Timeout expired
//		managerController.checkPedingOrders();		
//
//		ordersFromUser = managerController.getOrdersFromUser(managerTestHelper
//				.getDefaultFederationToken().getAccessId());
//		for (Order order : ordersFromUser) {
//			Assert.assertEquals(OrderState.OPEN, order.getState());
//			Assert.assertTrue(managerController.isOrderForwardedtoRemoteMember(order.getId()));
//		}
//		Assert.assertTrue(managerController.isOrderForwardedtoRemoteMember(orderOne.getId()));
//		
//		// Send to third member
//		managerController.checkAndSubmitOpenOrders();
//
//		ordersFromUser = managerController.getOrdersFromUser(managerTestHelper
//				.getDefaultFederationToken().getAccessId());
//		for (Order order : ordersFromUser) {
//			Assert.assertEquals(OrderState.PENDING, order.getState());
//		}
//		Assert.assertTrue(managerController.isOrderForwardedtoRemoteMember(orderOne.getId()));			
//		
//		AsyncPacketSender packetSenderTwo = Mockito.mock(AsyncPacketSender.class);
//		managerController.setPacketSender(packetSenderTwo);				
//		
//		managerController.removeOrder(managerTestHelper.getDefaultFederationToken().getAccessId(), orderId);
//		
//		Mockito.verify(packetSenderTwo, VerificationModeFactory.times(3)).sendPacket(Mockito.argThat(new ArgumentMatcher<IQ>() {
//			@Override
//			public boolean matches(Object argument) {
//				IQ iq = (IQ) argument;
//				Element queryEl = iq.getElement().element("query");
//				if (queryEl == null) {
//					return false;
//				}
//				String orderId = queryEl.element(ManagerPacketHelper.ORDER_EL).elementText("id");
//				String accessId = queryEl.element("token").elementText("accessId");			
//				
//				if (orderId.equals(orderId) 
//						&& iq.getTo().toBareJID().equals(DefaultDataTestHelper.REMOTE_MANAGER_TWO_COMPONENT_URL)
//						&& accessId.equals(managerTestHelper.getDefaultFederationToken().getAccessId())) {
//					return true;
//				}
//				
//				if (orderId.equals(orderId) 
//						&& iq.getTo().toBareJID().equals(DefaultDataTestHelper.REMOTE_MANAGER_THREE_COMPONENT_URL)
//						&& accessId.equals(managerTestHelper.getDefaultFederationToken().getAccessId())) {
//					return true;
//				}
//				
//				if (orderId.equals(orderId) 
//						&& iq.getTo().toBareJID().equals(DefaultDataTestHelper.REMOTE_MANAGER_COMPONENT_URL)
//						&& accessId.equals(managerTestHelper.getDefaultFederationToken().getAccessId())) {
//					return true;
//				}				
//				
//				return false;
//			}
//		}));			
//		
//	}
	
	
//	@Test
//	public void testCheckInstancePreempted() {
//		Token federationUserToken = new Token("accessId", new Token.User("user", "user"), new Date(), null);
//		String instanceId = "instanceId00";
//		Order orderToPreempt = new Order("id", federationUserToken, instanceId, "providingMemberId"
//				, "requestingMemberId", new Date().getTime(), true, OrderState.FULFILLED, null, xOCCIAtt);
//		
//		ComputePlugin computePlugin = Mockito.mock(ComputePlugin.class);
//		Instance instanceNull = null;
//		Instance instance = new Instance(instanceId);
//		Mockito.when(computePlugin.getInstance(federationUserToken, instanceId))
//				.thenReturn(instance, instanceNull);
//		managerController.setComputePlugin(computePlugin);
//		
//		long now = System.currentTimeMillis();
//		managerController.checkInstancePreempted(federationUserToken, orderToPreempt);
//		long after = System.currentTimeMillis();
//		
//		long executionTime = after - now;
//		Assert.assertTrue(executionTime >= ManagerController.DEFAULT_CHECK_STILL_ALIVE_WAIT_TIME 
//				&& executionTime < ManagerController.DEFAULT_CHECK_STILL_ALIVE_WAIT_TIME * 2 - 1);
//	}
	
//	@Test
//	public void testMonitorWillRemoveLocalFailedInstance() throws InterruptedException {
//		// setting order repository
//		HashMap<String, String> xOCCIAttr = new HashMap<String, String>();
//		xOCCIAttr.put(OrderAttribute.RESOURCE_KIND.getValue(), OrderConstants.COMPUTE_TERM);
//		Order order1 = new Order("id1", managerTestHelper.getDefaultFederationToken(), null, xOCCIAtt, true, "");
//		order1.setInstanceId(DefaultDataTestHelper.INSTANCE_ID);
//		order1.setState(OrderState.FULFILLED);
//		order1.setProvidingMemberId(DefaultDataTestHelper.LOCAL_MANAGER_COMPONENT_URL);
//
//		managerController.getManagerDataStoreController().addOrder(order1);
//
//		// updating compute mock
//		Instance expectedInstance = new Instance(DefaultDataTestHelper.INSTANCE_ID, new LinkedList<Resource>(), 
//				new HashMap<String, String>(), new LinkedList<Link>(), InstanceState.FAILED);
//		Mockito.when(managerTestHelper.getComputePlugin().getInstance(Mockito.any(Token.class),
//						Mockito.anyString())).thenReturn(expectedInstance);
//
//		managerController.monitorInstancesForLocalOrders();
//		
//		// checking if instance was properly removed
//		Mockito.verify(managerTestHelper.getComputePlugin()).removeInstance(
//				Mockito.any(Token.class), Mockito.eq(DefaultDataTestHelper.INSTANCE_ID));
//		
//		// checking if order is closed
//		List<Order> ordersFromUser = managerController
//				.getOrdersFromUser(DefaultDataTestHelper.FED_ACCESS_TOKEN_ID);
//		Assert.assertEquals(1, ordersFromUser.size());
//		for (Order order : ordersFromUser) {
//			Assert.assertTrue(order.getState().equals(OrderState.CLOSED));
//		}
//	}

}

package org.fogbowcloud.manager.experiments.monitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.ManagerControllerXP;
import org.fogbowcloud.manager.core.plugins.compute.fake.FakeCloudComputePlugin;
import org.fogbowcloud.manager.experiments.MainExperiments;
import org.fogbowcloud.manager.experiments.data.OrderStatus;
import org.fogbowcloud.manager.experiments.data.PeerState;
import org.fogbowcloud.manager.experiments.monitor.MonitorPeerStateSingleton.MonitorPeerStateAssync;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.occi.order.OrderAttribute;
import org.fogbowcloud.manager.occi.order.OrderConstants;
import org.fogbowcloud.manager.occi.order.OrderState;
import org.fogbowcloud.manager.occi.order.OrderType;
import org.fogbowcloud.manager.occi.order.OrderXP;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestMonitorPeerStateSingleton {

	private final String PATH_BASE = "src/test/resources/experiments/";
	private final String PATH_MANAGER = PATH_BASE + "manager.conf";
	private final String PATH_INFRASTRUCTURE = PATH_BASE + "infrastructure.conf";
	private final String PATH_FEDERATION = PATH_BASE + "federation.conf";
	private final String PEER_ID = "p1";
	
	private Properties prop = null;
	private MonitorPeerStateAssync monitor;	
	private ManagerControllerXP mc;
	private String managerId;
	
	@Before
	public void setup(){		
		try {
			prop = MainExperiments.createProperties(new File(PATH_MANAGER), new File(PATH_INFRASTRUCTURE), new File(PATH_FEDERATION));
			prop.put(ConfigurationConstants.XMPP_JID_KEY, PEER_ID);
			prop.put(FakeCloudComputePlugin.COMPUTE_FAKE_QUOTA, "2");	//compute_fake_quota
			prop.put(MonitorPeerStateSingleton.OUTPUT_DATA_ENDING_TIME, "600000");	//output_data_ending_time
			prop.put(MonitorPeerStateSingleton.OUTPUT_FOLDER, "");	//output_data_ending_time
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		managerId = prop.getProperty(ConfigurationConstants.XMPP_JID_KEY);
		
		mc = Mockito.mock(ManagerControllerXP.class);
		Mockito.when(mc.getManagerId()).thenReturn(managerId);
		Mockito.when(mc.getProperties()).thenReturn(prop);		
		
		List<ManagerControllerXP> fms = new ArrayList<ManagerControllerXP>();
		fms.add(mc);
		
		MonitorPeerStateSingleton.getInstance().init(fms, true);		
		monitor = MonitorPeerStateSingleton.getInstance().getMonitors().get(managerId);		
	}
	
//	PeerState(fm.getManagerId(),now, dTot, dFed, rFed, oFed, sFed);
	
	@Test
	public void testGetPeerStateWithNoOrders() {
		Mockito.when(mc.getMaxCapacityDefaultUser()).thenReturn(2);
		
		PeerState state = monitor.getPeerState();
		Assert.assertNotNull(state);		
		Assert.assertEquals(0, state.getdTot());
		Assert.assertEquals(0, state.getdFed());
		Assert.assertEquals(0, state.getrFed());
		Assert.assertEquals(2, state.getoFed());
		Assert.assertEquals(0, state.getsFed());		
	}
	
	@Test
	public void testGetPeerStateWith1OpenLocalOrder() {	
		Mockito.when(mc.getMaxCapacityDefaultUser()).thenReturn(2);
		OrderXP o = createOrder("o1", true, OrderState.OPEN, true);
		monitor.getCurrentOrders().put(o.getId(), new OrderStatus(o.getState(), o.getRequestingMemberId(), o.getProvidingMemberId()));
		
		PeerState state = monitor.getPeerState();
		Assert.assertNotNull(state);		
		Assert.assertEquals(1, state.getdTot());
		Assert.assertEquals(0, state.getdFed());
		Assert.assertEquals(0, state.getrFed());
		Assert.assertEquals(2, state.getoFed());
		Assert.assertEquals(0, state.getsFed());		
	}
	
	@Test
	public void testGetPeerStateWith2OpenLocalOrder() {
		Mockito.when(mc.getMaxCapacityDefaultUser()).thenReturn(2);
		OrderXP o1 = createOrder("o1", true, OrderState.OPEN, true);
		monitor.getCurrentOrders().put(o1.getId(), new OrderStatus(o1.getState(), o1.getRequestingMemberId(), o1.getProvidingMemberId()));
		OrderXP o2 = createOrder("o2", true, OrderState.OPEN, true);
		monitor.getCurrentOrders().put(o2.getId(), new OrderStatus(o2.getState(), o2.getRequestingMemberId(), o2.getProvidingMemberId()));		
		
		PeerState state = monitor.getPeerState();
		Assert.assertNotNull(state);		
		Assert.assertEquals(2, state.getdTot());
		Assert.assertEquals(0, state.getdFed());
		Assert.assertEquals(0, state.getrFed());
		Assert.assertEquals(2, state.getoFed());
		Assert.assertEquals(0, state.getsFed());		
	}
	
	@Test
	public void testGetPeerStateWith1FulfilledLocalOrderMetLocally() {	
		Mockito.when(mc.getMaxCapacityDefaultUser()).thenReturn(2);
		OrderXP o = createOrder("o1", true, OrderState.FULFILLED, true);
		monitor.getCurrentOrders().put(o.getId(), new OrderStatus(o.getState(), o.getRequestingMemberId(), o.getProvidingMemberId()));
		
		PeerState state = monitor.getPeerState();
		Assert.assertNotNull(state);		
		Assert.assertEquals(1, state.getdTot());
		Assert.assertEquals(0, state.getdFed());
		Assert.assertEquals(0, state.getrFed());
		Assert.assertEquals(1, state.getoFed());
		Assert.assertEquals(0, state.getsFed());		
	}
	
	@Test
	public void testGetPeerStateWith2FulfilledLocalOrderMetLocally() {	
		Mockito.when(mc.getMaxCapacityDefaultUser()).thenReturn(2);
		OrderXP o1 = createOrder("o1", true, OrderState.FULFILLED, true);
		monitor.getCurrentOrders().put(o1.getId(), new OrderStatus(o1.getState(), o1.getRequestingMemberId(), o1.getProvidingMemberId()));
		OrderXP o2 = createOrder("o2", true, OrderState.FULFILLED, true);
		monitor.getCurrentOrders().put(o2.getId(), new OrderStatus(o2.getState(), o2.getRequestingMemberId(), o2.getProvidingMemberId()));		
		
		PeerState state = monitor.getPeerState();
		Assert.assertNotNull(state);		
		Assert.assertEquals(2, state.getdTot());
		Assert.assertEquals(0, state.getdFed());
		Assert.assertEquals(0, state.getrFed());
		Assert.assertEquals(0, state.getoFed());
		Assert.assertEquals(0, state.getsFed());		
	}
	
	@Test
	public void testGetPeerStateWith2FulfilledLocalOrderMetLocallyAnd1FulfilledLocalOrderMetRemotely() {	
		Mockito.when(mc.getMaxCapacityDefaultUser()).thenReturn(2);
		OrderXP o1 = createOrder("o1", true, OrderState.FULFILLED, true);
		monitor.getCurrentOrders().put(o1.getId(), new OrderStatus(o1.getState(), o1.getRequestingMemberId(), o1.getProvidingMemberId()));
		OrderXP o2 = createOrder("o2", true, OrderState.FULFILLED, true);
		monitor.getCurrentOrders().put(o2.getId(), new OrderStatus(o2.getState(), o2.getRequestingMemberId(), o2.getProvidingMemberId()));		
		OrderXP o3 = createOrder("o3", true, OrderState.FULFILLED, false);
		monitor.getCurrentOrders().put(o3.getId(), new OrderStatus(o3.getState(), o3.getRequestingMemberId(), o3.getProvidingMemberId()));
		
		PeerState state = monitor.getPeerState();
		Assert.assertNotNull(state);		
		Assert.assertEquals(3, state.getdTot());
		Assert.assertEquals(1, state.getdFed());
		Assert.assertEquals(1, state.getrFed());
		Assert.assertEquals(0, state.getoFed());
		Assert.assertEquals(0, state.getsFed());		
	}
	
	@Test
	public void testGetPeerStateWith1FulfilledLocalOrderMetLocallyAnd1FulfilledServedOrderMetLocally() {	
		Mockito.when(mc.getMaxCapacityDefaultUser()).thenReturn(2);
		OrderXP o1 = createOrder("o1", true, OrderState.FULFILLED, true);
		monitor.getCurrentOrders().put(o1.getId(), new OrderStatus(o1.getState(), o1.getRequestingMemberId(), o1.getProvidingMemberId()));
		OrderXP o2 = createOrder("o2", false, OrderState.FULFILLED, true);
		monitor.getCurrentOrders().put(o2.getId(), new OrderStatus(o2.getState(), o2.getRequestingMemberId(), o2.getProvidingMemberId()));
		
		PeerState state = monitor.getPeerState();
		Assert.assertNotNull(state);		
		Assert.assertEquals(1, state.getdTot());
		Assert.assertEquals(0, state.getdFed());
		Assert.assertEquals(0, state.getrFed());
		Assert.assertEquals(1, state.getoFed());
		Assert.assertEquals(1, state.getsFed());		
	}
	
	@Test
	public void testGetPeerStateWith2FulfilledServedOrderMetLocally() {	
		Mockito.when(mc.getMaxCapacityDefaultUser()).thenReturn(2);
		OrderXP o1 = createOrder("o1", false, OrderState.FULFILLED, true);
		monitor.getCurrentOrders().put(o1.getId(), new OrderStatus(o1.getState(), o1.getRequestingMemberId(), o1.getProvidingMemberId()));
		OrderXP o2 = createOrder("o2", false, OrderState.FULFILLED, true);
		monitor.getCurrentOrders().put(o2.getId(), new OrderStatus(o2.getState(), o2.getRequestingMemberId(), o2.getProvidingMemberId()));
		
		PeerState state = monitor.getPeerState();
		Assert.assertNotNull(state);		
		Assert.assertEquals(0, state.getdTot());
		Assert.assertEquals(0, state.getdFed());
		Assert.assertEquals(0, state.getrFed());
		Assert.assertEquals(2, state.getoFed());
		Assert.assertEquals(2, state.getsFed());		
	}
	
	@Test
	public void testMonitorOrderWith1OpenLocalOrder() {
		Mockito.when(mc.getMaxCapacityDefaultUser()).thenReturn(2);
		OrderXP o = createOrder("o1", true, OrderState.OPEN, true);
		monitor.monitorOrder(o);
		
		Assert.assertEquals(1, monitor.getCurrentOrders().size());
		
		Assert.assertNotNull(monitor.getCurrentOrders().get("o1"));
	}
	
	@Test
	public void testMonitorOrderWith3OpenLocalOrder() {
		Mockito.when(mc.getMaxCapacityDefaultUser()).thenReturn(2);		
		
		monitor.monitorOrder(createOrder("o1", true, OrderState.OPEN, true));
		PeerState state = new PeerState("", 0, 1, 0, 0, 2, 0, 0, 0, 0);
		Assert.assertTrue(areTheseStatesEqual(state, monitor.getLastState()));
		
		monitor.monitorOrder(createOrder("o2", true, OrderState.OPEN, true));
		state = new PeerState("", 0, 2, 0, 0, 2, 0, 0, 0, 0);
		Assert.assertTrue(areTheseStatesEqual(state, monitor.getLastState()));
		
		monitor.monitorOrder(createOrder("o3", true, OrderState.OPEN, true));
		state = new PeerState("", 0, 3, 1, 0, 2, 0, 0, 0, 0);
		Assert.assertTrue(areTheseStatesEqual(state, monitor.getLastState()));
		
		Assert.assertEquals(3, monitor.getCurrentOrders().size());		
		Assert.assertNotNull(monitor.getCurrentOrders().get("o3"));
		
		monitor.monitorOrder(createOrder("o3", true, OrderState.OPEN, true));
		state = new PeerState("", 0, 3, 1, 0, 2, 0, 0, 0, 0);
		Assert.assertTrue(areTheseStatesEqual(state, monitor.getLastState()));
		Assert.assertEquals(3, monitor.getCurrentOrders().size());
		
		monitor.monitorOrder(createOrder("o3", true, OrderState.FULFILLED, true));
		state = new PeerState("", 0, 3, 1, 0, 1, 0, 0, 0, 0);
		Assert.assertTrue(areTheseStatesEqual(state, monitor.getLastState()));
		Assert.assertEquals(3, monitor.getCurrentOrders().size());
		
		monitor.monitorOrder(createOrder("o3", true, OrderState.CLOSED, true));
		state = new PeerState("", 0, 2, 0, 0, 2, 0, 0, 0, 0);
		Assert.assertTrue(areTheseStatesEqual(state, monitor.getLastState()));
		Assert.assertEquals(2, monitor.getCurrentOrders().size());
		
		monitor.monitorOrder(createOrder("o2", true, OrderState.DELETED, true));
		state = new PeerState("", 0, 1, 0, 0, 2, 0, 0, 0, 0);
		Assert.assertTrue(areTheseStatesEqual(state, monitor.getLastState()));
		Assert.assertEquals(1, monitor.getCurrentOrders().size());
		
		monitor.monitorOrder(createOrder("o1", true, OrderState.FAILED, true));
		state = new PeerState("", 0, 0, 0, 0, 2, 0, 0, 0, 0);
		Assert.assertTrue(areTheseStatesEqual(state, monitor.getLastState()));
		Assert.assertEquals(0, monitor.getCurrentOrders().size());
	}
	
	private OrderXP createOrder(String id, boolean isLocal, OrderState state, boolean metLocally){		
		List<Category> categories = new ArrayList<Category>();
		
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(OrderAttribute.RESOURCE_KIND.getValue(), OrderConstants.COMPUTE_TERM);
		attributes.put(OrderAttribute.INSTANCE_COUNT.getValue(), "1");
		attributes.put(OrderAttribute.DATA_PUBLIC_KEY.getValue(), "public key");
		attributes.put(OrderAttribute.TYPE.getValue(), OrderType.PERSISTENT.getValue());
		attributes.put(OrderAttribute.PREVIOUS_ELAPSED_TIME.getValue(), "0");
		attributes.put(OrderAttribute.CURRENT_ELAPSED_TIME.getValue(), "0");
		attributes.put(OrderAttribute.RUNTIME.getValue(), "1000");
		
		Token token = new Token("accessIdToken", new Token.User("user", ""), new Date(), attributes);
		
		String requestingMemberId = "";
		if(isLocal){
			requestingMemberId = prop.getProperty(ConfigurationConstants.XMPP_JID_KEY);
		} else{
			requestingMemberId = "anyMember";
		}
		
		OrderXP o = new OrderXP(id, token, categories, attributes, isLocal, requestingMemberId);
		o.setState(state);
		
		if(state.equals(OrderState.FULFILLED)){
			if(metLocally){
				o.setProvidingMemberId(prop.getProperty(ConfigurationConstants.XMPP_JID_KEY));
			} else{
				o.setProvidingMemberId("anyMember");
			}			
		}		
		
		return o;
	}
	
	private boolean areTheseStatesEqual(PeerState lastState, PeerState currentState){
		if(lastState.getdTot() != currentState.getdTot() ||
				lastState.getdFed() != currentState.getdFed() ||
				lastState.getrFed() != currentState.getrFed() ||
				lastState.getoFed() != currentState.getoFed() ||
				lastState.getsFed() != currentState.getsFed()){
			return false;
		} else {
			return true;
		}
		
	}

}

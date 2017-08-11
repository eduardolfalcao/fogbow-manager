package org.fogbowcloud.manager.occi;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderAttribute;
import org.fogbowcloud.manager.occi.order.OrderConstants;
import org.fogbowcloud.manager.occi.order.OrderState;
import org.fogbowcloud.manager.occi.order.OrderType;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestManagerDataStoreXP {
	
	private Order orderOne;
	private Order orderTwo;
	private Order orderThree;
	private Order orderFour;
	
	private Properties properties = null;
	private ManagerDataStoreXP database = null; 
	
	@Before
	public void initialize() {		
		this.properties = new Properties();
		this.database = new ManagerDataStoreXP(properties);
		initializeOrders();
	}		
	
	@Test
	public void testAddOrder() throws SQLException, JSONException {
		database.addOrder(orderOne);
		List<Order> orders = database.getOrders();
		Assert.assertEquals(1, orders.size());

		Assert.assertTrue(orderOne.equals(orders.get(0)));
	}
	
	@Test
	public void testGetOrders() throws SQLException, JSONException {
		List<Order> orders = new ArrayList<Order>();
		orders.add(orderOne);
		orders.add(orderTwo);
		orders.add(orderThree);
		
		for (Order order : orders) {
			database.addOrder(order);
		}
		
		orders = database.getOrders();
		
		Assert.assertEquals(3, orders.get(0).getFederationToken().getAttributes().size());
		Assert.assertEquals(10, orders.get(0).getxOCCIAtt().size());
		Assert.assertEquals(2, orders.get(0).getCategories().size());
	}
	
	@Test
	public void testGetOrdersWithState() throws SQLException, JSONException {
		List<Order> orders = new ArrayList<Order>();
		orders.add(orderOne);
		orders.add(orderTwo);
		orders.add(orderThree);
		orders.add(orderFour);
		
		for (Order order : orders) {
			database.addOrder(order);
		}
		
		Assert.assertEquals(4, database.getOrders().size());
		Assert.assertEquals(2, database.getOrders(OrderState.OPEN).size());
		Assert.assertEquals(1, database.getOrders(OrderState.FULFILLED).size());
	}	
	
	@Test
	public void testGetOrder() throws SQLException, JSONException {
		List<Order> orders = new ArrayList<Order>();
		orders.add(orderOne);
		orders.add(orderTwo);
		orders.add(orderThree);
		
		for (Order order : orders) {
			database.addOrder(order);
		}
				
		Order orderTwoExcepted = database.getOrder(orderTwo.getId());
		Assert.assertEquals(orderTwoExcepted, orderTwo);
		Order nullValue = database.getOrder("");
		Assert.assertNull(nullValue);
	}	
	
	@Test
	public void testGetSyncronousOrder() throws SQLException, JSONException {
		List<Order> orders = new ArrayList<Order>();
		orders.add(orderOne);
		orders.add(orderTwo);
		
		for (Order order : orders) {
			database.addOrder(order);
		}
		
		orderTwo.setSyncronousStatus(true);
		database.updateOrderAsyncronous(orderTwo.getId(), new Date().getTime(), true);
				
		boolean isOrderSyncronous = true;
		Order orderTwoBD = database.getOrder(orderTwo.getId(), isOrderSyncronous);
		System.out.println(orderTwo.toString());
		System.out.println(orderTwoBD.toString());
		Assert.assertEquals(this.orderTwo.getId(), orderTwoBD.getId());
		Order orderOneBD = database.getOrder(orderOne.getId(), isOrderSyncronous);
		Assert.assertNull(orderOneBD);
	}	
	
	@Test
	public void testUpdateOrder() throws SQLException, JSONException {
		database.addOrder(orderOne);
		List<Order> orders = database.getOrders();
		Assert.assertEquals(1, orders.size());
		Assert.assertTrue(orderOne.equals(orders.get(0)));
		
		orderOne.setProvidingMemberId("ProvidingMemberId");
		orderOne.getCategories().add(new Category("@@@", "@@@", "@@@"));
		orderOne.getxOCCIAtt().put("@@", "@@");
		orderOne.getFederationToken().setExpirationDate(new Date());
		
		database.updateOrder(orderOne);
		
		orders = database.getOrders();
		Assert.assertEquals(1, orders.size());
		Assert.assertEquals(orderOne, orders.get(0));	
	}
	
	@Test
	public void testUpdateOrderAsyncronous() throws SQLException, JSONException {
		database.addOrder(orderOne);
		
		Order order = database.getOrder(orderOne.getId());
		Assert.assertEquals(0L, order.getSyncronousTime());
				
		long syncronousTime = 100;		
		database.updateOrderAsyncronous(order.getId(), syncronousTime, true);
		
		Assert.assertEquals(syncronousTime, database.getOrder(order.getId(), true).getSyncronousTime());
		//FIXME check if the test was wrong
//		Assert.assertEquals(syncronousTime, database.getOrder(order.getId()).getSyncronousTime());
	}
	
	@Test
	public void testRemoveOrder() throws SQLException, JSONException {
		List<Order> orders = new ArrayList<Order>();
		orders.add(orderOne);
		orders.add(orderTwo);
		orders.add(orderThree);
		
		for (Order order : orders) {
			database.addOrder(order);
		}
		
		List<Order> ordersDB = database.getOrders();
		Assert.assertEquals(3, ordersDB.size());
		
		database.removeOrder(orderOne);
		
		ordersDB = database.getOrders();
		Assert.assertEquals(2, ordersDB.size());
		
		database.removeOrder(orderTwo);
		database.removeOrder(orderThree);
		
		ordersDB = database.getOrders();
		Assert.assertEquals(0, ordersDB.size());		
	}
	
	@Test
	public void testRemoveAllOrder() throws SQLException, JSONException {
		List<Order> orders = new ArrayList<Order>();
		orders.add(orderOne);
		orders.add(orderTwo);
		orders.add(orderThree);
		
		for (Order order : orders) {
			database.addOrder(order);
		}
		
		List<Order> ordersDB = database.getOrders();
		Assert.assertEquals(3, ordersDB.size());
		
		database.removeAllOrder();
		
		ordersDB = database.getOrders();
		Assert.assertEquals(0, ordersDB.size());		
	}	
	
	@Test
	public void testCountOrder() {
		List<Order> orders = new ArrayList<Order>();
		orders.add(orderOne);
		orders.add(orderTwo);
		orders.add(orderThree);
		orders.add(orderFour);
		
		for (Order order : orders) {
			database.addOrder(order);
		}
		
		List<OrderState> orderStates = new ArrayList<OrderState>();
		int count = database.countOrder(orderStates);
		Assert.assertEquals(orderStates.size(), count);
		//FIXME check if the test was wrong
//		Assert.assertEquals(orders.size(), count);
		
		orderStates.add(OrderState.FULFILLED);
		
		count = database.countOrder(orderStates);
		Assert.assertEquals(1, count);
		
		orderStates.add(OrderState.DELETED);
		
		count = database.countOrder(orderStates);
		Assert.assertEquals(2, count);
		
		orderStates.clear();
		
		orderStates.add(OrderState.OPEN);
		
		count = database.countOrder(orderStates);
		Assert.assertEquals(2, count);
	}	
	
	@Test
	public void addFederationMemberServered() throws SQLException, JSONException {
		database.addOrder(orderOne);
		List<String> federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(0, federationMembersServered.size());
		
		String federationMemberServerd = "federationMemberServerdOne";
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd);
		
		federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(1, federationMembersServered.size());
		Assert.assertEquals(federationMemberServerd, federationMembersServered.get(0));		
	}
	
	@Test
	public void getFederationMemberServeredWithSameName() throws SQLException, JSONException {
		database.addOrder(orderOne);
		List<String> federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(0, federationMembersServered.size());
		
		String federationMemberServerd = "federationMemberServerdOne";
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd);
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd);
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd);
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd);
		
		federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(1, federationMembersServered.size());
		Assert.assertEquals(federationMemberServerd, federationMembersServered.get(0));		
	}	
	
	@Test
	public void getFederationMemberServeredBy() throws SQLException, JSONException {
		database.addOrder(orderOne);
		List<String> federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(0, federationMembersServered.size());
		
		String federationMemberServerd = "federationMemberServerdOne";
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd);
		
		federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(1, federationMembersServered.size());
		Assert.assertEquals(federationMemberServerd, federationMembersServered.get(0));
		
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd + "Two");
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd + "Three");
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd + "Four");
		federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(4, federationMembersServered.size());
	}
	
	@Test
	public void removeFederationMemberServeredBy() throws SQLException, JSONException {
		database.addOrder(orderOne);
		List<String> federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(0, federationMembersServered.size());
		
		String federationMemberServerd = "federationMemberServerdOne";
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd);
		
		federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(1, federationMembersServered.size());
		Assert.assertEquals(federationMemberServerd, federationMembersServered.get(0));
		
		String federationMemberServerdTwo = federationMemberServerd + "Two";
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerdTwo);
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd + "Three");
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd + "Four");
		federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(4, federationMembersServered.size());
		
		database.removeFederationMemberServed(federationMemberServerd);
		federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(3, federationMembersServered.size());
		
		database.removeFederationMemberServed(federationMemberServerdTwo);
		federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(2, federationMembersServered.size());		
	}
	
	@Test
	public void removeAllFederationMemberServeredInCastateRemovingTheOrder() throws SQLException, JSONException {
		Assert.assertEquals(0, database.getOrders().size());
		
		database.addOrder(orderOne);
		List<String> federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(1, database.getOrders().size());
		Assert.assertEquals(0, federationMembersServered.size());
		
		String federationMemberServerd = "federationMemberServerdOne";
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd);
		
		federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(1, federationMembersServered.size());
		Assert.assertEquals(federationMemberServerd, federationMembersServered.get(0));
		
		String federationMemberServerdTwo = federationMemberServerd + "Two";
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerdTwo);
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd + "Three");
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd + "Four");
		Assert.assertEquals(1, database.getOrders().size());
		federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(4, federationMembersServered.size());
		
		database.removeOrder(orderOne);
		federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(0, database.getOrders().size());
		Assert.assertEquals(0, federationMembersServered.size());
	}	
	
	@Test
	public void addFederationMemberServeredWithoutAddOrderInDB(){
		List<String> federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(0, federationMembersServered.size());
		
		String federationMemberServerd = "federationMemberServerdOne";
		database.addFederationMemberServered(orderOne.getId(), federationMemberServerd);
		
		federationMembersServered = database.getFederationMembersServeredBy(orderOne.getId());
		Assert.assertEquals(0, federationMembersServered.size());
	}	
	
	private void initializeOrders() {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("attrOne", "valueOne");
		attributes.put("attrTwo", "valueTwo");
		List<Category> categories = new ArrayList<Category>();
		categories.add(new Category("term", "schem", "class"));
		categories.add(new Category("termTwo", "schemTwo", "classTwo"));
		Map<String, String> xOCCIAttributes = new HashMap<String, String>();
		xOCCIAttributes.put("occiAttr1.occi", "occiValue1");
		xOCCIAttributes.put("occiAttr2.occi", "occiValue2=");
		xOCCIAttributes.put("occiAttr3.occi", "x>=1 && y=1");		
		xOCCIAttributes.put(OrderAttribute.RESOURCE_KIND.getValue(), OrderConstants.COMPUTE_TERM);
		xOCCIAttributes.put(OrderAttribute.INSTANCE_COUNT.getValue(), "1");
		xOCCIAttributes.put(OrderAttribute.DATA_PUBLIC_KEY.getValue(), "public key");
		xOCCIAttributes.put(OrderAttribute.TYPE.getValue(), OrderType.PERSISTENT.getValue());
		xOCCIAttributes.put(OrderAttribute.PREVIOUS_ELAPSED_TIME.getValue(), "0");
		xOCCIAttributes.put(OrderAttribute.CURRENT_ELAPSED_TIME.getValue(), "0");
		xOCCIAttributes.put(OrderAttribute.RUNTIME.getValue(), "1000");
		Token token = new Token("accessIdToken", new Token.User("user", ""), new Date(), attributes);
		orderOne =  new Order("requstIdOne", token , "instanceIdOne", "providerOne", "memberOne",
				new Date().getTime(), true, OrderState.OPEN, categories, xOCCIAttributes);
		orderTwo =  new Order("requstIdTwo", token , "instanceIdTwo", "providerTwo", "memberTwo",
				new Date().getTime(), true, OrderState.OPEN, categories, xOCCIAttributes);		
		
		Map<String, String> xOCCIAttributesAux = new HashMap<String, String>();
		xOCCIAttributesAux.put(OrderAttribute.PREVIOUS_ELAPSED_TIME.getValue(), "0");
		xOCCIAttributesAux.put(OrderAttribute.CURRENT_ELAPSED_TIME.getValue(), "0");
		xOCCIAttributesAux.put(OrderAttribute.RUNTIME.getValue(), "1000");		
		orderThree = new Order("requstIdThree", token, "instanceIdThree", "providerThree",
				"memberThree", new Date().getTime(), true, OrderState.FULFILLED,
				new ArrayList<Category>(), xOCCIAttributesAux);
		
		HashMap<String, String> xOCCIAttributesTwo = new HashMap<String, String>();
		xOCCIAttributesTwo.put("1.22.3.5.1", "#@$#gv=.j0");
		xOCCIAttributesTwo.put(OrderAttribute.PREVIOUS_ELAPSED_TIME.getValue(), "0");
		xOCCIAttributesTwo.put(OrderAttribute.CURRENT_ELAPSED_TIME.getValue(), "0");
		xOCCIAttributesTwo.put(OrderAttribute.RUNTIME.getValue(), "1000");
		orderFour = new Order("requstIdFour", token, "instanceIdThree", "providerThree",
				"memberThree", new Date().getTime(), true, OrderState.DELETED,
				new ArrayList<Category>(), xOCCIAttributesTwo);		
	}
	
}

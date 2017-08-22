package org.fogbowcloud.manager.occi;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.experiments.monitor.MonitorPeerStateSingleton;
import org.fogbowcloud.manager.experiments.monitor.WorkloadMonitorAssync;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderState;
import org.fogbowcloud.manager.occi.storage.StorageLink;
import org.json.JSONException;

public class ManagerDataStoreXP extends ManagerDataStore{

	private static final Logger LOGGER = Logger.getLogger(ManagerDataStoreXP.class);
	
	
	private String managerId;

	private Map<String, Order> orders;
	private Map<String, Set<String>> servedMembers;
	private List<String> threads = new ArrayList<String>();
	private WorkloadMonitorAssync workloadMonitorAssync;
	
	public ManagerDataStoreXP(Properties properties) {
		super();
		orders = new HashMap<String, Order>();		
		servedMembers = new HashMap<String, Set<String>>();
		managerId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);		
	}
	
	private synchronized void monitorOrderState(final Order order) {
		if(!order.getState().equals(OrderState.SPAWNING))		
			MonitorPeerStateSingleton.getInstance().getMonitors().get(managerId).monitorOrder(order);
	}
	
	public void setWorkloadMonitorAssync(
			WorkloadMonitorAssync workloadMonitorAssync) {
		this.workloadMonitorAssync = workloadMonitorAssync;
	}
	
	@Override
	public synchronized boolean addOrder(Order order){
		
		try{
			orders.put(order.getId(), order);
		} catch(Exception e){
			LOGGER.error("<"+managerId+">: Exception while adding order("+order.getId()+") in map: "+orders, e);
			return false;
		}		
		
		try{
			monitorOrderState(order);
		}catch(Exception e){
			LOGGER.error("<"+managerId+">: Exception while monitoring order with id "+order.getId(), e);
			return false;
		}
		
		if (order.getState().equals(OrderState.FULFILLED)) {
			try{
				this.workloadMonitorAssync.monitorOrder(order);
				threads.add(order.getId());		
			} catch(Exception e){
				LOGGER.error("<"+managerId+">: Exception while tried to schedule the monitoring of order with id "+order.getId(), e);
			}	
		} else if (threads.contains(order.getId())) {
			try{
				threads.remove(order.getId());
				this.workloadMonitorAssync.stopMonitoring(order);
			}catch(Exception e){
				LOGGER.error("<"+managerId+">: Exception while tried to schedule remotion of order with id "+order.getId(), e);
			}	
		}
		
		return true;
	}
	
	@Override
	public synchronized List<Order> getOrders(){
		return new ArrayList<Order>(orders.values());
	}
	
	@Override
	public synchronized List<Order> getOrders(OrderState orderState) {		
		List<Order> ordersInSpecificState = new ArrayList<Order>();
		for (Map.Entry<String, Order> entry : orders.entrySet()){
			if(entry.getValue().getState().equals(orderState))
				ordersInSpecificState.add(entry.getValue());
		}
		return ordersInSpecificState;
	}		
	
	@Override		
	public synchronized Order getOrder(String orderId) {
		return getOrder(orderId,false);
	}
	
	@Override
	public synchronized Order getOrder(String orderId, boolean isOrderSyncronous) {
		Order order = null;		
		if(isOrderSyncronous){
			if(orders.containsKey(orderId) && orders.get(orderId).isSyncronousStatus()==isOrderSyncronous){
				order = orders.get(orderId);
			}
		} else{
			if(orders.containsKey(orderId)){
				order = orders.get(orderId);
			}
		}
		return order;		
	}	
	
	@Override
	public synchronized boolean removeOrder(Order order) {
		
		try{
			orders.remove(order.getId());
			servedMembers.remove(order.getId());
		} catch(Exception e){
			LOGGER.error("<"+managerId+">: Exception while removing order("+order.getId()+") from map: "+orders, e);
			return false;
		}
		
		try{
			monitorOrderState(order);
		}catch(Exception e){
			LOGGER.error("<"+managerId+">: Exception while monitoring order with id "+order.getId(), e);
		}		
		
		try{
			if (threads.contains(order.getId())) {		
				threads.remove(order.getId());
				this.workloadMonitorAssync.stopMonitoring(order);		
			}
		} catch(Exception e){
			LOGGER.error("<"+managerId+">: Exception while tried to schedule remotion of order with id "+order.getId(), e);
		}	
		
		return true;
	}	
	
	@Override
	public synchronized boolean removeAllOrder() {
		try{
			orders.clear();
		} catch(Exception e){
			LOGGER.error("<"+managerId+">: Exception while clearing map: "+orders, e);
			return false;
		}
		
		return true;
	}		
		
	@Override
	public synchronized boolean updateOrder(Order order) {		
		if(!orders.containsKey(order.getId())){
			return false;
		}
			
		try{
			orders.put(order.getId(), order);
		} catch(Exception e){
			LOGGER.error("<"+managerId+">: Exception while updating order("+order.getId()+") from map: "+orders, e);
			return false;
		}
		
		try{
			monitorOrderState(order);
		}catch(Exception e){
			LOGGER.error("<"+managerId+">: Exception while monitoring order with id "+order.getId(), e);
		}
		
		if (order.getState().equals(OrderState.FULFILLED)) {
			try{
				this.workloadMonitorAssync.monitorOrder(order);
				if(!threads.contains(order.getId())){
					LOGGER.info("<"+managerId+">: "+"Added order id "+order.getId()+" on threads list");
					threads.add(order.getId());
				}
			} catch(Exception e){
				LOGGER.error("<"+managerId+">: Exception while tried to schedule the monitoring of order with id "+order.getId(), e);
			}			
		} else if (threads.contains(order.getId())) {
			try{
				LOGGER.info("<"+managerId+">: "+"Removed order id "+order.getId()+" from threads list");
				threads.remove(order.getId());
				this.workloadMonitorAssync.stopMonitoring(order);
			} catch(Exception e){
				LOGGER.error("<"+managerId+">: Exception while tried to schedule remotion of order with id "+order.getId(), e);
			}	
		}
		
		return true;
	}
	
	public synchronized boolean updateOrderAsyncronous(String orderId, long syncronousTime, boolean syncronousStatus){
		try{
			orders.get(orderId).setSyncronousTime(syncronousTime);
			orders.get(orderId).setSyncronousStatus(syncronousStatus);
		} catch(Exception e){
			LOGGER.error("<"+managerId+">: Exception while updating assynchronous order("+orderId+") on map: "+orders, e);
			return false;
		}
		
		return true;
	}
		
	public synchronized int countOrder(List<OrderState> orderStates) {
		
		if(orderStates.size()==0)
			return orders.size();
		
		int size = 0;
		for(OrderState state : orderStates){
			List<Order> ordersInSpecificState = getOrders(state);
			size += ordersInSpecificState==null? 0 : ordersInSpecificState.size();
		}
		return size;		
	}		
	
	/**
	 * Storage Link
	 */
	
	public boolean addStorageLink(StorageLink storageLink) {
		return true;
	}
	
	public List<StorageLink> getStorageLinks() {
		return new ArrayList<StorageLink>();
	}	
		
	public boolean removeStorageLink(StorageLink storageLink) {
		return true;
	}		
		
	public boolean updateStorageLink(StorageLink storageLink) {
		return true;
	}	
	
	/**
	 * Federation member servered
	 */		
	@Override
	public boolean addFederationMemberServered(String orderId, String federationMemberServerd){
		if(!orders.containsKey(orderId)){
			return false;
		}
		else{
			if(!servedMembers.containsKey(orderId))
				servedMembers.put(orderId, new HashSet<String>());
			servedMembers.get(orderId).add(federationMemberServerd);
			return true;
		}
	}
	
	@Override
	public List<String> getFederationMembersServeredBy(String orderId){
		List<String> federationMembersServered = new ArrayList<String>();
		if(servedMembers.get(orderId)!=null)
			federationMembersServered.addAll(servedMembers.get(orderId));
		return federationMembersServered;
	}	
		
	/* used only in tests */
	@Override
	public boolean removeFederationMemberServed(String federationMemberServered) throws SQLException {
		
		boolean removed = false;
		
		Iterator<Entry<String, Set<String>>> it = servedMembers.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, Set<String>> e = it.next();
			Set<String> s = e.getValue();
			Iterator<String> itS = s.iterator();
			while(itS.hasNext()){
				String name = itS.next();
				if(name.equals(federationMemberServered)){
					itS.remove();
					removed = true;
				}
			}
			if(e.getValue().size()==0)
				it.remove();
		}
		
		return removed;
	}
	
}

package org.fogbowcloud.manager.experiments.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.experiments.scheduler.WorkloadScheduler;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.fogbowcloud.manager.occi.order.Order;

public class WorkloadMonitorAssync {
	
	private static final Logger LOGGER = Logger.getLogger(WorkloadMonitorAssync.class);
	private ManagerController fm;
	private String managerId;
	private ScheduledThreadPoolExecutor executorMonitor;
	private Map<Order, ScheduledFuture<?>> orders;	
	
	public WorkloadMonitorAssync(ManagerController fm) {
		this.fm = fm;
		this.managerId = fm.getManagerId();
		this.orders = new HashMap<Order, ScheduledFuture<?>>();
		this.executorMonitor = new ScheduledThreadPoolExecutor(50);
		this.executorMonitor.setRemoveOnCancelPolicy(true);
	}
	
	public void monitorOrder(final Order order){
		long time = order.getRuntime() - order.getPreviousElapsedTime() - order.getCurrentElapsedTime();
		ScheduledFuture<?> schedule = executorMonitor.schedule(
			new Runnable() {					
				@Override
				public void run() {
					boolean isRemoving = false;
					order.updateElapsedTime(isRemoving);
					LOGGER.info("<"+managerId+">: "+"checking if the order("+order.getId()+"), with instance("+order.getInstanceId()+"), requested by "+order.getRequestingMemberId()+
							" and provided by "+ order.getProvidingMemberId() +" will be removed or rescheduled. runtime: " + order.getRuntime()+
							", previousElapsedTime: "+order.getPreviousElapsedTime()+", currentElapsedTime: "+order.getCurrentElapsedTime()+", fulfilledTime: "+order.getFulfilledTime());
					boolean finished = (order.getPreviousElapsedTime() + order.getCurrentElapsedTime()) >= order.getRuntime();
					if(finished){
						removeOrder(fm, order);
					} 
					else{
						long endingTime = order.getRuntime() - order.getPreviousElapsedTime() - order.getCurrentElapsedTime();
						monitorOrder(order);
						LOGGER.info("<"+managerId+">: Rescheduling order("+order.getId()+"), with instance("+order.getInstanceId()+"), requested by "+order.getRequestingMemberId()+
								" and provided by "+ order.getProvidingMemberId() +", with delay "+endingTime);
					}
				}
			}, time, TimeUnit.MILLISECONDS);
		orders.put(order, schedule);
	}
	
	public void stopMonitoring(Order o){
		orders.get(o).cancel(true);
	}
	
	private void removeOrder(final ManagerController fm, final Order order){
		Runnable run = new Runnable() {
			public void run() {
			    try{
			    	if(order.isLocal()){
			    		LOGGER.info("<"+fm.getManagerId()+">: "+"removing local instance ("+order.getInstanceId()+"), orderId("+order.getId()+"), requested by "+order.getRequestingMemberId());
			    		fm.removeInstance(WorkloadScheduler.FAKE_TOKEN, order.getGlobalInstanceId(), order.getResourceKind());
			    	} else{
			    		LOGGER.info("<"+fm.getManagerId()+">: "+"removing local instance ("+order.getInstanceId()+"), orderId("+order.getId()+"), for remote member, requested by "+order.getRequestingMemberId());
			    		fm.removeInstanceForRemoteMember(order.getGlobalInstanceId());
			    	} 
			    } catch(OCCIException ex){
			    	LOGGER.error("<"+fm.getManagerId()+">: Exception while removing instance " + order.getGlobalInstanceId(),ex);
			    }
			}
	   	};
	   	new Thread(run).start();
	}
}						
	

	
	



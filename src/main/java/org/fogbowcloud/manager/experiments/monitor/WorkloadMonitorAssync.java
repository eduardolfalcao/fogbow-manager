package org.fogbowcloud.manager.experiments.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
	private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(10);
	private Map<Order, List<ScheduledFuture<?>>> orders;	
	
	public WorkloadMonitorAssync(ManagerController fm) {
		this.fm = fm;
		this.managerId = fm.getManagerId();
		this.orders = new HashMap<Order, List<ScheduledFuture<?>>>();
		executor.setRemoveOnCancelPolicy(true);
		executor.setMaximumPoolSize(10);
	}
	
	public void monitorOrder(final Order order){
		long time = order.getRuntime() - order.getPreviousElapsedTime() - order.getCurrentElapsedTime();
		ScheduledFuture<?> schedule = executor.schedule(
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
		if(!orders.containsKey(order))
			orders.put(order, new ArrayList<ScheduledFuture<?>>());
		orders.get(order).add(schedule);		
	}
	
	public synchronized void stopMonitoring(Order o){
		LOGGER.info("<"+managerId+">: 1 - number of threads executing on executor "+executor.getActiveCount()+", and total: "+java.lang.Thread.activeCount());
		List<ScheduledFuture<?>> threads = orders.get(o); 
		for(ScheduledFuture<?> thread : threads)
			thread.cancel(true);
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
	

	
	



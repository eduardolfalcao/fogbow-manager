package org.fogbowcloud.manager.experiments.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(10000);
	private Map<Order, ScheduledFuture<?>> orders;	
	
	public WorkloadMonitorAssync(ManagerController fm) {
		this.fm = fm;
		this.managerId = fm.getManagerId();
		this.orders = new HashMap<Order, ScheduledFuture<?>>();
	}
	
	public void monitorOrder(final Order order){
		long time = order.getRuntime() - order.getElapsedTime();
		ScheduledFuture<?> schedule = executor.schedule(
			new Runnable() {					
				@Override
				public void run() {
					boolean isRemoving = false;
					order.updateElapsedTime(isRemoving);
					LOGGER.info("<"+managerId+">: "+"checking if the order("+order.getId()+"), with instance("+order.getInstanceId()+"), requested by "+order.getRequestingMemberId()+
							" and provided by "+ order.getProvidingMemberId() +" will be removed or rescheduled. runtime: " + order.getRuntime()+
							", elapsedTime: "+order.getElapsedTime()+", fulfilledTime: "+order.getFulfilledTime());
					boolean finished = order.getElapsedTime() >= order.getRuntime();
					if(finished){
						removeOrder(fm, order);
					} 
					else{
						long endingTime = order.getRuntime() - order.getElapsedTime();
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
		LOGGER.info("<"+managerId+">: Stopped monitoring order "+o.getId());
	}
	
	private void removeOrder(final ManagerController fm, final Order order){
			Runnable run = new Runnable() {
				public void run() {
				    try{
				    	if(order.isLocal()){
				    		LOGGER.info("<"+fm.getManagerId()+">: "+"removing local instance ("+order.getInstanceId()+"), requested by "+order.getRequestingMemberId());
				    		fm.removeInstance(WorkloadScheduler.FAKE_TOKEN, order.getGlobalInstanceId(), order.getResourceKind());
				    	} else{
				    		LOGGER.info("<"+fm.getManagerId()+">: "+"removing local instance ("+order.getInstanceId()+") for remote member, requested by "+order.getRequestingMemberId());
				    		fm.removeInstanceForRemoteMember(order.getGlobalInstanceId());
				    	} 
				    } catch(OCCIException ex){
				    	LOGGER.error("<"+fm.getManagerId()+">: Exception while removing instance " + order.getGlobalInstanceId(),ex);
				    }
				    
				    try{
				    	if(order.isLocal()){
				    		LOGGER.info("<"+fm.getManagerId()+">: "+"removing local order ("+order.getId()+"), requested by "+order.getRequestingMemberId());
					    	fm.removeOrder(WorkloadScheduler.FAKE_TOKEN, order.getId());
				    	}
				    	else{
				    		LOGGER.info("<"+fm.getManagerId()+">: "+"removing remote order ("+order.getId()+"), requested by "+order.getRequestingMemberId()+". order: "+order);
				    		fm.removeOrderForRemoteMember(WorkloadScheduler.FAKE_TOKEN, order.getId());
				    	}
				    	
				    } catch(OCCIException ex){
				    	LOGGER.error("<"+fm.getManagerId()+">: Exception while removing order " + order.getId(), ex);
				    }
				}
		   	};
		   	new Thread(run).start();
	}	
		    						
}
	

	
	



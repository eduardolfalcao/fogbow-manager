package org.fogbowcloud.manager.experiments.monitor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
	
	public WorkloadMonitorAssync(ManagerController fm) {
		this.fm = fm;
		this.managerId = fm.getManagerId();
	}
	
	public void monitorOrder(final Order order, long time){	
		executor.schedule(
			new Runnable() {					
				@Override
				public void run() {
					Order updatedOrder = fm.getOrder("", order.getId());
					boolean isRemoving = false;
					updatedOrder.updateElapsedTime(isRemoving);
					LOGGER.info("<"+managerId+">: "+"checking if the order("+updatedOrder.getId()+"), with instance("+updatedOrder.getInstanceId()+"), requested by "+updatedOrder.getRequestingMemberId()+
							" and provided by "+ updatedOrder.getProvidingMemberId() +" will be removed or rescheduled. runtime: " + updatedOrder.getRuntime()+
							", elapsedTime: "+updatedOrder.getElapsedTime()+", fulfilledTime: "+updatedOrder.getFulfilledTime());
					boolean finished = updatedOrder.getElapsedTime() >= updatedOrder.getRuntime();
					if(finished){
						removeOrder(fm, updatedOrder);
					} 
					else{
						long endingTime = updatedOrder.getRuntime() - updatedOrder.getElapsedTime();
						monitorOrder(updatedOrder,endingTime);
						LOGGER.info("<"+managerId+">: Rescheduling order("+updatedOrder.getId()+"), with instance("+updatedOrder.getInstanceId()+"), requested by "+updatedOrder.getRequestingMemberId()+
								" and provided by "+ updatedOrder.getProvidingMemberId() +", with delay "+endingTime);
					}
				}
			}, time, TimeUnit.MILLISECONDS);

	}
	
	private void removeOrder(final ManagerController fm, final Order order){
			Runnable run = new Runnable() {
				public void run() {
				    try{				    	
				    	LOGGER.info("<"+fm.getManagerId()+">: "+"removing instance ("+order.getInstanceId()+"), requested by "+order.getRequestingMemberId());
				    	fm.removeInstance(WorkloadScheduler.FAKE_TOKEN, order.getGlobalInstanceId(), order.getResourceKind()); 
				    } catch(OCCIException ex){
				    	LOGGER.error("Exception while removing instance " + order.getGlobalInstanceId(),ex);
				    }
				    
				    try{
				    	if(fm.getManagerId().equals(order.getProvidingMemberId())){
				    		LOGGER.info("<"+fm.getManagerId()+">: "+"removing local order ("+order.getId()+"), requested by "+order.getRequestingMemberId());
					    	fm.removeOrder(WorkloadScheduler.FAKE_TOKEN, order.getId());
				    	}
				    	else{
				    		LOGGER.info("<"+fm.getManagerId()+">: "+"removing remote order ("+order.getId()+"), requested by "+order.getRequestingMemberId()+". order: "+order);
					    	fm.removeRemoteOrder(order.getProvidingMemberId(),order);
				    	}
				    	
				    } catch(OCCIException ex){
				    	LOGGER.error("Exception while removing order " + order.getId(), ex);
				    }
				}
		   	};
		   	new Thread(run).start();
	}	
		    						
}
	

	
	



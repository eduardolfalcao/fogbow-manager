package org.fogbowcloud.manager.experiments.monitor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.fogbowcloud.manager.occi.order.Order;

public class WorkloadMonitorAssync {
	
	private static final Logger LOGGER = Logger.getLogger(WorkloadMonitorAssync.class);
	private ManagerController fm;
	private String managerId;
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(100);
	
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
				    	fm.removeInstance(null, order.getGlobalInstanceId(), order.getResourceKind()); 
				    } catch(OCCIException ex){
				    	LOGGER.error("Exception while removing instance " + order.getGlobalInstanceId(),ex);
				    }
				    
				    try{
				    	if(fm.getManagerId().equals(order.getProvidingMemberId())){
				    		LOGGER.info("<"+fm.getManagerId()+">: "+"removing local order ("+order.getId()+"), requested by "+order.getRequestingMemberId());
					    	fm.removeOrder(null, order.getId());
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
	
	//TODO implement equals and hashCode
	
//	public void monitorOrders(){
//		String managerId = fm.getManagerId();
//		List<Order> localOrders = fm.getManagerDataStoreController().getAllOrders();
//		List<Order> ordersToBeRemoved = new ArrayList<Order>();
//		for(Order order : localOrders){			
//			if(order == null){
//				LOGGER.warn("<"+managerId+">: Trying to close null order...");
//				continue;
//			}
//			if (!order.getResourceKind().equals(OrderConstants.COMPUTE_TERM)) {
//				LOGGER.warn("<"+managerId+">: There is an order that is not a compute: "+order);
//				continue;
//			}
////			if(order.getProvidingMemberId()!=null && order.getProvidingMemberId().equals(managerId) && !order.getState().equals(OrderState.CLOSED)){
//			if(order.getProvidingMemberId()!=null && order.getProvidingMemberId().equals(managerId)){
//				boolean isRemoving = false;
//				order.updateElapsedTime(isRemoving);	
//				LOGGER.info("<"+managerId+">: "+"checking if the order("+order.getId()+"), with instance("+order.getInstanceId()+"), requested by "+order.getRequestingMemberId()+
//						" will be removed or rescheduled. runtime: " + order.getRuntime()+
//						", elapsedTime: "+order.getElapsedTime()+", fulfilledTime: "+order.getFulfilledTime());
//				
//				boolean finished = order.getElapsedTime() >= order.getRuntime();
//				if(finished)
//					ordersToBeRemoved.add(order);
//			}
//		}
//		removeOrders(fm, ordersToBeRemoved);		
//		
//	}
	
	
	
//	private void removeOrders(final ManagerController fm, List<Order> ordersToBeRemoved){
//		for (final Order order : ordersToBeRemoved){
//			Runnable run = new Runnable() {
//				public void run() {
//				    if(order.getGlobalInstanceId()==null)
//				    	LOGGER.error("<"+fm.getManagerId()+">: trying to remove instance from order "+order);
//				    try{
//				    	LOGGER.info("<"+fm.getManagerId()+">: "+"removing instance ("+order.getInstanceId()+"), requested by "+order.getRequestingMemberId());
//				    	if (order.isLocal()) {
//				    		fm.removeInstance(null, order.getGlobalInstanceId(), order.getResourceKind());				    		
//				    	} else {
//				    		fm.removeInstanceForRemoteMember(order.getInstanceId());
//				    	}
//				    } catch(OCCIException ex){
//				    	LOGGER.error("Exception while removing instance " + order.getGlobalInstanceId(),ex);
//				    }
//				    
//				    try{
//				    	LOGGER.info("<"+fm.getManagerId()+">: "+"removing order ("+order.getId()+"), requested by "+order.getRequestingMemberId());
//				    	if (order.isLocal()) {
//				    		fm.removeOrder(null, order.getId());				    		
//				    	} else {
//				    		fm.removeOrderForRemoteMember("", order.getId());
//				    	}
//				    } catch(OCCIException ex){
//				    	LOGGER.error("Exception while removing order " + order.getId(), ex);
//				    }
//				}
//		   	};
//		   	new Thread(run).start();
//		}	
//		    						
//	}
	
//	public void addJob(Job j){
//		long endingTime = (t.getRuntime()+j.getSubmitTime())
//		executor.schedule(
//				new Runnable() {					
//					@Override
//					public void run() {
//						// TODO Auto-generated method stub
//						
//					}
//				}, 10, TimeUnit.MILLISECONDS);
////		synchronized(jobsSubmitted) {
////			jobsSubmitted.add(j);
////		}
//	}
	
	



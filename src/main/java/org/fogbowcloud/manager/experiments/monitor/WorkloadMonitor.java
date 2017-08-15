package org.fogbowcloud.manager.experiments.monitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.ManagerControllerXP;
import org.fogbowcloud.manager.experiments.scheduler.WorkloadScheduler;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderXP;

public class WorkloadMonitor {
	
	private static final Logger LOGGER = Logger.getLogger(WorkloadMonitor.class);
	private ManagerControllerXP fm;
	private List<Order> ordersSubmitted;
	
	private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
		
	public WorkloadMonitor(ManagerControllerXP fm) {
		this.fm = fm;
		this.ordersSubmitted = new ArrayList<Order>();
		initMonitoring();
	}
	
	private void initMonitoring() {
		Runnable run = new Runnable() {
		    public void run() {
		    	monitorJobs();
		    } 		    		    
	    };
	    executor.scheduleAtFixedRate(run, 0, 1, TimeUnit.SECONDS);
	}
	
	public void monitorJobs(){
		List<OrderXP> ordersToBeRemoved = new ArrayList<OrderXP>();
		synchronized(ordersSubmitted) {			
			LOGGER.info("<"+fm.getManagerId()+">:Submitted size: "+ordersSubmitted.size());
			
			Iterator<Order> itOrders = ordersSubmitted.iterator();			
			while(itOrders.hasNext()){
				String orderId = itOrders.next().getId();
				OrderXP order = (OrderXP)fm.getOrder(WorkloadScheduler.FAKE_TOKEN, orderId);
				boolean isRemoving = false;
				order.updateElapsedTime(isRemoving);				
				LOGGER.info("<"+fm.getManagerId()+">: orderId("+order.getId()+"), runtime("+order.getRuntime()+"), "
					+ "elapsedTime("+(order.getPreviousElapsedTime()+order.getCurrentElapsedTime())+")");				
				boolean finished = (order.getPreviousElapsedTime() + order.getCurrentElapsedTime()) >= order.getRuntime();
				if(finished){
					LOGGER.info("<"+fm.getManagerId()+">: FINISHED orderId("+order.getId()+"), runtime("+order.getRuntime()+"), "
							+ "elapsedTime("+(order.getPreviousElapsedTime()+order.getCurrentElapsedTime())+")");
					ordersToBeRemoved.add(order);
					itOrders.remove();
				} 				
			}
		}
		removeOrders(ordersToBeRemoved);
	}
	
	private void removeOrders(List<OrderXP> ordersToBeRemoved){
		for (OrderXP orderToBeRemoved : ordersToBeRemoved){
			LOGGER.info("<"+fm.getManagerId()+">: "+"monitor scheduling the remotion of instance ("+orderToBeRemoved.getInstanceId()+"), "
					+ "orderId("+orderToBeRemoved.getId()+"), runtime("+orderToBeRemoved.getRuntime()+"), "
					+ "elapsedTime("+(orderToBeRemoved.getPreviousElapsedTime()+orderToBeRemoved.getCurrentElapsedTime())+")");
    		fm.removeInstance(WorkloadScheduler.FAKE_TOKEN, orderToBeRemoved.getGlobalInstanceId(), orderToBeRemoved.getResourceKind());
		}		
	}
	
	public void addOrder(Order o){
		synchronized(ordersSubmitted) {
			ordersSubmitted.add(o);
		}
	}

}

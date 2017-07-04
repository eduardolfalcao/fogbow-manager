package org.fogbowcloud.manager.experiments.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.model.DateUtils;
import org.fogbowcloud.manager.experiments.scheduler.model.Job;
import org.fogbowcloud.manager.experiments.scheduler.model.Task;
import org.fogbowcloud.manager.occi.order.Order;

public class WorkloadMonitor {
	
	private static final Logger LOGGER = Logger.getLogger(WorkloadMonitor.class);
	private List<ManagerController> fms;
	private List<Job> jobsSubmitted;
	
	private DateUtils dateUtils = new DateUtils();
	
	public WorkloadMonitor(List<ManagerController> fms) {
		this.fms = fms;
		this.jobsSubmitted = new ArrayList<Job>();
	}
	
	public void monitorJobs(){
		long now = dateUtils.currentTimeMillis();
		Map<ManagerController,Order> ordersToBeRemoved = new HashMap<ManagerController,Order>();
		synchronized(jobsSubmitted) {
			Iterator<Job> itJobs = jobsSubmitted.iterator();			
			while(itJobs.hasNext()){
				Job j = itJobs.next();
				ManagerController fm = getManager(j);
				Iterator<Task> itTasks = j.getTasks().iterator();
				while(itTasks.hasNext()){
					Task t = itTasks.next();
					Order order = fm.getOrder(null, t.getOrderId());					
					long fulfilledTime = order.getFulfilledTime();
					if(fulfilledTime>0){
						long durationInSec = TimeUnit.MILLISECONDS.toSeconds(now - fulfilledTime);
						if(durationInSec>=t.getRuntime()){							
							ordersToBeRemoved.put(fm, order);
							System.out.println("Removing "+t);
							itTasks.remove();							
						}					
					}
				}
				if(j.getTasks().size()==0)
					itJobs.remove();
			}
		}
		removeOrders(ordersToBeRemoved);
	}
	
	private void removeOrders(Map<ManagerController,Order> ordersToBeRemoved){
		for (Map.Entry<ManagerController, Order> entry : ordersToBeRemoved.entrySet()){
		    ManagerController mc = entry.getKey();
		    Order orderToBeRemoved = entry.getValue();		    
		    mc.removeInstance(null, orderToBeRemoved.getGlobalInstanceId(), orderToBeRemoved.getResourceKind());
		    mc.removeOrder(null, orderToBeRemoved.getId());
		}		
	}
	
	private ManagerController getManager(Job j){
		ManagerController fm = null;
		for(ManagerController mc : fms){
			if(mc.getManagerId().equals(j.getPeerId())){
				fm = mc;
				break;
			}
		}
		return fm;
	}
	
	public void addJob(Job j){
		synchronized(jobsSubmitted) {
			jobsSubmitted.add(j);
		}
	}

}

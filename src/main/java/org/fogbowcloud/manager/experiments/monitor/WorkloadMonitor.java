package org.fogbowcloud.manager.experiments.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.model.DateUtils;
import org.fogbowcloud.manager.experiments.scheduler.model.Job;
import org.fogbowcloud.manager.experiments.scheduler.model.Task;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderAttribute;

public class WorkloadMonitor {
	
	private static final Logger LOGGER = Logger.getLogger(WorkloadMonitor.class);
	private List<ManagerController> fms;
	private List<Job> jobsSubmitted;
	
	private DateUtils dateUtils = new DateUtils();
	
	public WorkloadMonitor(List<ManagerController> fms) {
		this.fms = fms;
		this.jobsSubmitted = new ArrayList<Job>();
		LOGGER.setLevel(Level.INFO);
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
					long elapsedTime = order.getElapsedTime();
					if(fulfilledTime>0 || elapsedTime > 0){
						long durationInMs = elapsedTime;
						if(fulfilledTime>0)
							durationInMs += (now - fulfilledTime);
						long durationInSec = TimeUnit.MILLISECONDS.toSeconds(durationInMs);
						if(durationInSec>=t.getRuntime()){							
							ordersToBeRemoved.put(fm, order);
							itTasks.remove();
							LOGGER.info("Peer "+fm+" removing task "+t+", order "+order.getId()+" - ~ending time:"+(t.getRuntime()+j.getSubmitTime()));
						}					
					}
				}
				if(j.getTasks().size()==0)
					itJobs.remove();
			}
		}
		removeOrders(ordersToBeRemoved);
	}
	
	private void removeOrders(final Map<ManagerController,Order> ordersToBeRemoved){
		for (final Map.Entry<ManagerController, Order> entry : ordersToBeRemoved.entrySet()){
			Runnable run = new Runnable() {
				public void run() {
				    ManagerController mc = entry.getKey();
				    Order orderToBeRemoved = entry.getValue();
				    if(orderToBeRemoved.getGlobalInstanceId()==null)
				    	LOGGER.error("<"+mc.getManagerId()+">: trying to remove instance from order "+orderToBeRemoved);
				    try{
				    	mc.removeInstance(null, orderToBeRemoved.getGlobalInstanceId(), orderToBeRemoved.getResourceKind());
				    } catch(OCCIException ex){
				    	LOGGER.error("Exception while removing instance " + orderToBeRemoved.getGlobalInstanceId() + "\n" + ex.getMessage());
				    }
				    
				    try{
				    	mc.removeOrder(null, orderToBeRemoved.getId());
				    } catch(OCCIException ex){
				    	LOGGER.error("Exception while removing order " + orderToBeRemoved.getId() + "\n" + ex.getMessage());
				    }
				}
		   	};
		   	new Thread(run).start();
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

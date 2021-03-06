package org.fogbowcloud.manager.experiments.scheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.ManagerControllerXP;
import org.fogbowcloud.manager.core.model.DateUtils;
import org.fogbowcloud.manager.experiments.scheduler.model.DataReader;
import org.fogbowcloud.manager.experiments.scheduler.model.Job;
import org.fogbowcloud.manager.experiments.scheduler.model.Peer;
import org.fogbowcloud.manager.experiments.scheduler.model.User;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderAttribute;
import org.fogbowcloud.manager.occi.order.OrderConstants;
import org.fogbowcloud.manager.occi.order.OrderType;
import org.fogbowcloud.manager.xmpp.ManagerPacketHelper;

public class WorkloadScheduler {
	
	private static final Logger LOGGER = Logger.getLogger(WorkloadScheduler.class);
	public static final String 	WORKLOAD_FOLDER = "workload_folder";
	public static final String 	FAKE_TOKEN = "fake-token";	
	private Properties props;
		
	private long time = 0;
	private List<Peer> peers;
	private Map<String, ManagerController> relations;
	
	private Map<String, String> xOCCIAtt;
	private List<Category> categories;
	
	private long initialTime;
	
	public WorkloadScheduler(List<ManagerControllerXP> fms, Properties props) {
		LOGGER.setLevel(Level.INFO);
		this.props = props;
		this.peers = new ArrayList<Peer>();
		
		readWorkloads();
		sortJobsAndTasks();
		
		relations = new HashMap<String, ManagerController>();
		for(Peer p : peers){
			for(ManagerControllerXP mc : fms){
				if(mc.getManagerId().equals(p.getPeerId())){
					relations.put(p.getPeerId(), mc);
					break;
				}				
			}			
		}	
		
		initOrderParams();		
	}
	
	private void initOrderParams() {
		xOCCIAtt = new HashMap<String, String>();
		xOCCIAtt.put(OrderAttribute.RESOURCE_KIND.getValue(), OrderConstants.COMPUTE_TERM);
		xOCCIAtt.put(OrderAttribute.INSTANCE_COUNT.getValue(), "1");
		xOCCIAtt.put(OrderAttribute.DATA_PUBLIC_KEY.getValue(), "public key");
		xOCCIAtt.put(OrderAttribute.TYPE.getValue(), OrderType.PERSISTENT.getValue());
		xOCCIAtt.put(OrderAttribute.PREVIOUS_ELAPSED_TIME.getValue(), "0");
		xOCCIAtt.put(OrderAttribute.CURRENT_ELAPSED_TIME.getValue(), "0");
		
		categories = new ArrayList<Category>();
		categories.add(new Category(OrderConstants.TERM,OrderConstants.SCHEME,OrderConstants.KIND_CLASS));
		categories.add(new Category("fogbow-ubuntu",OrderConstants.TEMPLATE_OS_SCHEME,OrderConstants.MIXIN_CLASS));
		categories.add(new Category(OrderConstants.PUBLIC_KEY_TERM,OrderConstants.CREDENTIALS_RESOURCE_SCHEME,OrderConstants.MIXIN_CLASS));		
	}

	public void checkAndSubmitTasks() {
		if(time==0)
			initialTime = new DateUtils().currentTimeMillis();
		long now = new DateUtils().currentTimeMillis();
		LOGGER.info("TimeMonitor: "+time+"; RealTime: "+TimeUnit.MILLISECONDS.toSeconds((now-initialTime)));
				
		runJobs();
		time++;		
	}		
	
	private void runJobs(){
		Map<ManagerController, List<Job>> peersAndJobs = getJobs(time);		    	
		for(final Entry<ManagerController, List<Job>> e : peersAndJobs.entrySet()){
			new Runnable() {					
				@Override
				public void run() {
					List<Job> jobsToBeSubmitted = e.getValue();
				   	ManagerController mc = e.getKey();
				   	for(Job j : jobsToBeSubmitted){			    										
						Map<String, String> xOCCIAttClone = new HashMap<String, String>();
						xOCCIAttClone.putAll(xOCCIAtt);
						List<Category> categoriesClone = new ArrayList<Category>();
						categoriesClone.addAll(categories);
						for(int i = 0; i < j.getTasks().size(); i++){
							xOCCIAttClone.put(OrderAttribute.RUNTIME.getValue(), String.valueOf(j.getTasks().get(i).getRuntime()*1000));
							List<Order> orders = mc.createOrders(WorkloadScheduler.FAKE_TOKEN, categoriesClone, xOCCIAttClone);
							j.getTasks().get(i).setOrderId(orders.get(0).getId());
						}					
						LOGGER.info("Time: "+time+", Peer "+j.getPeerId()+" creating "+j);
				   	}
				}
			}.run();			
		}
	}
	
	private Map<ManagerController, List<Job>> getJobs(long time) {
		Map<ManagerController, List<Job>> peersAndJobs = new HashMap<ManagerController, List<Job>>();	
		Iterator<Peer> peersIt = peers.iterator();
		while(peersIt.hasNext()){
			Peer p = peersIt.next();			
			ManagerController mc = relations.get(p.getPeerId());
			List<Job> jobs = new ArrayList<Job>();			
			Iterator<User> usersIt = p.getUsers().iterator();
			while(usersIt.hasNext()){
				User u = usersIt.next();
				Iterator<Job> jobsIt = u.getJobs().iterator();
				while(jobsIt.hasNext()){
					Job j = jobsIt.next();
					if(j.getSubmitTime() == time){						
						jobs.add(j);
						jobsIt.remove();
					}
					else if(j.getSubmitTime() > time)
						break;
				}
				if(u.getJobs().isEmpty())
					usersIt.remove();
				peersAndJobs.put(mc, jobs);
			}
			if(p.getUsers().isEmpty())
				peersIt.remove();
		}
		return peersAndJobs;
	}
	
	private void sortJobsAndTasks(){
		for(Peer p : this.peers){
			for(User u : p.getUsers()){
				Collections.sort(u.getJobs());
				for(Job j : u.getJobs()){
					Collections.sort(j.getTasks());
				}
			}
		}
	}

	private void readWorkloads(){
		DataReader df = new DataReader();
		for(String file : getFiles()){
			LOGGER.info("Running on file: "+file);
			df.readWorkload(this.peers, file);
		}		
	}
	
	private List<String> getFiles(){		
		String workloadFolder = props.getProperty(WORKLOAD_FOLDER);
		File folder = new File(workloadFolder);		
		List<String> files = new ArrayList<String>();		
		for (final File fileEntry : folder.listFiles()) {
	        if (!fileEntry.isDirectory()) 
	            files.add(fileEntry.getAbsolutePath());	        
	    }		
		return files; 
	}

	

}

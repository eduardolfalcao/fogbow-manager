package org.fogbowcloud.manager.experiments.scheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.experiments.MainExperiments;
import org.fogbowcloud.manager.experiments.scheduler.model.DataReader;
import org.fogbowcloud.manager.experiments.scheduler.model.Job;
import org.fogbowcloud.manager.experiments.scheduler.model.Peer;
import org.fogbowcloud.manager.experiments.scheduler.model.User;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.order.OrderAttribute;
import org.fogbowcloud.manager.occi.order.OrderConstants;

public class Scheduler {
	
	private static final Logger LOGGER = Logger.getLogger(Scheduler.class);
	public static final String 	WORKLOAD_FOLDER = "workload_folder";	
	private Properties props;
	
	private long time = 0;
	private List<Peer> peers;
	private Map<String, ManagerController> relations;	
	
	private Map<String, String> xOCCIAtt;
	private List<Category> categories;
	
	public Scheduler(List<ManagerController> fms, Properties props) {
		this.props = props;
		this.peers = new ArrayList<Peer>();
		
		readWorkloads();
		
		relations = new HashMap<String, ManagerController>();
		for(Peer p : peers){
			for(ManagerController mc : fms){
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
		xOCCIAtt.put(OrderAttribute.TYPE.getValue(), "one-time");
		
		categories = new ArrayList<Category>();
		categories.add(new Category(OrderConstants.TERM,OrderConstants.SCHEME,OrderConstants.KIND_CLASS));
		categories.add(new Category("fogbow-ubuntu",OrderConstants.TEMPLATE_OS_SCHEME,OrderConstants.MIXIN_CLASS));
		categories.add(new Category(OrderConstants.PUBLIC_KEY_TERM,OrderConstants.CREDENTIALS_RESOURCE_SCHEME,OrderConstants.MIXIN_CLASS));		
	}

	public void checkAndSubmitTasks() {
		List<Job> jobsToBeSubmitted = getJobs(time);
		for(Job j : jobsToBeSubmitted){
			ManagerController mc = relations.get(j.getPeerId());
			Map<String, String> xOCCIAttClone = new HashMap<String, String>();
			xOCCIAttClone.putAll(xOCCIAtt);
			xOCCIAttClone.put(OrderAttribute.INSTANCE_COUNT.getValue(), String.valueOf(j.getTasks().size()));	
			List<Category> categoriesClone = new ArrayList<Category>();
			categoriesClone.addAll(categories);
			mc.createOrders("", categoriesClone, xOCCIAttClone);
			System.out.println("Peer "+j.getPeerId()+" creating "+j);
		}		
		time++;		
	}
	
	private List<Job> getJobs(long time) {
		List<Job> jobs = new ArrayList<Job>();
		for(Peer p : peers){
			for(User u : p.getUsers()){
				for(Job j: u.getJobs()){
					if(j.getSubmitTime() == time)
						jobs.add(j);
					else if(j.getSubmitTime() > time)
						break;
				}
			}			
		}
		return jobs;
	}

	private void readWorkloads(){
		DataReader df = new DataReader();
		for(String file : getFiles()){
			System.out.println("Running on file: "+file);
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

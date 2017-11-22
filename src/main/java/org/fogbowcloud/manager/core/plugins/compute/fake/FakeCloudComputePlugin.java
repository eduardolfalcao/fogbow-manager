package org.fogbowcloud.manager.core.plugins.compute.fake;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.ManagerControllerXP;
import org.fogbowcloud.manager.core.model.ImageState;
import org.fogbowcloud.manager.core.model.ResourcesInfo;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.benchmarking.VanillaBenchmarkingPlugin;
import org.fogbowcloud.manager.experiments.data.OrderStatus;
import org.fogbowcloud.manager.experiments.data.PeerState;
import org.fogbowcloud.manager.experiments.monitor.MonitorPeerStateSingleton;
import org.fogbowcloud.manager.occi.instance.Instance;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.model.ErrorType;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderAttribute;
import org.fogbowcloud.manager.occi.order.OrderState;
import org.restlet.Request;
import org.restlet.Response;

public class FakeCloudComputePlugin implements ComputePlugin {

	public static final String COMPUTE_FAKE_QUOTA = "compute_fake_quota";
	private static final Logger LOGGER = Logger.getLogger(FakeCloudComputePlugin.class);
	
	private int quota;	
	private List<String> instances = new ArrayList<String>();
	
	private String managerId;
	
	private ManagerControllerXP manager = null;
	
	public FakeCloudComputePlugin(Properties properties){
		quota = Integer.parseInt(properties.getProperty(COMPUTE_FAKE_QUOTA));
		managerId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
		LOGGER.info("<"+managerId+">: quota: "+quota+"!");
	}
	
	@Override
	public synchronized String requestInstance(Token token, List<Category> categories,
			Map<String, String> xOCCIAtt, String imageId) {
		
		if(instances.size()>=quota){
			
//			List<Order> orders = manager.getManagerDataStoreController().getOrdersIn(OrderState.FULFILLED);
//			int ordersFulfilledLocally = 0;
//			for(Order o : orders){
//				if(o.getProvidingMemberId().equals(managerId))
//						ordersFulfilledLocally++;
//			}
//			
//			//sempre que sFed+dTot-rFed for < que quota, não deveria haver quota exceeded
//			
//			PeerState currentState = MonitorPeerStateSingleton.getInstance().getMonitors().get(managerId).getCurrentStateDebug();
//			if((currentState.getdTot()-currentState.getrFed()+currentState.getsFed())<quota){				
//				LOGGER.info("<"+managerId+">: <QuotaExceeded> Orders fulfilled locally: "+ordersFulfilledLocally+". "
//						+ "Instances on compute: "+instances.size()+"; "+instances+". "
//						+ "CurrentState: "+currentState+".");
//				
//				List<String> instancesWithoutOrder = new ArrayList<String>();
//				instancesWithoutOrder.addAll(instances);
//				
//				int i = 1;
//				for(Order o : orders){
//					LOGGER.info("<"+managerId+">: <QuotaExceeded> order num "+i+" - "+o);
//					instancesWithoutOrder.remove(o.getInstanceId());
//					i++;
//				}
//				
//				i = 1;
//				for(String instanceMissing : instancesWithoutOrder){
//					LOGGER.info("<"+managerId+">: <QuotaExceeded> instance missing num "+i+" - "+instanceMissing);
//					i++;
//				}
//				
//				
//				LOGGER.info("<"+managerId+">: ########");				
//			}
			
			throw new OCCIException(ErrorType.QUOTA_EXCEEDED, "<"+managerId+">: "+"There is no more quota in the underlying cloud.");
		}					
				
		String name = "instance"+instances.size();
		name += "-"+ String.valueOf(UUID.randomUUID());
		instances.add(name);
		
		if(instances.size()>quota){
			LOGGER.info("<"+managerId+">: Existing instances("+instances.size()+"): "+instances+". Created instance: "+name+".");
		}
		
		return name;
	}
	
	public void setManager(ManagerControllerXP manager) {
		this.manager = manager;
	}
	
	@Override
	public synchronized Instance getInstance(Token token, String instanceId) {
		if(instances.contains(instanceId)){
			Instance i = new Instance(instanceId);
			i.addAttribute("occi.compute.cores", "8");
			i.addAttribute("occi.compute.memory", "16");
			return i;
		}
		LOGGER.info("<"+managerId+">: Existing instances: "+instances+". Requested instance: "+instanceId);//debug-EDUARDO
		return null;
	}	

	@Override
	public synchronized void removeInstance(Token token, String instanceId) {	
		if(instanceId!=null){
			boolean success = instances.remove(instanceId);
			if(success)
				LOGGER.info("<"+managerId+">: FakeCloudComputePlugin removing instance (" + instanceId + ").");
			else
				LOGGER.info("<"+managerId+">: FakeCloudComputePlugin tried to remove instance (" + instanceId + ") "
						+ "but it doesn't exist.");
		}
	}
	
	public int getQuota(){
		return quota;
	}
	
	public synchronized int getFreeQuota(){
			return quota-instances.size();
	}

	/**
	 * Here we have to return the quota of the cloud.
	 */
	@Override
	public ResourcesInfo getResourcesInfo(Token token) {
		//instancesInUse + instancesIdle must be equal to quota
		ResourcesInfo ri = new ResourcesInfo();
		ri.setInstancesIdle("0");
		ri.setInstancesInUse(String.valueOf(quota));
		return ri;
	}

	/**
	 * The methods above do not interfere in simulation lane. 
	 */
	@Override
	public void removeInstances(Token token) {
		/**
		 * FIXME
		 * Usado apenas em testes.
		 */		
	}
	
	
	public synchronized List<String> getInstances() {
		LOGGER.info("<"+managerId+">: FakeCloudComputePlugin number of instances " + instances.size() + ".");
		return instances;
	}
	
	@Override
	public List<Instance> getInstances(Token token) {
		/**
		 * FIXME
		 * Usado apenas em testes (getAllFogbowFederationInstances).
		 */
		return null;
	}
	
	@Override
	public void bypass(Request request, Response response) {		
	}

	@Override
	public void uploadImage(Token token, String imagePath, String imageName,
			String diskFormat) {
	}

	@Override
	public String getImageId(Token token, String imageName) {
		return null;
	}

	@Override
	public ImageState getImageState(Token token, String imageName) {
		return null;
	}

	@Override
	public String attach(Token token, List<Category> categories,
			Map<String, String> xOCCIAtt) {
		return null;
	}

	@Override
	public void dettach(Token token, List<Category> categories,
			Map<String, String> xOCCIAtt) {		
	}

}

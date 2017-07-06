package org.fogbowcloud.manager.core.plugins.compute.fake;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.model.ImageState;
import org.fogbowcloud.manager.core.model.ResourcesInfo;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.benchmarking.VanillaBenchmarkingPlugin;
import org.fogbowcloud.manager.occi.instance.Instance;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.model.ErrorType;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.occi.order.OrderAttribute;
import org.restlet.Request;
import org.restlet.Response;

public class FakeCloudComputePlugin implements ComputePlugin {

	public static final String COMPUTE_FAKE_QUOTA = "compute_fake_quota";
	private static final Logger LOGGER = Logger.getLogger(FakeCloudComputePlugin.class);
	
	private int quota;	
	private int instanceCounter = 0;
	private List<String> instances = new ArrayList<String>();
	
	private String managerId;
	
	public FakeCloudComputePlugin(Properties properties){
		quota = Integer.parseInt(properties.getProperty(COMPUTE_FAKE_QUOTA));
		managerId = properties.getProperty(ConfigurationConstants.XMPP_JID_KEY);
	}
	
	@Override
	public String requestInstance(Token token, List<Category> categories,
			Map<String, String> xOCCIAtt, String imageId) {
		if(instanceCounter>=quota)
			throw new OCCIException(ErrorType.QUOTA_EXCEEDED, "<"+managerId+">: "+"There is no more quota in the underlying cloud.");
		
		String name = "instance"+(++instanceCounter);
		name += "-"+ String.valueOf(UUID.randomUUID());
		instances.add(name);
		
		return name;
	}
	
	@Override
	public Instance getInstance(Token token, String instanceId) {
		if(instances.contains(instanceId)){
			Instance i = new Instance(instanceId);
			i.addAttribute("occi.compute.cores", "8");
			i.addAttribute("occi.compute.memory", "16");
			return i;
		}
		LOGGER.warn("Existing instances: "+instances);//debug-EDUARDO
		LOGGER.warn("Requested instance: "+instanceId);//debug-EDUARDO
		return null;
	}	

	@Override
	public void removeInstance(Token token, String instanceId) {
		instanceCounter--;
		instances.remove(instanceId);		
	}
	
	public int getQuota(){
		return quota;
	}
	
	public int getFreeQuota(){
		return quota-instanceCounter;
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

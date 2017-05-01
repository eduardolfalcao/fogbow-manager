package org.fogbowcloud.manager.core.plugins.compute.fake;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.model.ImageState;
import org.fogbowcloud.manager.core.model.ResourcesInfo;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.occi.instance.Instance;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.model.ErrorType;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.fogbowcloud.manager.occi.model.Token;
import org.restlet.Request;
import org.restlet.Response;

public class FakeCloudComputePlugin implements ComputePlugin {

	public static final String COMPUTE_FAKE_QUOTA = "compute_fake_quota";
	
	private int quota;	
	private int instanceCounter = 0;
	private List<String> instances = new ArrayList<String>();
	
	public FakeCloudComputePlugin(Properties properties){
		quota = Integer.parseInt(properties.getProperty(COMPUTE_FAKE_QUOTA));
	}
	
	@Override
	public String requestInstance(Token token, List<Category> categories,
			Map<String, String> xOCCIAtt, String imageId) {
		if(instanceCounter>=quota)
			throw new OCCIException(ErrorType.QUOTA_EXCEEDED, "There is no more quota in the underlying cloud.");
		String name = "instance"+(++instanceCounter);
		instances.add(name);
		return name;
	}
	
	@Override
	public Instance getInstance(Token token, String instanceId) {
		if(instances.contains(instanceId))
			return new Instance(instanceId);
		return null;
	}	

	@Override
	public void removeInstance(Token token, String instanceId) {
		instances.remove(instanceId);		
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

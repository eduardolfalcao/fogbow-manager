package org.fogbowcloud.manager.core.plugins.compute.fake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.model.ImageState;
import org.fogbowcloud.manager.core.model.ResourcesInfo;
import org.fogbowcloud.manager.occi.instance.Instance;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestFakeCloudComputePlugin {

	private FakeCloudComputePlugin fakeCloudComputePlugin;

	@Before
	public void setUp() {
		Properties properties = new Properties();
		properties.put(FakeCloudComputePlugin.COMPUTE_FAKE_QUOTA, "2");
		properties.put(ConfigurationConstants.XMPP_JID_KEY, "managerId");
		this.fakeCloudComputePlugin = new FakeCloudComputePlugin(properties);
	}
	
	@Test
	public void testRequestNoInstances() {
		Assert.assertEquals(0, fakeCloudComputePlugin.getInstances().size());
		Assert.assertEquals(2, fakeCloudComputePlugin.getFreeQuota());
		Assert.assertEquals(2, fakeCloudComputePlugin.getQuota());		
	}
	
	@Test
	public void testRequestOneInstance() {
		fakeCloudComputePlugin.requestInstance(null, null, null, "image");
		Assert.assertEquals(1, fakeCloudComputePlugin.getInstances().size());
		Assert.assertEquals(1, fakeCloudComputePlugin.getFreeQuota());
		Assert.assertEquals(2, fakeCloudComputePlugin.getQuota());		
	}
	
	@Test
	public void testRequestAllFreeQuota() {
		fakeCloudComputePlugin.requestInstance(null, null, null, "image");		
		fakeCloudComputePlugin.requestInstance(null, null, null, "image");
		Assert.assertEquals(2, fakeCloudComputePlugin.getInstances().size());
		Assert.assertEquals(0, fakeCloudComputePlugin.getFreeQuota());
		Assert.assertEquals(2, fakeCloudComputePlugin.getQuota());		
	}
	
	@Test(expected = OCCIException.class)
	public void testRequestMoreThanFreeQuota() {
		fakeCloudComputePlugin.requestInstance(null, null, null, "image");		
		fakeCloudComputePlugin.requestInstance(null, null, null, "image");
		fakeCloudComputePlugin.requestInstance(null, null, null, "image");
		Assert.assertEquals(2, fakeCloudComputePlugin.getInstances().size());
		Assert.assertEquals(0, fakeCloudComputePlugin.getFreeQuota());
		Assert.assertEquals(2, fakeCloudComputePlugin.getQuota());		
	}
	
	@Test
	public void testGetInstance() {
		String id = fakeCloudComputePlugin.requestInstance(null, null, null, "image");
		Assert.assertEquals(id, fakeCloudComputePlugin.getInstance(null, id).getId());
	}
	
	@Test
	public void testAttrsGetInstance() {
		String id = fakeCloudComputePlugin.requestInstance(null, null, null, "image");
		Instance i = fakeCloudComputePlugin.getInstance(null, id);
		Assert.assertEquals("8", i.getAttributes().get("occi.compute.cores"));
		Assert.assertEquals("16", i.getAttributes().get("occi.compute.memory"));
	}
	
	@Test
	public void testGetInstanceNullParams() {
		fakeCloudComputePlugin.requestInstance(null, null, null, "image");
		Assert.assertNull(fakeCloudComputePlugin.getInstance(null, null));
	}
	
	@Test
	public void testRemoveInstance() {
		String id = fakeCloudComputePlugin.requestInstance(null, null, null, "image");
		Assert.assertEquals(1, fakeCloudComputePlugin.getInstances().size());
		Assert.assertEquals(1, fakeCloudComputePlugin.getFreeQuota());
		Assert.assertEquals(2, fakeCloudComputePlugin.getQuota());
		fakeCloudComputePlugin.removeInstance(null, id);
		Assert.assertEquals(0, fakeCloudComputePlugin.getInstances().size());
		Assert.assertEquals(2, fakeCloudComputePlugin.getFreeQuota());
		Assert.assertEquals(2, fakeCloudComputePlugin.getQuota());		
	}
	
	@Test
	public void testRemoveInstanceNullParams() {
		fakeCloudComputePlugin.requestInstance(null, null, null, "image");
		Assert.assertEquals(1, fakeCloudComputePlugin.getInstances().size());
		Assert.assertEquals(1, fakeCloudComputePlugin.getFreeQuota());
		Assert.assertEquals(2, fakeCloudComputePlugin.getQuota());
		fakeCloudComputePlugin.removeInstance(null, null);
		Assert.assertEquals(1, fakeCloudComputePlugin.getInstances().size());
		Assert.assertEquals(1, fakeCloudComputePlugin.getFreeQuota());
		Assert.assertEquals(2, fakeCloudComputePlugin.getQuota());		
	}
	
	@Test
	public void testRemoveInstanceWithOrder() {
		String id = fakeCloudComputePlugin.requestInstance(null, null, null, "image");
		Assert.assertEquals(1, fakeCloudComputePlugin.getInstances().size());
		Assert.assertEquals(1, fakeCloudComputePlugin.getFreeQuota());
		Assert.assertEquals(2, fakeCloudComputePlugin.getQuota());
		Order o = new Order("", null, id, "", "", 0, false, OrderState.OPEN, new ArrayList(), new HashMap());
		fakeCloudComputePlugin.removeInstance(null, id);
		Assert.assertEquals(0, fakeCloudComputePlugin.getInstances().size());
		Assert.assertEquals(2, fakeCloudComputePlugin.getFreeQuota());
		Assert.assertEquals(2, fakeCloudComputePlugin.getQuota());
	}
	
	@Test
	public void testRemoveInstanceWithOrderNullParams() {
		fakeCloudComputePlugin.requestInstance(null, null, null, "image");
		Assert.assertEquals(1, fakeCloudComputePlugin.getInstances().size());
		Assert.assertEquals(1, fakeCloudComputePlugin.getFreeQuota());
		Assert.assertEquals(2, fakeCloudComputePlugin.getQuota());
		fakeCloudComputePlugin.removeInstance(null, null);
		Assert.assertEquals(1, fakeCloudComputePlugin.getInstances().size());
		Assert.assertEquals(1, fakeCloudComputePlugin.getFreeQuota());
		Assert.assertEquals(2, fakeCloudComputePlugin.getQuota());
	}
	
	@Test
	public void testGetQuotas(){
		Assert.assertEquals(2, fakeCloudComputePlugin.getFreeQuota());
		Assert.assertEquals(2, fakeCloudComputePlugin.getQuota());
	}
	
	@Test
	public void testGetResourcesInfo(){
		ResourcesInfo ri = fakeCloudComputePlugin.getResourcesInfo(null);
		Assert.assertEquals("0", ri.getInstancesIdle());
		Assert.assertEquals("2", ri.getInstancesInUse());
	}
	
	@Test
	public void testgetInstances() {
		fakeCloudComputePlugin.requestInstance(null, null, null, "image");		
		fakeCloudComputePlugin.requestInstance(null, null, null, "image");
		Assert.assertEquals(2, fakeCloudComputePlugin.getInstances().size());		
	}
}

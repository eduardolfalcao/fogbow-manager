package org.fogbowcloud.manager.core.plugins.compute.cloudstack;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.http.ProtocolVersion;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicStatusLine;
import org.fogbowcloud.manager.core.model.ResourcesInfo;
import org.fogbowcloud.manager.core.plugins.identity.cloudstack.CloudStackHelper;
import org.fogbowcloud.manager.core.plugins.util.HttpClientWrapper;
import org.fogbowcloud.manager.core.plugins.util.HttpResponseWrapper;
import org.fogbowcloud.manager.occi.instance.Instance;
import org.fogbowcloud.manager.occi.instance.InstanceState;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.occi.util.PluginHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestCloudStackComputePlugin {

	private static String ONE_INSTANCE;
	private static final String NO_INSTANCE = "{ \"listvirtualmachinesresponse\" : {}}";
	private static String RESOURCES_INFO;
	private static final String COMPUTE_DEFAULT_ZONE = "root";
	private static final String CLOUDSTACK_URL = "http://localhost:8080/client/api";
	private static final String OS_TYPE = "Linux";
	private static final String IMAGE_DOWNLOADED_BASE_URL = "http://127.0.0.1";
	private static final String IMAGE_DOWNLOADED_BASE_PATH = "/var/www";
	private static final String ZONE_ID = "zoneId";

	@Before
	public void setUp() throws IOException {
		ONE_INSTANCE = PluginHelper
				.getContentFile("src/test/resources/cloudstack/response.one_instance");
		RESOURCES_INFO = PluginHelper
				.getContentFile("src/test/resources/cloudstack/response.resources_info");
	}

	private CloudStackComputePlugin createPlugin(HttpClientWrapper httpClient) {
		Properties properties = new Properties();
		properties.put("compute_cloudstack_api_url", CLOUDSTACK_URL);
		properties.put("compute_cloudstack_default_zone", COMPUTE_DEFAULT_ZONE);
		properties.put("compute_cloudstack_image_download_base_path",
				CLOUDSTACK_URL);
		properties.put("compute_cloudstack_image_download_base_path", IMAGE_DOWNLOADED_BASE_PATH);
		properties.put("compute_cloudstack_image_download_os_type_id", OS_TYPE);
		properties.put("compute_cloudstack_image_download_base_url", IMAGE_DOWNLOADED_BASE_URL);
		properties.put("compute_cloudstack_zone_id", ZONE_ID);
		
		if (httpClient == null) {
			return new CloudStackComputePlugin(properties);
		} else {
			return new CloudStackComputePlugin(properties, httpClient);
		}
	}

	private HttpClientWrapper createHttpClientWrapperMock(Token token,
			Map<String[], String> commandResponse, String requestType) {
		HttpClientWrapper httpClient = Mockito.mock(HttpClientWrapper.class);
		ProtocolVersion proto = new ProtocolVersion("HTTP", 1, 1);
		for (Entry<String[], String> entry : commandResponse.entrySet()) {
			String command = entry.getKey()[0];
			String response = entry.getValue();
			URIBuilder uriBuilder = CloudStackComputePlugin.createURIBuilder(
					CLOUDSTACK_URL, command);
			if (entry.getKey().length > 0) {
				String[] parameters = entry.getKey();
				for (int i = 1; i < parameters.length; i++) {
					String parameter = parameters[i];
					uriBuilder.addParameter(parameter.split(" ")[0],
							parameter.split(" ")[1]);
				}
			}
			CloudStackHelper.sign(uriBuilder, token.getAccessId());
			HttpResponseWrapper returned = new HttpResponseWrapper(
					new BasicStatusLine(proto, 200, "test reason"), response);
			if (requestType.equals("get")) {
				Mockito.when(httpClient.doGet(uriBuilder.toString()))
						.thenReturn(returned);
			} else {
				Mockito.when(httpClient.doPost(uriBuilder.toString()))
						.thenReturn(returned);
			}
		}
		return httpClient;
	}

	@Test
	public void testGetInstances() {
		Token token = new Token("api:key", null, null, null);
		Map<String[], String> commandResponse = new HashMap<String[], String>();
		String[] commands = new String[1];
		commands[0] = CloudStackComputePlugin.LIST_VMS_COMMAND;
		commandResponse.put(commands, ONE_INSTANCE);
		HttpClientWrapper httpClient = createHttpClientWrapperMock(token,
				commandResponse, "get");
		CloudStackComputePlugin cscp = createPlugin(httpClient);
		List<Instance> instances = cscp.getInstances(token);
		Assert.assertEquals(1, instances.size());
		Assert.assertEquals(InstanceState.RUNNING, instances.get(0).getState());
		commandResponse.put(commands, NO_INSTANCE);
		httpClient = createHttpClientWrapperMock(token, commandResponse, "get");
		cscp = createPlugin(httpClient);
		instances = cscp.getInstances(token);
		Assert.assertEquals(0, instances.size());
	}

	@Test
	public void testGetInstance() {
		String vmId = "50b2b99a-8215-4437-9dfe-17382242e08c";
		Token token = new Token("api:key", null, null, null);
		Map<String[], String> commandResponse = new HashMap<String[], String>();
		String[] commands = new String[2];
		commands[0] = CloudStackComputePlugin.LIST_VMS_COMMAND;
		commands[1] = CloudStackComputePlugin.VM_ID + " " + vmId;
		commandResponse.put(commands, ONE_INSTANCE);
		HttpClientWrapper httpClient = createHttpClientWrapperMock(token,
				commandResponse, "get");
		CloudStackComputePlugin cscp = createPlugin(httpClient);
		Instance instance = cscp.getInstance(token, vmId);
		Assert.assertEquals(vmId, instance.getId());
		commandResponse.put(commands, NO_INSTANCE);
		httpClient = createHttpClientWrapperMock(token, commandResponse, "get");
		cscp = createPlugin(httpClient);
		try {
			cscp.getInstance(token, vmId);
		} catch (OCCIException e) {
			return;
		}
		fail();
	}

	@Test
	public void testRemoveInstance() {
		String vmId = "50b2b99a-8215-4437-9dfe-17382242e08c";
		Token token = new Token("api:key", null, null, null);
		URIBuilder uriBuilder = CloudStackComputePlugin.createURIBuilder(
				CLOUDSTACK_URL, CloudStackComputePlugin.DESTROY_VM_COMMAND);
		uriBuilder.addParameter(CloudStackComputePlugin.VM_ID, vmId);
		CloudStackHelper.sign(uriBuilder, token.getAccessId());

		Map<String[], String> commandResponse = new HashMap<String[], String>();
		String[] commands1 = new String[1];
		commands1[0] = CloudStackComputePlugin.LIST_VMS_COMMAND;
		commandResponse.put(commands1, ONE_INSTANCE);
		HttpClientWrapper httpClient = createHttpClientWrapperMock(token,
				commandResponse, "get");

		CloudStackComputePlugin cscp = createPlugin(httpClient);
		cscp.removeInstances(token);
		Mockito.verify(httpClient).doPost(uriBuilder.toString());
	}

	@Test
	public void testGetResourceInfo() {
		Token token = new Token("api:key", null, null, null);
		Map<String[], String> commandResponse = new HashMap<String[], String>();
		String[] commands1 = new String[1];
		commands1[0] = CloudStackComputePlugin.LIST_VMS_COMMAND;
		commandResponse.put(commands1, ONE_INSTANCE);
		String[] commands2 = new String[1];
		commands2[0] = CloudStackComputePlugin.LIST_RESOURCE_LIMITS_COMMAND;
		commandResponse.put(commands2, RESOURCES_INFO);
		HttpClientWrapper httpClient = createHttpClientWrapperMock(token,
				commandResponse, "get");
		CloudStackComputePlugin cscp = createPlugin(httpClient);
		ResourcesInfo ri = cscp.getResourcesInfo(token);
		Assert.assertEquals("1", ri.getInstancesIdle());
		Assert.assertEquals("512", ri.getMemInUse());
		Assert.assertEquals("1", ri.getCpuInUse());
	}

	@Test
	public void testGetResourcesWithNoInstances() {
		Token token = new Token("api:key", null, null, null);
		Map<String[], String> commandResponse = new HashMap<String[], String>();
		String[] commands1 = new String[1];
		commands1[0] = CloudStackComputePlugin.LIST_VMS_COMMAND;
		commandResponse.put(commands1, NO_INSTANCE);
		String[] commands2 = new String[1];
		commands2[0] = CloudStackComputePlugin.LIST_RESOURCE_LIMITS_COMMAND;
		commandResponse.put(commands2, RESOURCES_INFO);
		HttpClientWrapper httpClient = createHttpClientWrapperMock(token,
				commandResponse, "get");
		CloudStackComputePlugin cscp = createPlugin(httpClient);
		ResourcesInfo ri = cscp.getResourcesInfo(token);
		Assert.assertEquals("0", ri.getMemInUse());
		Assert.assertEquals("0", ri.getCpuInUse());
	}

	@Test
	public void testBypass() {
		CloudStackComputePlugin cscp = createPlugin(null);
		try {
			cscp.bypass(null, null);
		} catch (UnsupportedOperationException e) {
			return;
		}
		fail();
	}

	@Test
	public void testUploadImage() {
		String imageName = "name";
		String diskFormat = "format";
		String hypervisor = "KVM";
		String imagePath = "/var/www/cirros.img";
		Token token = new Token("api:key", null, null, null);
		Map<String[], String> commandResponse = new HashMap<String[], String>();
		String imageURL = imagePath.replace(IMAGE_DOWNLOADED_BASE_PATH, IMAGE_DOWNLOADED_BASE_URL + "/");
		String[] commands1 = new String[9];
		commands1[0] = CloudStackComputePlugin.REGISTER_TEMPLATE_COMMAND;
		commands1[1] = CloudStackComputePlugin.DISPLAY_TEXT + " "+ imageName;
		commands1[2] = CloudStackComputePlugin.FORMAT + " "+ diskFormat.toUpperCase();
		commands1[3] = CloudStackComputePlugin.HYPERVISOR + " "+ hypervisor;
		commands1[4] = CloudStackComputePlugin.NAME + " "+ imageName;
		commands1[5] = CloudStackComputePlugin.OS_TYPE_ID + " "+ OS_TYPE;
		commands1[6] = CloudStackComputePlugin.ZONE_ID + " "+ ZONE_ID;
		commands1[7] = CloudStackComputePlugin.URL + " "+  imageURL;
		commands1[8] = CloudStackComputePlugin.IS_PUBLIC + " "
				+ Boolean.TRUE.toString();
		commandResponse.put(commands1, "response");
		HttpClientWrapper httpClient = createHttpClientWrapperMock(token,
				commandResponse, "post");
		CloudStackComputePlugin cscp = createPlugin(httpClient);
		cscp.uploadImage(token, imagePath, imageName, diskFormat);
	}

}

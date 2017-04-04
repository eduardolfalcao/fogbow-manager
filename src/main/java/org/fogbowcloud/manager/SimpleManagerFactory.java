package org.fogbowcloud.manager;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.BenchmarkingPlugin;
import org.fogbowcloud.manager.core.plugins.CapacityControllerPlugin;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.FederationMemberAuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.FederationMemberPickerPlugin;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.ImageStoragePlugin;
import org.fogbowcloud.manager.core.plugins.MapperPlugin;
import org.fogbowcloud.manager.core.plugins.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.PrioritizationPlugin;
import org.fogbowcloud.manager.core.plugins.StoragePlugin;
import org.fogbowcloud.manager.core.plugins.accounting.FCUAccountingPlugin;
import org.fogbowcloud.manager.core.plugins.accounting.SimpleStorageAccountingPlugin;
import org.fogbowcloud.manager.core.plugins.benchmarking.VanillaBenchmarkingPlugin;
import org.fogbowcloud.manager.core.plugins.capacitycontroller.satisfactiondriven.SatisfactionDrivenCapacityControllerPlugin;
import org.fogbowcloud.manager.core.plugins.imagestorage.http.HTTPDownloadImageStoragePlugin;
import org.fogbowcloud.manager.core.plugins.localcredentials.SingleMapperPlugin;
import org.fogbowcloud.manager.core.plugins.memberauthorization.DefaultMemberAuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.memberpicker.RoundRobinMemberPickerPlugin;
import org.fogbowcloud.manager.core.plugins.prioritization.TwoFoldPrioritizationPlugin;
import org.fogbowcloud.manager.occi.DataStoreHelper;
import org.fogbowcloud.manager.occi.ManagerDataStore;

public class SimpleManagerFactory {
	
	private static final Logger LOGGER = Logger.getLogger(SimpleManagerFactory.class);
	
	private static final String BASE_PLUGINS = "org.fogbowcloud.manager.core.plugins.";
	private static final String XMPP_JID_FREERIDER = "free-rider-manager";
	private static final String XMPP_JID_COOPERATIVE = "cooperative-manager";
	
	private static final String PATH_FOGBOW_MANAGER = System.getProperty("user.dir");
	private static final String PATH_DATASTORES = PATH_FOGBOW_MANAGER+"/experiments/datastores/";
	
	private static final int HTTP_PORT = 9192;
	
	
	public static Properties adjustPropertiesManager(int id, boolean freeRider, Properties properties){
		
		Properties prop = (Properties) properties.clone();
		
		String managerId = "", memberPickerPlugin = "", capacityControllerPlugin = "";		
		if(freeRider){
			managerId = XMPP_JID_FREERIDER+id;
			memberPickerPlugin = BASE_PLUGINS+"memberpicker.RoundRobinMemberPickerPlugin";
			capacityControllerPlugin = BASE_PLUGINS+"capacitycontroller.freerider.FreeRiderCapacityControllerPlugin";
			prop.put(ConfigurationConstants.COMPUTE_ACCOUNTING_PLUGIN_CLASS_KEY, BASE_PLUGINS+"compute.nocloud.NoCloudComputePlugin");
		}
		else{
			managerId = XMPP_JID_COOPERATIVE+id;
			memberPickerPlugin = BASE_PLUGINS+"memberpicker.NoFMemberPickerPlugin";
			prop.put(ConfigurationConstants.CAPACITY_CONTROLLER_PLUGIN_CLASS, BASE_PLUGINS+"capacitycontroller.fairnessdriven.TwoFoldCapacityController");
			prop.put(ConfigurationConstants.COMPUTE_ACCOUNTING_PLUGIN_CLASS_KEY, BASE_PLUGINS+"compute.fake.FakeCloudComputePlugin");
		}
		
		prop.put(ConfigurationConstants.XMPP_JID_KEY, managerId);
		prop.put(ConfigurationConstants.MEMBER_PICKER_PLUGIN_CLASS_KEY, memberPickerPlugin);
		prop.put(ConfigurationConstants.CAPACITY_CONTROLLER_PLUGIN_CLASS, capacityControllerPlugin);
		
		
		String accountingDatastoreUrl = PATH_DATASTORES+"accounting-"+managerId+".sqlite";
		String managerDatastoreUrl = PATH_DATASTORES+"manager-"+managerId+".sqlite";
		String instanceDatastoreUrl = PATH_DATASTORES+"instance-"+managerId+".sqlite";
		String storageDatastoreUrl = PATH_DATASTORES+"storage-"+managerId+".sqlite";
		String networkDatastoreUrl = PATH_DATASTORES+"network-"+managerId+".sqlite";
		
		prop.put(FCUAccountingPlugin.ACCOUNTING_DATASTORE_URL, DataStoreHelper.PREFIX_DATASTORE_URL+accountingDatastoreUrl);
		prop.put(ManagerDataStore.MANAGER_DATASTORE_URL, DataStoreHelper.PREFIX_DATASTORE_URL+managerDatastoreUrl);
		prop.put("instance_datastore_url", DataStoreHelper.PREFIX_DATASTORE_URL+instanceDatastoreUrl);
		prop.put("storage_datastore_url", DataStoreHelper.PREFIX_DATASTORE_URL+storageDatastoreUrl);
		prop.put("network_datastore_url", DataStoreHelper.PREFIX_DATASTORE_URL+networkDatastoreUrl);
		
		prop.put("http_port", HTTP_PORT+id);		
		
		return prop;
	}
	
	public static ManagerController createFM(Properties properties){
		
		ComputePlugin computePlugin = null;
		try {
			computePlugin = (ComputePlugin) MainHelper.createInstance(
					ConfigurationConstants.COMPUTE_CLASS_KEY, properties);
		} catch (Exception e) {
			LOGGER.warn("Compute Plugin not specified in the properties.", e);
			System.exit(MainHelper.EXIT_ERROR_CODE);
		}

		AuthorizationPlugin authorizationPlugin = null;
		try {
			authorizationPlugin = (AuthorizationPlugin) MainHelper.createInstance(
					ConfigurationConstants.AUTHORIZATION_CLASS_KEY, properties);
		} catch (Exception e) {
			LOGGER.warn("Authorization Plugin not especified in the properties.", e);
			System.exit(MainHelper.EXIT_ERROR_CODE);
		}
		
		IdentityPlugin localIdentityPlugin = null;
		try {
			localIdentityPlugin = (IdentityPlugin) MainHelper.getIdentityPluginByPrefix(properties,
					ConfigurationConstants.LOCAL_PREFIX);
		} catch (Exception e) {
			LOGGER.warn("Local Identity Plugin not especified in the properties.", e);
			System.exit(MainHelper.EXIT_ERROR_CODE);
		}
		
		IdentityPlugin federationIdentityPlugin = null;
		try {
			federationIdentityPlugin = (IdentityPlugin) MainHelper.getIdentityPluginByPrefix(properties,
					ConfigurationConstants.FEDERATION_PREFIX);
		} catch (Exception e) {
			LOGGER.warn("Federation Identity Plugin not especified in the properties.", e);
			System.exit(MainHelper.EXIT_ERROR_CODE);
		}

		FederationMemberAuthorizationPlugin validator = new DefaultMemberAuthorizationPlugin(properties);
		try {
			validator = (FederationMemberAuthorizationPlugin) MainHelper.createInstance(
					ConfigurationConstants.MEMBER_VALIDATOR_CLASS_KEY, properties);
		} catch (Exception e) {
			LOGGER.warn("Member Validator not especified in the properties.");
			System.exit(MainHelper.EXIT_ERROR_CODE);
		}
		
		if (properties.get(ConfigurationConstants.RENDEZVOUS_JID_KEY) == null
				|| properties.get(ConfigurationConstants.RENDEZVOUS_JID_KEY).toString().isEmpty()) {
			LOGGER.warn("Rendezvous (" + ConfigurationConstants.RENDEZVOUS_JID_KEY
					+ ") not especified in the properties.");
		}
		
		ImageStoragePlugin imageStoragePlugin = null;
		try {
			imageStoragePlugin = (ImageStoragePlugin) MainHelper.createInstanceWithComputePlugin(
					ConfigurationConstants.IMAGE_STORAGE_PLUGIN_CLASS, properties, computePlugin);
		} catch (Exception e) {
			imageStoragePlugin = new HTTPDownloadImageStoragePlugin(properties, computePlugin);
			LOGGER.warn("Image Storage plugin not specified in properties. Using the default one.", e);
		}
				
		BenchmarkingPlugin benchmarkingPlugin = null;
		try {
			benchmarkingPlugin = (BenchmarkingPlugin) MainHelper.createInstance(
					ConfigurationConstants.BENCHMARKING_PLUGIN_CLASS_KEY, properties);
		} catch (Exception e) {
			benchmarkingPlugin = new VanillaBenchmarkingPlugin(properties);
			LOGGER.warn("Benchmarking plugin not specified in properties. Using the default one.", e);
		}
				
		AccountingPlugin computeAccountingPlugin = null;
		try {
			computeAccountingPlugin = (AccountingPlugin) MainHelper.createInstanceWithBenchmarkingPlugin(
					ConfigurationConstants.COMPUTE_ACCOUNTING_PLUGIN_CLASS_KEY, properties, benchmarkingPlugin);
		} catch (Exception e) {
			computeAccountingPlugin = new FCUAccountingPlugin(properties, benchmarkingPlugin);
			LOGGER.warn("Accounting plugin (compute) not specified in properties. Using the default one.", e);
		}
		
		AccountingPlugin storageccountingPlugin = null;
		try {
			storageccountingPlugin = (AccountingPlugin) MainHelper.createInstance(
					ConfigurationConstants.STORAGE_ACCOUNTING_PLUGIN_CLASS_KEY, properties);
		} catch (Exception e) {
			storageccountingPlugin = new SimpleStorageAccountingPlugin(properties);
			LOGGER.warn("Accounting plugin (storage) not specified in properties. Using the default one.", e);
		}		
		
		FederationMemberPickerPlugin memberPickerPlugin = null;
		try {
			memberPickerPlugin = (FederationMemberPickerPlugin) MainHelper.createInstanceWithAccountingPlugin(
					ConfigurationConstants.MEMBER_PICKER_PLUGIN_CLASS_KEY, properties,
					computeAccountingPlugin);
		} catch (Exception e) {
			memberPickerPlugin = new RoundRobinMemberPickerPlugin(properties, computeAccountingPlugin);
			LOGGER.warn("Member picker plugin not specified in properties. Using the default one.", e);
		}
		
		MapperPlugin mapperPlugin = null;
		try {
			mapperPlugin = (MapperPlugin) MainHelper.createInstance(
					ConfigurationConstants.LOCAL_CREDENTIALS_CLASS_KEY, properties);
		} catch (Exception e) {
			mapperPlugin = new SingleMapperPlugin(properties);
			LOGGER.warn("Federation user credential plugin not specified in properties. Using the default one.", e);
		}		
		
		StoragePlugin storagePlugin = null;
		try {
			storagePlugin = (StoragePlugin) MainHelper.createInstance(
					ConfigurationConstants.STORAGE_CLASS_KEY, properties);
		} catch (Exception e) {
			LOGGER.warn("Storage Plugin not especified in the properties.", e);
			//System.exit(MainHelper.EXIT_ERROR_CODE);
		}
			
		
		NetworkPlugin networkPlugin = null;
		try {
			networkPlugin = (NetworkPlugin) MainHelper.createInstance(
					ConfigurationConstants.NETWORK_CLASS_KEY, properties);
		} catch (Exception e) {
			LOGGER.warn("Network Plugin not especified in the properties.", e);
			//System.exit(MainHelper.EXIT_ERROR_CODE);
		}		

		
		String occiExtraResourcesPath = properties
				.getProperty(ConfigurationConstants.OCCI_EXTRA_RESOURCES_KEY_PATH);
		if (occiExtraResourcesPath != null && !occiExtraResourcesPath.isEmpty()) {
			if (properties.getProperty(ConfigurationConstants.INSTANCE_DATA_STORE_URL) == null) {
				LOGGER.error("If OCCI extra resources was set for supporting post-compute, you must also set instance datastore property ("
						+ ConfigurationConstants.INSTANCE_DATA_STORE_URL + ").");
				System.exit(MainHelper.EXIT_ERROR_CODE);
			}
		}
		
		PrioritizationPlugin prioritizationPlugin = null;
		try {
			prioritizationPlugin = (PrioritizationPlugin) MainHelper.createInstanceWithAccountingPlugin(
					ConfigurationConstants.PRIORITIZATION_PLUGIN_CLASS, properties, computeAccountingPlugin);
		} catch (Exception e) {
			LOGGER.warn("Prioritization Plugin not especified in the properties.", e);
			System.exit(MainHelper.EXIT_ERROR_CODE);
		}		
		
		CapacityControllerPlugin capacityControllerPlugin = null;
		try {
			capacityControllerPlugin = (CapacityControllerPlugin) MainHelper.createInstanceWithAccountingPlugin(
					ConfigurationConstants.CAPACITY_CONTROLLER_PLUGIN_CLASS, properties, computeAccountingPlugin);
		} catch (Exception e) {
			capacityControllerPlugin = new SatisfactionDrivenCapacityControllerPlugin();
			LOGGER.warn("Capacity Controller plugin not specified in properties. Using the default one.", e);
		}

		ManagerController fm = new ManagerController(properties);
		fm.setComputePlugin(computePlugin);
		fm.setAuthorizationPlugin(authorizationPlugin);
		fm.setLocalIdentityPlugin(localIdentityPlugin);
		fm.setFederationIdentityPlugin(federationIdentityPlugin);
		fm.setImageStoragePlugin(imageStoragePlugin);
		fm.setValidator(validator);
		fm.setBenchmarkingPlugin(benchmarkingPlugin);
		fm.setComputeAccountingPlugin(computeAccountingPlugin);
		fm.setStorageAccountingPlugin(storageccountingPlugin);
		fm.setMemberPickerPlugin(memberPickerPlugin);
		fm.setPrioritizationPlugin(prioritizationPlugin);
		fm.setLocalCredentailsPlugin(mapperPlugin);
		fm.setStoragePlugin(storagePlugin);
		fm.setCapacityControllerPlugin(capacityControllerPlugin);
		fm.setNetworkPlugin(networkPlugin);
		
		return fm;
	}

}

package org.fogbowcloud.manager.core;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.BenchmarkingPlugin;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.MapperPlugin;
import org.fogbowcloud.manager.core.plugins.StoragePlugin;
import org.fogbowcloud.manager.core.plugins.capacitycontroller.satisfactiondriven.SatisfactionDrivenCapacityControllerPlugin;
import org.fogbowcloud.manager.core.plugins.compute.fake.FakeCloudComputePlugin;
import org.fogbowcloud.manager.core.plugins.memberauthorization.DefaultMemberAuthorizationPlugin;
import org.fogbowcloud.manager.core.util.DefaultDataTestHelper;
import org.fogbowcloud.manager.occi.instance.Instance;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.xmpp.ManagerXmppComponent;
import org.mockito.Mockito;

public class ManagerTestHelperXP extends ManagerTestHelper {

	@Override
	public ManagerXmppComponent initializeXMPPManagerComponent(boolean init) throws Exception {

		Properties properties = new Properties();
		properties.put(ConfigurationConstants.XMPP_JID_KEY, MANAGER_TEST_JID);
		properties.put(ConfigurationConstants.TOKEN_HOST_PRIVATE_ADDRESS_KEY,
				DefaultDataTestHelper.SERVER_HOST);
		properties.put(ConfigurationConstants.TOKEN_HOST_HTTP_PORT_KEY,
				String.valueOf(DefaultDataTestHelper.TOKEN_SERVER_HTTP_PORT));
		properties.put("max_whoisalive_manager_count", MAX_WHOISALIVE_MANAGER_COUNT);
		
		properties.put(FakeCloudComputePlugin.COMPUTE_FAKE_QUOTA, "10");
		
		ManagerController managerFacade = Mockito.spy(new ManagerControllerXP(properties, null));
		
		return initializeXMPPManagerComponent(init, managerFacade);
	}
	
	@SuppressWarnings("unchecked")
	public ManagerXmppComponent initializeXMPPManagerComponent(boolean init, ManagerController managerFacade) throws Exception {
		
		this.storagePlugin = Mockito.mock(StoragePlugin.class);
		
		this.computePlugin = Mockito.spy( new FakeCloudComputePlugin(managerFacade.getProperties()));
		
		this.identityPlugin = Mockito.mock(IdentityPlugin.class);
		this.federationIdentityPlugin = Mockito.mock(IdentityPlugin.class);
		this.benchmarkingPlugin = Mockito.mock(BenchmarkingPlugin.class);
		this.computeAccountingPlugin = Mockito.mock(AccountingPlugin.class);
		this.mapperPlugin = Mockito.mock(MapperPlugin.class);
		this.authorizationPlugin = Mockito.mock(AuthorizationPlugin.class);
		
		Mockito.when(computePlugin.getResourcesInfo(Mockito.any(Token.class))).thenReturn(
				getResources());
		Mockito.when(mapperPlugin.getAllLocalCredentials()).thenReturn(
				this.defaultFederationAllUsersCrendetials);
		Mockito.when(identityPlugin.createToken(Mockito.anyMap())).thenReturn(
				defaultFederationToken);		
		
		// mocking benchmark executor
		ExecutorService benchmarkExecutor = new CurrentThreadExecutorService();
				
		managerFacade.setComputePlugin(computePlugin);
		managerFacade.setComputeAccountingPlugin(computeAccountingPlugin);
		managerFacade.setLocalIdentityPlugin(identityPlugin);
		managerFacade.setBenchmarkExecutor(benchmarkExecutor);
		managerFacade.setBenchmarkingPlugin(benchmarkingPlugin);
		managerFacade.setFederationIdentityPlugin(federationIdentityPlugin);
		managerFacade.setAuthorizationPlugin(authorizationPlugin);
		managerFacade.setValidator(new DefaultMemberAuthorizationPlugin(null));
		managerFacade.setLocalCredentailsPlugin(mapperPlugin);
		managerFacade.setStoragePlugin(storagePlugin);
		managerFacade.setCapacityControllerPlugin(new SatisfactionDrivenCapacityControllerPlugin());
				
		managerXmppComponent = Mockito.spy(new ManagerXmppComponent(LOCAL_MANAGER_COMPONENT_URL,
				MANAGER_COMPONENT_PASS, SERVER_HOST, SERVER_COMPONENT_PORT, managerFacade, DEFAULT_XMPP_TIMEOUT));
				
		managerXmppComponent.setDescription("Manager Component");
		managerXmppComponent.setName("Manager");
		managerXmppComponent.setRendezvousAddress(CLIENT_ADRESS + SMACK_ENDING);
		fakeServer.connect(managerXmppComponent);
		managerXmppComponent.process();
		if (init) {
			managerXmppComponent.init();
		}
		managerFacade.setPacketSender(managerXmppComponent);
		return managerXmppComponent;
	}	
}

package org.fogbowcloud.manager.core.plugins.localcredentails;

import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.LocalCredentialsPlugin;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.occi.request.Request;

public class FederationUserBasedLocalCrendentialsPlugin implements LocalCredentialsPlugin {

	private Properties properties;
	private IdentityPlugin federationIdentityPlugin;
	
	public FederationUserBasedLocalCrendentialsPlugin(Properties properties) {
		this.properties = properties;
		
		federationIdentityPlugin = null;		
		try {
			federationIdentityPlugin = (IdentityPlugin) getIdentityPluginByPrefix(properties,
					ConfigurationConstants.FEDERATION_PREFIX);
		} catch (Exception e) {
			throw new IllegalArgumentException("Federation Identity Plugin not especified in the properties.", e);
		}
	}
	
	private Object getIdentityPluginByPrefix(Properties properties, String prefix)
			throws Exception {
		Properties pluginProperties = new Properties();
		for (Object keyObj : properties.keySet()) {
			String key = keyObj.toString();
			pluginProperties.put(key, properties.get(key));
			if (key.startsWith(prefix)) {
				String newKey = key.replace(prefix, "");
				pluginProperties.put(newKey, properties.get(key));
			}
		}
		return createInstance(prefix + ConfigurationConstants.IDENTITY_CLASS_KEY, pluginProperties);
	}

	private Object createInstance(String propName, Properties properties) throws Exception {
		return Class.forName(properties.getProperty(propName)).getConstructor(Properties.class)
				.newInstance(properties);
	}

	@Override
	public Map<String, String> getLocalCredentials(Request request) {
		String federationUser = request.getFederationToken().getUser();
		
		Map<String, String> credentialsPerMember = LocalCredentialsHelper
				.getCredentialsPerRelatedLocalName(this.properties, federationUser);
		if (!credentialsPerMember.isEmpty()) {
			return credentialsPerMember;
		}
		return LocalCredentialsHelper.getCredentialsPerRelatedLocalName(this.properties,
				LocalCredentialsHelper.FOGBOW_DEFAULTS);
	}

	@Override
	public Map<String, Map<String, String>> getAllLocalCredentials() {
		return LocalCredentialsHelper.getLocalCredentials(properties, null);
	}

	@Override
	public Map<String, String> getLocalCredentials(String accessId) {
		Token token = federationIdentityPlugin.getToken(accessId);
		
		String federationUser = token.getUser();		
		Map<String, String> credentialsPerMember = LocalCredentialsHelper
				.getCredentialsPerRelatedLocalName(this.properties, federationUser);
		if (!credentialsPerMember.isEmpty()) {
			return credentialsPerMember;
		}
		return LocalCredentialsHelper.getCredentialsPerRelatedLocalName(this.properties,
				LocalCredentialsHelper.FOGBOW_DEFAULTS);
		
	}
}

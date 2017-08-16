package org.fogbowcloud.manager.core.plugins.identity.nocloud;

import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import org.fogbowcloud.manager.occi.model.Token;

public class NoCloudIdentityPluginXP extends NoCloudIdentityPlugin{
	
	public NoCloudIdentityPluginXP(Properties properties) {
		super(properties);
	}

	@Override
	public Token getForwardableToken(Token originalToken) {
		return new Token(String.valueOf(UUID.randomUUID()), new Token.User(FAKE_USERNAME, FAKE_USERNAME), 
				null, new HashMap<String, String>());
	}

}

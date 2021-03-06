package org.fogbowcloud.manager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggingEvent;
import org.fogbowcloud.manager.core.ConfigurationConstants;
import org.fogbowcloud.manager.core.plugins.AccountingPlugin;
import org.fogbowcloud.manager.core.plugins.BenchmarkingPlugin;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;

public class MainHelper {

	private static final int MINIMUM_XMPP_TIMEOUT = 4000;
	public static final int EXIT_ERROR_CODE = 128;
	public static final int DEFAULT_XMPP_TIMEOUT = 15000; // 15 segundos
	public static final int DEFAULT_HTTP_PORT = 8182;
	public static final int DEFAULT_HTTPS_PORT = 8183;
	public static final boolean DEFAULT_HTTPS_ENABLED = false;
	public static final int DEFAULT_REQUEST_HEADER_SIZE = 1024*1024;
	public static final int DEFAULT_RESPONSE_HEADER_SIZE = 1024*1024;
	private static final String LOG_PATH = "log4j.appender.file.File";
	
	private static Properties props = new Properties();
	
	public static long getXMPPTimeout(Properties properties) {
		String timeoutStr = properties.getProperty(ConfigurationConstants.XMPP_TIMEOUT);
		long timeout = 0L;
		if (timeoutStr != null && !timeoutStr.isEmpty()) {
			try {
				timeout = Long.parseLong(timeoutStr);
				if (timeout < MINIMUM_XMPP_TIMEOUT) {
					throw new Error("Timeout is too small. " + MINIMUM_XMPP_TIMEOUT + " milliseconds is the minimum.");
				}
			} catch (Exception e) {
				throw new Error("Could not get timeout.", e);
			}
		} else {
			timeout = DEFAULT_XMPP_TIMEOUT;
		}
		return timeout;
	}	

	public static Object getIdentityPluginByPrefix(Properties properties, String prefix)
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

	public static Object createInstance(String propName, Properties properties) throws Exception {
		return Class.forName(properties.getProperty(propName)).getConstructor(Properties.class)
				.newInstance(properties);
	}
	
	public static Object createInstanceWithComputePlugin(String propName, 
			Properties properties, ComputePlugin computePlugin) throws Exception {
		return Class.forName(properties.getProperty(propName)).getConstructor(Properties.class, ComputePlugin.class)
				.newInstance(properties, computePlugin);
	}
	
	public static Object createInstanceWithBenchmarkingPlugin(
			String propName, Properties properties,
			BenchmarkingPlugin benchmarkingPlugin) throws Exception {
		return Class.forName(properties.getProperty(propName)).getConstructor(Properties.class, BenchmarkingPlugin.class)
				.newInstance(properties, benchmarkingPlugin);
	}
	
	public static Object createInstanceWithAccountingPlugin(
			String propName, Properties properties,
			AccountingPlugin accoutingPlugin) throws Exception {
		return Class.forName(properties.getProperty(propName)).getConstructor(Properties.class, AccountingPlugin.class)
				.newInstance(properties, accoutingPlugin);
	}	

	public static void configureLog4j(String path) {
		try {
			props.load(new FileInputStream(path));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try{
			Files.deleteIfExists(Paths.get(props.getProperty(LOG_PATH)));
		}catch(Exception e){
			e.printStackTrace();
		}		
		
		PropertyConfigurator.configure(props);
	}	
	
}

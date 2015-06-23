package org.fogbowcloud.manager.core.plugins.util;

import org.apache.commons.codec.Charsets;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.occi.model.ErrorType;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.fogbowcloud.manager.occi.model.ResponseConstants;

public class HttpClientWrapper {

	private static final Logger LOGGER = Logger.getLogger(HttpClientWrapper.class);
	private static final int SC_REQUEST_HEADER_TOO_LARGE = 431;
	private HttpClient client;

	private String doRequest(String url, String method) {
		HttpRequestBase request = null;
		if (method.equals("post")) {
			request = new HttpPost(url);
		} else if (method.equals("get")) {
			request = new HttpGet(url);
		}
		HttpResponse response = null;
		String responseStr = null;
		try {
			response = getClient().execute(request);
			responseStr = EntityUtils.toString(response.getEntity(), Charsets.UTF_8);
		} catch (HttpHostConnectException e) {
			LOGGER.error("could not connect to the host.", e);
			throw new OCCIException(ErrorType.BAD_REQUEST, ResponseConstants.NOT_FOUND);
		} 	
		catch (Exception e) {
			LOGGER.error("Could not do post request.", e);
			throw new OCCIException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
		}
		checkStatusResponse(response);
		return responseStr;
	}
	
	public String doPost(String url)  {
         return doRequest(url, "post");
	}
	
	public String doGet(String url)  {
		return doRequest(url, "get");
	}
	
	private HttpClient getClient() {
		if (client == null) {
			client = HttpClients.createMinimal();
		}
		return client;
	}
	
	private void checkStatusResponse(HttpResponse response) {
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
			throw new OCCIException(ErrorType.UNAUTHORIZED, ResponseConstants.UNAUTHORIZED);
		} else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND || 
				response.getStatusLine().getStatusCode() == SC_REQUEST_HEADER_TOO_LARGE) {
			throw new OCCIException(ErrorType.NOT_FOUND, ResponseConstants.NOT_FOUND);
		} 
	}
	
}

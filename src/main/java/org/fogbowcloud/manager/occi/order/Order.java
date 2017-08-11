package org.fogbowcloud.manager.occi.order;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.model.DateUtils;
import org.fogbowcloud.manager.experiments.monitor.MonitorPeerStateSingleton;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.model.Token;

public class Order {

	public static String SEPARATOR_GLOBAL_ID = "@";
	
	private String id;
	private Token federationToken;
	private String instanceId;
	private String providingMemberId;
	private final String requestingMemberId;
	private long fulfilledTime = 0;
	private long previousElapsedTime, currentElapsedTime, runtime;	
	private final boolean isLocal;
	private OrderState state;
	private List<Category> categories;
	private Map<String, String> xOCCIAtt;	
	private String resourceKind;
	private long syncronousTime;
	private boolean syncronousStatus;
	
	private DateUtils dateUtils = new DateUtils();
	private static final Logger LOGGER = Logger.getLogger(Order.class);	
		
	public Order(String id, Token federationToken, String instanceId, String providingMemberId,
			String requestingMemberId, long fulfilledTime, boolean isLocal, OrderState state,
			List<Category> categories, Map<String, String> xOCCIAtt) {
		LOGGER.setLevel(Level.INFO);
		this.id = id;
		this.federationToken = federationToken;
		this.instanceId = instanceId;
		this.providingMemberId = providingMemberId;
		this.requestingMemberId = requestingMemberId;
		this.fulfilledTime = fulfilledTime;
		this.isLocal = isLocal;
		this.state = state;
		this.categories = categories;
		this.xOCCIAtt = xOCCIAtt;
		if (this.xOCCIAtt == null) {
			this.resourceKind = null;			
		} else {
			this.resourceKind = this.xOCCIAtt.get(OrderAttribute.RESOURCE_KIND.getValue());
			this.runtime = Long.parseLong(this.xOCCIAtt.get(OrderAttribute.RUNTIME.getValue()));
			this.previousElapsedTime = Long.parseLong(this.xOCCIAtt.get(OrderAttribute.PREVIOUS_ELAPSED_TIME.getValue()));
			this.currentElapsedTime = Long.parseLong(this.xOCCIAtt.get(OrderAttribute.CURRENT_ELAPSED_TIME.getValue()));			
		}		
	}
	
	public Order(String id, Token federationToken, 
			List<Category> categories, Map<String, String> xOCCIAtt, boolean isLocal, String requestingMemberId, String providingMemberId) {
		this(id, federationToken, categories, xOCCIAtt, isLocal, requestingMemberId, providingMemberId, new DateUtils());
	}

	public Order(String id, Token federationToken, 
			List<Category> categories, Map<String, String> xOCCIAtt, boolean isLocal, String requestingMemberId) {
		this(id, federationToken, categories, xOCCIAtt, isLocal, requestingMemberId, new DateUtils());
	}
	
	public Order(String id, Token federationToken, 
			List<Category> categories, Map<String, String> xOCCIAtt, boolean isLocal, String requestingMemberId, DateUtils dateUtils) {
		this.id = id;
		this.federationToken = federationToken;
		this.categories = categories;
		this.xOCCIAtt = xOCCIAtt;
		this.isLocal = isLocal;
		this.requestingMemberId = requestingMemberId;
		this.dateUtils = dateUtils;
		setState(OrderState.OPEN);		
		if (this.xOCCIAtt == null) {
			this.resourceKind = null;			
		} else {
			this.resourceKind = this.xOCCIAtt.get(OrderAttribute.RESOURCE_KIND.getValue());
			this.runtime = Long.parseLong(this.xOCCIAtt.get(OrderAttribute.RUNTIME.getValue()));
			this.previousElapsedTime = Long.parseLong(this.xOCCIAtt.get(OrderAttribute.PREVIOUS_ELAPSED_TIME.getValue()));
			this.currentElapsedTime = Long.parseLong(this.xOCCIAtt.get(OrderAttribute.CURRENT_ELAPSED_TIME.getValue()));	
		}
	}
	
	public Order(String id, Token federationToken, 
			List<Category> categories, Map<String, String> xOCCIAtt, boolean isLocal, String requestingMemberId, String providingMemberId, DateUtils dateUtils) {
		this.id = id;
		this.federationToken = federationToken;
		this.categories = categories;
		this.xOCCIAtt = xOCCIAtt;
		this.isLocal = isLocal;
		this.requestingMemberId = requestingMemberId;
		this.providingMemberId = providingMemberId;
		this.dateUtils = dateUtils;
		setState(OrderState.OPEN);		
		if (this.xOCCIAtt == null) {
			this.resourceKind = null;			
		} else {
			this.resourceKind = this.xOCCIAtt.get(OrderAttribute.RESOURCE_KIND.getValue());
			this.runtime = Long.parseLong(this.xOCCIAtt.get(OrderAttribute.RUNTIME.getValue()));
			this.previousElapsedTime = Long.parseLong(this.xOCCIAtt.get(OrderAttribute.PREVIOUS_ELAPSED_TIME.getValue()));
			this.currentElapsedTime = Long.parseLong(this.xOCCIAtt.get(OrderAttribute.CURRENT_ELAPSED_TIME.getValue()));	
		}
	}
	
	public Order(Order order) {
		this(order.getId(), order.getFederationToken(), order.getCategories(), 
				order.getxOCCIAtt(), order.isLocal, order.getRequestingMemberId());
	}

	public List<Category> getCategories() {
		if (categories == null) {
			return null;
		}
		return new ArrayList<Category>(categories);
	}

	public void addCategory(Category category) {
		if (categories == null) {
			categories = new LinkedList<Category>();
		}
		if (!categories.contains(category)) {
			categories.add(category);
		}
	}

	public boolean isSyncronousStatus() {
		return syncronousStatus;
	}
	
	public void setSyncronousStatus(boolean syncronousStatus) {
		this.syncronousStatus = syncronousStatus;
	}
	
	public long getSyncronousTime() {
		return syncronousTime;
	}
	
	public void setSyncronousTime(long syncronousTime) {
		this.syncronousTime = syncronousTime;
	}
	
	public String getRequirements() {
		return xOCCIAtt.get(OrderAttribute.REQUIREMENTS.getValue());
	}
	
	public String getBatchId() {
		return xOCCIAtt.get(OrderAttribute.BATCH_ID.getValue());
	}
	
	public String getInstanceId() {
		return instanceId;
	}

	public String getGlobalInstanceId() {
		if (instanceId != null) {
			return instanceId + SEPARATOR_GLOBAL_ID + providingMemberId;
		}
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}
	
	public void setDateUtils(DateUtils dateUtils) {
		this.dateUtils = dateUtils;
	}
	
	public boolean isLocal(){
		return isLocal;
	}

	public OrderState getState() {
		return state;
	}

	public void setState(OrderState state) {
		if (state.in(OrderState.FULFILLED)) {
			fulfilledTime = dateUtils.currentTimeMillis();
		} else if (state.in(OrderState.OPEN)) {
			fulfilledTime = 0;
		}
		this.state = state;									
	}
	
	public void updateElapsedTime(boolean isRemoving){
		long now = dateUtils.currentTimeMillis();
		if(fulfilledTime!=0){
			currentElapsedTime = (now - fulfilledTime);		
		}
		if(isRemoving){
			fulfilledTime = 0;
			previousElapsedTime += currentElapsedTime;
			currentElapsedTime = 0;
		}
		this.xOCCIAtt.put(OrderAttribute.CURRENT_ELAPSED_TIME.getValue(), String.valueOf(currentElapsedTime));
		this.xOCCIAtt.put(OrderAttribute.PREVIOUS_ELAPSED_TIME.getValue(), String.valueOf(previousElapsedTime));
	}

	public String getId() {
		return id;
	}

	public String getAttValue(String attributeName) {
		if (xOCCIAtt == null) {
			return null;
		}
		return xOCCIAtt.get(attributeName);
	}

	public void putAttValue(String attributeName, String attributeValue) {
		if (xOCCIAtt == null) {
			xOCCIAtt = new HashMap<String, String>();
		}
		xOCCIAtt.put(attributeName, attributeValue);
	}

	public Token getFederationToken() {
		return this.federationToken;
	}

	public void setFederationToken(Token token) {
		this.federationToken = token;
	}
	
	public long getFulfilledTime() {
		return fulfilledTime;
	}
	
	public long getRuntime() {
		return runtime;
	}
	
	public long getPreviousElapsedTime() {
		return previousElapsedTime;
	}
	
	public long getCurrentElapsedTime() {
		return currentElapsedTime;
	}	

	public Map<String, String> getxOCCIAtt() {
		if (xOCCIAtt == null) {
			return new HashMap<String, String>();
		}
		return new HashMap<String, String>(xOCCIAtt);
	}
	
	public String getRequestingMemberId(){
		return requestingMemberId;
	}

	public String getProvidingMemberId() {
		return providingMemberId;
	}

	public void setProvidingMemberId(String providingMemberId) {
		this.providingMemberId = providingMemberId;
	}
	
	public void setxOCCIAtt(Map<String, String> xOCCIAtt) {
		this.xOCCIAtt = xOCCIAtt;
	}
	
	public String getResourceKind() {
		return resourceKind;
	}
	
	public void setResourceKing(String resourceKing) {
		this.resourceKind = resourceKing;
	}

	public String toString() {
		return "id: " + id + ", token: " + federationToken + ", instanceId: " + instanceId
				+ ", providingMemberId: " + providingMemberId + ", requestingMemberId: "
				+ requestingMemberId + ", state: " + state + ", isLocal " + isLocal
				+ ", categories: " + categories + ", xOCCIAtt: " + xOCCIAtt 
				+ ", runtime: " + runtime + ", fulfilledTime: "+fulfilledTime
				+ ", previousElpasedTime: "+previousElapsedTime+""
				+ ", currentElapsedTime: "+currentElapsedTime+"\n";
	}


	public boolean isIntoValidPeriod() {
		String startDateStr = xOCCIAtt.get(OrderAttribute.VALID_FROM.getValue());
		Date startDate = DateUtils.getDateFromISO8601Format(startDateStr);
		if (startDate == null) {
			if (startDateStr != null) {
				return false;
			}
			startDate = new Date();
		}
		long now = new DateUtils().currentTimeMillis();
		return startDate.getTime() <= now && !isExpired();
	}

	public boolean isExpired() {
		String expirationDateStr = xOCCIAtt.get(OrderAttribute.VALID_UNTIL.getValue());
		Date expirationDate = DateUtils.getDateFromISO8601Format(expirationDateStr);
		if (expirationDateStr == null) {
			return false;
		} else if (expirationDate == null) {
			return true;
		}

		long now = new DateUtils().currentTimeMillis();
		return expirationDate.getTime() < now;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		else if(obj instanceof Order){
			Order o = (Order) obj;
			if(o.getId().equals(getId()))
				return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getId().hashCode();
	}
	
}
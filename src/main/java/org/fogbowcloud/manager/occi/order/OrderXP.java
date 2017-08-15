package org.fogbowcloud.manager.occi.order;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.model.DateUtils;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.model.Token;

public class OrderXP extends Order{

	
	private long previousElapsedTime, currentElapsedTime, runtime;	
	
	
	private DateUtils dateUtils = new DateUtils();
	private static final Logger LOGGER = Logger.getLogger(OrderXP.class);	
		
	public OrderXP(String id, Token federationToken, String instanceId, String providingMemberId,
			String requestingMemberId, long fulfilledTime, boolean isLocal, OrderState state,
			List<Category> categories, Map<String, String> xOCCIAtt) {
		super(id, federationToken, instanceId, providingMemberId,requestingMemberId, fulfilledTime, isLocal, 
				state, categories, xOCCIAtt);		
		putOrderAttrs();
	}
	
	public OrderXP(String id, Token federationToken, 
			List<Category> categories, Map<String, String> xOCCIAtt, boolean isLocal, String requestingMemberId, String providingMemberId) {
		this(id, federationToken, categories, xOCCIAtt, isLocal, requestingMemberId, providingMemberId, new DateUtils());
	}

	public OrderXP(String id, Token federationToken, 
			List<Category> categories, Map<String, String> xOCCIAtt, boolean isLocal, String requestingMemberId) {
		this(id, federationToken, categories, xOCCIAtt, isLocal, requestingMemberId, new DateUtils());
	}
	
	public OrderXP(String id, Token federationToken, 
			List<Category> categories, Map<String, String> xOCCIAtt, boolean isLocal, String requestingMemberId, DateUtils dateUtils) {
		super(id, federationToken, categories, xOCCIAtt, isLocal, requestingMemberId, dateUtils);
		putOrderAttrs();		
	}
	
	public OrderXP(String id, Token federationToken, 
			List<Category> categories, Map<String, String> xOCCIAtt, boolean isLocal, String requestingMemberId, String providingMemberId, DateUtils dateUtils) {
		super(id, federationToken, categories, xOCCIAtt, isLocal, requestingMemberId, dateUtils);
		this.providingMemberId = providingMemberId;			
		putOrderAttrs();		
	}
	
	private void putOrderAttrs(){
		this.runtime = Long.parseLong(this.xOCCIAtt.get(OrderAttribute.RUNTIME.getValue()));
		this.previousElapsedTime = Long.parseLong(this.xOCCIAtt.get(OrderAttribute.PREVIOUS_ELAPSED_TIME.getValue()));
		this.currentElapsedTime = Long.parseLong(this.xOCCIAtt.get(OrderAttribute.CURRENT_ELAPSED_TIME.getValue()));
	}
	
	public OrderXP(OrderXP order) {
		this(order.getId(), order.getFederationToken(), order.getCategories(), 
				order.getxOCCIAtt(), order.isLocal, order.getRequestingMemberId());
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
	
	public long getRuntime() {
		return runtime;
	}
	
	public long getPreviousElapsedTime() {
		return previousElapsedTime;
	}
	
	public long getCurrentElapsedTime() {
		return currentElapsedTime;
	}
	

	@Override
	public String toString() {
		return "id: " + id + ", token: " + federationToken + ", instanceId: " + instanceId
				+ ", providingMemberId: " + providingMemberId + ", requestingMemberId: "
				+ requestingMemberId + ", state: " + state + ", isLocal " + isLocal
				+ ", categories: " + categories + ", xOCCIAtt: " + xOCCIAtt 
				+ ", runtime: " + runtime + ", fulfilledTime: "+fulfilledTime
				+ ", previousElpasedTime: "+previousElapsedTime+""
				+ ", currentElapsedTime: "+currentElapsedTime+"\n";
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		else if(obj instanceof OrderXP){
			OrderXP o = (OrderXP) obj;
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
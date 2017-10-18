package org.fogbowcloud.manager.experiments.data;

import org.fogbowcloud.manager.occi.order.OrderState;

public class OrderStatus {

	private OrderState state;
	private String requestingMemberId, providingMemberId;
	
	public OrderStatus(OrderState state, String requestingMemberId, String providingMemberId){
		this.state = state;
		this.requestingMemberId = requestingMemberId;
		this.providingMemberId = providingMemberId;
	}
	
	public OrderState getState() {
		return state;
	}
	
	public String getProvidingMemberId() {
		return providingMemberId;
	}
	
	public String getRequestingMemberId() {
		return requestingMemberId;
	}
	
	@Override
	public String toString() {
		return "State: "+state+", requestingMemberId: "+requestingMemberId+", providingMemberId: "+providingMemberId;
	}
	
}

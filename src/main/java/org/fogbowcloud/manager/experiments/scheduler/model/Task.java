package org.fogbowcloud.manager.experiments.scheduler.model;

import org.fogbowcloud.manager.occi.order.Order;

public class Task {
	
	private int runtime;
	private String orderId;
	
	public Task(int runtime){
		this.runtime = runtime;
	}

	public int getRuntime() {
		return runtime;
	}
	
	@Override
	public String toString() {
		return "runtime: "+runtime+", orderId: "+orderId;
	}
	
	public String getOrderId() {
		return orderId;
	}
	
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

}

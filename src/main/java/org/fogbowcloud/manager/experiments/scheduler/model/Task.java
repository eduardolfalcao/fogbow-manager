package org.fogbowcloud.manager.experiments.scheduler.model;

public class Task implements Comparable<Task>{
	
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

	@Override
	public int compareTo(Task t) {
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;
				
		if (runtime < t.getRuntime()) 
	    	return BEFORE;
		else if(runtime == t.getRuntime())
			return EQUAL;
		else	// (runtime >= t.runtime) 
	    	return AFTER;
	}

}

package org.fogbowcloud.manager.experiments.scheduler.model;

public class Task {
	
	private int runtime;
	
	public Task(int runtime){
		this.runtime = runtime;
	}

	public int getRuntime() {
		return runtime;
	}
	
	@Override
	public String toString() {
		return "runtime: "+runtime;
	}

}

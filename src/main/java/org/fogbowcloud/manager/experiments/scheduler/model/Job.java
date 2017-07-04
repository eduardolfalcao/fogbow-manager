package org.fogbowcloud.manager.experiments.scheduler.model;

import java.util.ArrayList;
import java.util.List;

public class Job implements Comparable<Job>{
	
	private String peerId, userId;
	
	private int id, submitTime;
	private List<Task> tasks;
	
	public Job(String peerId, String userId, int id, int submitTime){
		this.peerId = peerId;
		this.userId = userId;
		this.id = id;
		this.submitTime = submitTime;
		tasks = new ArrayList<Task>();
	}
	
	@Override
	public String toString() {
		return "Job: "+id+", submitTime: "+submitTime+" - "+tasks;
	}
	
	@Override
	public boolean equals(Object obj){
		if(!(obj instanceof Job))
			return false;
		
		Job job = (Job) obj;
		if(id != job.getId())
			return false;
		else
			return true;
	}

	@Override
	public int compareTo(Job job) {
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;
				
		if (submitTime < job.getSubmitTime()) 
	    	return BEFORE;
		else if(submitTime == job.getSubmitTime())
			return EQUAL;
		else	// (submitTime >= job.getSubmitTime) 
	    	return AFTER;
	}
	
	public int getId(){
		return id;
	}
	
	public int getSubmitTime(){
		return submitTime;
	}
	
	public List<Task> getTasks(){
		return tasks;
	}
	
	public String getPeerId(){
		return peerId;
	}
	
	public String getUserId(){
		return userId;
	}

}

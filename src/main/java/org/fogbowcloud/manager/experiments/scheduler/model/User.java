package org.fogbowcloud.manager.experiments.scheduler.model;

import java.util.ArrayList;
import java.util.List;

public class User {
	
	private String userId;
	private List<Job> jobs;
	
	public User(String userId){
		this.userId = userId;
		jobs = new ArrayList<Job>();
	}
	
	@Override
	public String toString() {
		return "User: "+userId+" - "+jobs;
	}
	
	@Override
	public boolean equals(Object obj){
		if(!(obj instanceof User))
			return false;
		
		User user = (User) obj;
		if(!userId.equals(user.getUserId()))
			return false;
		else
			return true;
	}

	public String getUserId() {
		return userId;
	}

	public List<Job> getJobs() {
		return jobs;
	}

}

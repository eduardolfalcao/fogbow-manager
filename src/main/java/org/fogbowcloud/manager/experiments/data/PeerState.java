package org.fogbowcloud.manager.experiments.data;

public class PeerState implements Comparable<PeerState>{
	
	private String id;
	private int time;
	private int demand, supply, maxCapacity;
	
	public PeerState(String id, int time, int demand, int supply, int maxCapacity) {
		super();
		this.id = id;
		this.time = time;
		this.demand = demand;
		this.supply = supply;
		this.maxCapacity = maxCapacity;
	}
	
	public String getId() {
		return id;
	}
	
	public void setTime(int time) {
		this.time = time;
	}
	
	public int getTime() {
		return time;
	}
	
	public int getDemand() {
		return demand;
	}
	
	public int getSupply() {
		return supply;
	}
	
	public void setMaxCapacity(int maxCapacity) {
		this.maxCapacity = maxCapacity;
	}
	
	public int getMaxCapacity() {
		return maxCapacity;
	}
	
	@Override
	public String toString() {
		return "id:"+id+", time:"+time+", demand:"+demand+", supply:"+supply+", maxCapacity:"+maxCapacity;
	}

	@Override
	public int compareTo(PeerState p) {
		if(time < p.getTime())
			return -1;
		else if(time == p.getTime())
			return 0;
		else
			return 1;
	}
	
}

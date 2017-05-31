package org.fogbowcloud.manager.experiments.data;

public class PeerState implements Comparable<PeerState>{
	
	private String id;
	private int time;
	private int demand, supply;
	
	public PeerState(String id, int time, int demand, int supply) {
		super();
		this.id = id;
		this.time = time;
		this.demand = demand;
		this.supply = supply;
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
	
	@Override
	public String toString() {
		return "id:"+id+", time:"+time+", demand:"+demand+", supply:"+supply;
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

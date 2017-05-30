package org.fogbowcloud.manager.experiments.data;

public class PeerState {
	
	private long time;
	private int demand, supply;
	
	public PeerState(long time, int demand, int supply) {
		super();
		this.time = time;
		this.demand = demand;
		this.supply = supply;
	}
	
	public long getTime() {
		return time;
	}
	
	public int getDemand() {
		return demand;
	}
	
	public int getSupply() {
		return supply;
	}	
	
}

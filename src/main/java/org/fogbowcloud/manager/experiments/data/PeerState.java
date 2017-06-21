package org.fogbowcloud.manager.experiments.data;

public class PeerState{
	
	private String id;
	private int time;
	private int dTot, dFed, rFed, oFed, sFed;
	//d: demand
	//r: received
	//o: offered
	//s: supplied or donated
	
	public PeerState(String id, int time, int dTot, int dFed, int rFed, int oFed, int sFed) {
		super();
		this.id = id;
		this.time = time;
		this.dTot = dTot;
		this.dFed = dFed;
		this.rFed = rFed;
		this.oFed = oFed;
		this.sFed = sFed;
	}	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public int getdTot() {
		return dTot;
	}

	public void setdTot(int dTot) {
		this.dTot = dTot;
	}

	public int getdFed() {
		return dFed;
	}

	public void setdFed(int dFed) {
		this.dFed = dFed;
	}

	public int getrFed() {
		return rFed;
	}

	public void setrFed(int rFed) {
		this.rFed = rFed;
	}

	public int getoFed() {
		return oFed;
	}

	public void setoFed(int oFed) {
		this.oFed = oFed;
	}

	public int getsFed() {
		return sFed;
	}

	public void setsFed(int sFed) {
		this.sFed = sFed;
	}

	@Override
	public String toString() {
		return "id:"+id+", time:"+time+", demand(tot):"+dTot+", demand(fed):"+dFed+", \n"
				+ "received(fed):"+rFed+", offered(fed):"+oFed+", supplied(fed):"+sFed;
	}
	
}

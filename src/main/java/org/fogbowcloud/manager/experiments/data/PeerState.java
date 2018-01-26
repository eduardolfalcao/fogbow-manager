package org.fogbowcloud.manager.experiments.data;

public class PeerState{
	
	private String id;
	private int time;
	private int dTot, dFed, rFed, oFed, sFed, rLoc, unat, unatP;
	//d: demand
	//r: received
	//o: offered
	//s: supplied or donated
	
	public PeerState(String id, int time, int dTot, int dFed, int rFed, int oFed, int sFed, int rLoc, int unat, int unatP) {
		super();
		this.id = id;
		this.time = time;
		this.dTot = dTot;
		this.dFed = dFed;
		this.rFed = rFed;
		this.oFed = oFed;
		this.sFed = sFed;
		this.rLoc = rLoc;
		this.unat = unat;
		this.unatP = unatP;
	}
	
	public int getrLoc() {
		return rLoc;
	}
	
	public int getUnat() {
		return unat;
	}
	
	public int getUnatP() {
		return unatP;
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

	public void incDTot() {
		this.dTot++;
	}
	
	public void decDTot() {
		this.dTot--;
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

	public void incRFed() {
		this.rFed++;
	}
	
	public void decRFed() {
		this.rFed--;
	}

	public int getoFed() {
		return oFed;
	}

	public void incOFed() {
		this.oFed++;
	}
	
	public void incOFed(int n) {
		this.oFed += n;
	}
	
	public void decOFed() {
		this.oFed--;
	}

	public int getsFed() {
		return sFed;
	}

	public void incSFed() {
		this.sFed++;
	}
	
	public void decSFed() {
		this.sFed--;
	}

	@Override
	public String toString() {
		return "id:"+id+", time:"+time+", demand(tot):"+dTot+", demand(fed):"+dFed+", "
				+ "received(fed):"+rFed+", offered(fed):"+oFed+", supplied(fed):"+sFed+", "
				+ "rLoc:"+rLoc+", unat(fed):"+unat+", unatP(fed):"+unatP;
	}
	
}

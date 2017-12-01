package contention;

public class Status{
	
	private int dFed, oFed;
	private int dTot,rFed,sFed;
	
	public Status(int dFed, int oFed) {
		super();
		this.dFed = dFed;
		this.oFed = oFed;
	}
	
	public Status(int dTot, int dFed, int rFed, int oFed, int sFed) {
		super();
		this.dFed = dFed;
		this.oFed = oFed;
		this.dTot = dTot;
		this.rFed = rFed;
		this.sFed = sFed;
	}


	@Override
	public String toString() {
		return "dfed: "+dFed+", ofed: "+oFed;
	}
	
	public int getdFed() {
		return dFed;
	}

	public void setdFed(int dFed) {
		this.dFed = dFed;
	}

	public int getoFed() {
		return oFed;
	}

	public void setoFed(int oFed) {
		this.oFed = oFed;
	}
	
	public int getdTot() {
		return dTot;
	}
	
	public int getrFed() {
		return rFed;
	}
	
	public int getsFed() {
		return sFed;
	}

}
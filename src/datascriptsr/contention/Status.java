package contention;

public class Status{
	
	private int dFed, oFed;
	private int dTot,rFed,sFed;
	private int rLoc, unat, unatP;
	
	public Status() {}
	
	public Status(int dFed, int oFed) {
		super();
		this.dFed = dFed;
		this.oFed = oFed;
	}
	
	public Status(int dTot, int dFed, int rFed, int oFed, int sFed, int rLoc, int unat, int unatP) {
		super();
		this.dFed = dFed;
		this.oFed = oFed;
		this.dTot = dTot;
		this.rFed = rFed;
		this.sFed = sFed;
		this.rLoc = rLoc;
		this.unat = unat;
		this.unatP = unatP;
	}
	
	public Status(int dTot, int dFed, int rFed, int oFed, int sFed, int unat) {
		super();
		this.dFed = dFed;
		this.oFed = oFed;
		this.dTot = dTot;
		this.rFed = rFed;
		this.sFed = sFed;
		this.unat = unat;
	}


	@Override
	public String toString() {
		return "dfed: "+dFed+", ofed: "+oFed;
	}
	
	public int getdFed() {
		return dFed;
	}
	
	public void addDFed(int dFed) {
		this.dFed += dFed;;
	}

	public void setdFed(int dFed) {
		this.dFed = dFed;
	}

	public int getoFed() {
		return oFed;
	}
	
	public void addOFed(int oFed) {
		this.oFed += oFed;;
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
	
	public int getrLoc() {
		return rLoc;
	}
	
	public int getUnat() {
		return unat;
	}
	
	public int getUnatP() {
		return unatP;
	}

}
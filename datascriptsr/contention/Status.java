package contention;

public class Status{
	
	private int dFed, oFed;
	
	public Status(int dFed, int oFed) {
		super();
		this.dFed = dFed;
		this.oFed = oFed;
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

}
package contention;

public class PeerAndTime implements Comparable<PeerAndTime>{
	
	private String id;
	private int t;
	
	public PeerAndTime(String id, int t) {
		super();
		this.id = id;
		this.t = t;		
	}
	
	@Override
	public String toString() {
		return "t: "+t+", id: "+id;
	}

	@Override
	public int compareTo(PeerAndTime s) {
		if(t - s.getT() ==0){
			String idAux = id;	//to not remove the "p" inside the map
			idAux = idAux.replace("p", "");
			int idNum = Integer.parseInt(idAux);
			String sIdAux = s.getId();
			int sIdNum = Integer.parseInt(sIdAux.replace("p", ""));
			return idNum - sIdNum;
		}
		else return t - s.getT();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getT() {
		return t;
	}

	public void setT(int t) {
		this.t = t;
	}

}
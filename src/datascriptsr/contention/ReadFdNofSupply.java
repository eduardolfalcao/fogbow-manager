package contention;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import adjustdata.CountDonationToFreeRiders;

public class ReadFdNofSupply {
	
	private Map<Integer,Double> peerAndSupply;
	private int time = 0;
	private double supply = 800;
	private static final int NUM_PEERS = 40;
	
	public ReadFdNofSupply() {
		this.peerAndSupply = new HashMap<Integer,Double>();
	}
	
	public Map<Integer, Double> getPeerAndSupply() {
		return peerAndSupply;
	}
	
	public static void main(String[] args) throws IOException {
		
		Map<Integer,Map<Integer,Double>> peersAndSupply = new HashMap<Integer, Map<Integer,Double>>();
		
		for(int peer = 1; peer <= 1; peer++){
			ReadFdNofSupply rs = new ReadFdNofSupply();
			
			String path = "/home/eduardolfalcao/git/fogbow-manager/experiments/data/fdnof-k1/quotas.txt";
			File fin = new File(path);
			
			// Construct BufferedReader from FileReader
			BufferedReader br = new BufferedReader(new FileReader(fin));
			String line = null;
			while ((line = br.readLine()) != null) {
				rs.fulfillMap(line,peer);			
			}
			br.close();
			
			peersAndSupply.put(peer, rs.getPeerAndSupply());
		}
		
		
		
		
//		for (Map.Entry<Integer, Integer> pair : cdfr.getDonated().entrySet()) {
//			System.out.println("peer "+pair.getKey()+" donated "+pair.getValue());
//		}
		
	}
	
	private void fulfillMap(String line, int peer){
		
		if(line.contains("TimeMonitor")){
			int index = line.indexOf("RealTime: ");
			time = Integer.parseInt(line.substring(index+10));
			if(supply!=0){
				this.peerAndSupply.put(time, supply/(NUM_PEERS-1));
				supply = 0;
			}			
			System.out.println(time);
			return;
		}
		
		int indexProvider = line.indexOf("<p");
		int provider = -1;	
		try{
			provider = Integer.parseInt(line.substring(indexProvider+2,indexProvider+4));
		}catch(Exception e){
			provider = Integer.parseInt(line.substring(indexProvider+2,indexProvider+3));
		}
		if(provider == peer){
			int indexQuota = line.indexOf("Quota: ");
			double quota = Double.parseDouble(line.substring(indexQuota+7));
			supply += quota;			
			System.out.println("p"+provider+" - quota: "+quota);			
		}		
		
		if(time > 6)
			System.exit(0);
		
	}

}

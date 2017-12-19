package adjustdata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class CountDonationToFreeRiders {

	private Map<Integer,Integer> donated;
	
	public Map<Integer, Integer> getDonated() {
		return donated;
	}
	
	public CountDonationToFreeRiders() {
		donated = new TreeMap<Integer, Integer>();
	}
	
	public static void main(String[] args) throws IOException {
		
		CountDonationToFreeRiders cdfr = new CountDonationToFreeRiders();
		
		String path = "/home/eduardo/Área de Trabalho/Experimentos-Doutorado/debug/ordersAskedByp41-p50.txt";
		File fin = new File(path);
		
		// Construct BufferedReader from FileReader
		BufferedReader br = new BufferedReader(new FileReader(fin));
		String line = null;
		while ((line = br.readLine()) != null) {
			cdfr.fulfillMap(line);			
		}
		br.close();
		
		
		for (Map.Entry<Integer, Integer> pair : cdfr.getDonated().entrySet()) {
			System.out.println("peer "+pair.getKey()+" donated "+pair.getValue());
		}
		
	}
	
	private void fulfillMap(String line){
//		int indexFirstPeer = line.indexOf("<p");
		int indexSecondPeer = line.indexOf("to p");
		
		System.out.println(line);
		
		int providing = -1;
//		int consuming = -1;
//		double capacity = -1;
		
//		try{
//			consuming = Integer.parseInt(line.substring(indexFirstPeer+2,indexFirstPeer+4));
//		}catch(Exception e){
//			consuming = Integer.parseInt(line.substring(indexFirstPeer+2,indexFirstPeer+3));
//		}
		
		try{
			providing = Integer.parseInt(line.substring(indexSecondPeer+4,indexSecondPeer+6));
		}catch(Exception e){
			providing = Integer.parseInt(line.substring(indexSecondPeer+4,indexSecondPeer+5));
		}
		
		int currentProvided = 0;
		if(donated.containsKey(providing)){
			currentProvided = donated.get(providing);
		}
		donated.put(providing, currentProvided+1);
	}
	
}

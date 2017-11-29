package contention;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.fogbowcloud.manager.experiments.data.CsvGenerator;

public class ContentionCalculator {

	static DataReader dr = new DataReader();
	
	public static void main(String[] args) {
		
//		String [] nofVars = {"sdnof", "fdnof"};
		String [] nofVars = {"sdnof-10minutes","sdnof-7minutes"};
//		int [] cycleVars = {10,30,60};
		int [] cycleVars = {10};
		
		for(String nof : nofVars){
			for(int cycle : cycleVars){
				String path = "/home/eduardolfalcao/workspace3/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/weightedNof/cycle"+cycle+"/"+nof+"/";
				Map<PeerAndTime, Status> results = readFiles(path);
				Map<Integer, Double> contention = computeContention(results);
				output(contention, path+"contention/contention.csv", nof, String.valueOf(cycle));
			}			
		}				
	}
	
	private static Map<PeerAndTime, Status> readFiles(String path){
		Map<PeerAndTime, Status> results = new TreeMap<PeerAndTime, Status>();
		
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();
		for(File file : listOfFiles){
			if(file.isFile()){
				System.out.println("Reading file: "+file.getName());
				dr.readResults(results, file);
			}
		}
		return results;
	}
	
	private static Map<Integer, Double> computeContention(Map<PeerAndTime, Status> results){
		Map<Integer, Double> contention = new TreeMap<Integer, Double>();
		Map<String, Status> currentStatus = new HashMap<String, Status>();
		
		
		int t = 0;
		for(Map.Entry<PeerAndTime, Status> entry : results.entrySet()) {			
			if(t != entry.getKey().getT()){	//computa contenção e add no mapa
				double dFed = 0, oFed = 0;
				for(Map.Entry<String, Status> entryAux : currentStatus.entrySet()) {
					dFed += entryAux.getValue().getdFed();
					oFed += entryAux.getValue().getoFed();
				}
				contention.put(t, dFed/oFed);
			}
			
			currentStatus.put(entry.getKey().getId(), new Status(entry.getValue().getdFed(), entry.getValue().getoFed()));
			t = entry.getKey().getT();
		}
		
		return contention;
	}
	
	private static void output(Map<Integer, Double> contention, String outputFile, String nof, String cycle){
		FileWriter fw = CsvGenerator.createHeader(outputFile, new String[]{"t", "kappa", "nof", "cycle"});
		for(Map.Entry<Integer, Double> entry : contention.entrySet()) {
			CsvGenerator.outputValues(fw, new String[]{String.valueOf(entry.getKey()),String.valueOf(entry.getValue()), nof, cycle});
		}
		CsvGenerator.flushFile(fw);	
	}
	
}

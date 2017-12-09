package contention;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.fogbowcloud.manager.experiments.data.CsvGenerator;

public class ContentionCalculator {
	
	private static final String PATH_BASE_NOTEBOOK = "/home/eduardo/git/";
	private static final String PATH_BASE_LSD = "/home/eduardolfalcao/git/";

	static DataReader dr = new DataReader();
	
	public static void main(String[] args) {
		
		ContentionCalculator cc = new ContentionCalculator();
		
//		String [] nofVars = {"sdnof", "fdnof"};
		String [] nofVars = {"sdnof-10minutes-0.5kappa","fdnof-10minutes-0.5kappa"};
//		int [] cycleVars = {10,30,60};
		int [] cycleVars = {10};
		
		for(String nof : nofVars){
			for(int cycle : cycleVars){
				String path = PATH_BASE_LSD+"fogbow-manager/experiments/data scripts r/done/40peers-20capacity/weightedNof/cycle"+cycle+"/"+nof+"/";
				Map<PeerAndTime, Status> results = cc.readFiles(path);
				Map<Integer, ContentionCalculator.Triple> contention = cc.computeContention(results);
				cc.output(contention, path+"contention/contention.csv", nof, String.valueOf(cycle));
			}			
		}				
	}
	
	private Map<PeerAndTime, Status> readFiles(String path){
		Map<PeerAndTime, Status> results = new TreeMap<PeerAndTime, Status>();
		System.out.println("Path: "+path);
		
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

	
	
	private Map<Integer, Triple> computeContention(Map<PeerAndTime, Status> results){
		Map<Integer, ContentionCalculator.Triple> contention = new TreeMap<Integer, ContentionCalculator.Triple>();
		Map<String, Status> currentStatus = new HashMap<String, Status>();
		
		
		int t = 0;
		for(Map.Entry<PeerAndTime, Status> entry : results.entrySet()) {			
			if(t != entry.getKey().getT()){	//computa contenção e add no mapa
				double dFed = 0, oFed = 0;
				for(Map.Entry<String, Status> entryAux : currentStatus.entrySet()) {
					dFed += entryAux.getValue().getdFed();
					oFed += entryAux.getValue().getoFed();
				}
				for(; t < entry.getKey().getT(); t++){
					Triple triplet = this.new Triple((dFed/oFed),dFed,oFed);
					contention.put(t, triplet);
				}
			}
			
			currentStatus.put(entry.getKey().getId(), new Status(entry.getValue().getdFed(), entry.getValue().getoFed()));
			t = entry.getKey().getT();
		}
		
		return contention;
	}
	
	private void output(Map<Integer, ContentionCalculator.Triple> contention, String outputFile, String nof, String cycle){
		FileWriter fw = CsvGenerator.createHeader(outputFile, new String[]{"t", "kappa", "dFed", "oFed", "nof", "cycle"});
		for(Map.Entry<Integer, Triple> entry : contention.entrySet()) {
			CsvGenerator.outputValues(fw, new String[]{String.valueOf(entry.getKey()),String.valueOf(entry.getValue().getContention()), 
					String.valueOf(entry.getValue().getdFed()), String.valueOf(entry.getValue().getoFed()),nof, cycle});
		}
		CsvGenerator.flushFile(fw);	
	}
	
	private class Triple{
		
		private double contention, dFed, oFed;
		
		public Triple(double contention, double dFed, double oFed){
			this.contention = contention;
			this.dFed = dFed;
			this.oFed = oFed;
		}
		
		public double getContention() {
			return contention;
		}
		public double getoFed() {
			return oFed;
		}
		public double getdFed() {
			return dFed;
		}
	}
}


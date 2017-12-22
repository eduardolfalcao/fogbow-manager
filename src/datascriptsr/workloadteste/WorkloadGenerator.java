package workloadteste;

import java.io.FileWriter;
import java.io.IOException;

public class WorkloadGenerator {
	
	public static void main(String[] args) {
		int numPeersPerGroup = 20;
		int demand = 30;
		int[] cycle = new int[]{30*60};
		int totalDuration = 24*60*60;	//in seconds
		
		for(int curCycle : cycle){
			String filename = "experiments/workload/synthetic/kappa05-demand"+demand+"-cycle"+curCycle/60+"-totalDuration"+totalDuration/60/60+".txt";
			new WorkloadGenerator(numPeersPerGroup, demand, curCycle, totalDuration, filename);
		}
	}
	
	private final String USER_ID = "U1";
	private final String TRACE_ID = "synthetic";
	private final String CLUSTER = "any";
	
	private int numPeersPerGroup;
	private int demand;
	private int cycle, totalDuration;	//in seconds
	
	public WorkloadGenerator(int numPeersPerGroup, int demand, int cycle, int totalDuration, String filename) {
		super();
		this.numPeersPerGroup = numPeersPerGroup;
		this.demand = demand;
		this.cycle = cycle;
		this.totalDuration = totalDuration;
		
		FileWriter fw = createHeader(filename);
		writeLines(fw);
		flushFile(fw);
	}
	
	private FileWriter createHeader(String outputFile) {
		String [] header = new String[]{"SubmitTime", "RunTime", "JobID", "UserID", "PeerID", "TraceID", "Cluster.IAT", "Cluster.JRT", "Cluster.TRT"};
		FileWriter writer = null;
		try {
			writer = new FileWriter(outputFile, true);
			for(int i = 0; i < header.length; i++){
				writer.append(header[i]);
				if(i < header.length-1)
					writer.append(" ");
			}			
			writer.append('\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer;
	}
	
	private void writeLines(FileWriter writer){
		boolean groupAConsuming = true;
		int curTime = 0;
		
		int jobId = 1;
		
		while(curTime < totalDuration){
			int firstPeerId, lastPeerId;
			if(groupAConsuming){
				firstPeerId = 1;
				lastPeerId = numPeersPerGroup;
			}
			else{
				firstPeerId = numPeersPerGroup+1;
				lastPeerId = 2*numPeersPerGroup;
			}
			
			for(int i = firstPeerId; i <= lastPeerId; i++){
				int submitTime = curTime;
				int runtime = cycle;
				String peerId = "P"+i;
				
				for(int j = 0; j < demand; j++){
					String[] line = new String[]{String.valueOf(submitTime), String.valueOf(runtime), String.valueOf(jobId),
						USER_ID, peerId, TRACE_ID, CLUSTER, CLUSTER, CLUSTER};
					outputLine(writer, line);		
				}			
			}
				
			jobId++;
			curTime += cycle;
			groupAConsuming = !groupAConsuming;			
		}
	}

	
	
	private void outputLine(FileWriter writer, String[] values){
		try {
			String output = "";
			for(int i = 0; i < values.length; i++){
				output += values[i];
				if(i == values.length-1)
					output += "\n";
				else
					output += " ";
			}
			writer.append(output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void flushFile(FileWriter writer) {
		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

package org.fogbowcloud.manager.experiments.data;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvGenerator {
	
	public static FileWriter createHeader(String outputFile, String ... labels ) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(outputFile);
			for(int i = 0; i < labels.length; i++){
				writer.append(labels[i]);
				if(i < labels.length-1)
					writer.append(",");
			}			
			writer.append('\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer;
	}
	
	public static FileWriter getFile(String outputFile) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer;
	}
	
	public static void outputPeerStates(FileWriter writer, List<PeerState> states){
		try {
			for(PeerState s : states)			
				writer.append(s.getTime()+","+s.getDemand()+","+s.getSupply()+"\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void flushFile(FileWriter writer) {
		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

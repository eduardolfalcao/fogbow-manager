package adjustdata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.fogbowcloud.manager.experiments.data.CsvGenerator;

import contention.ContentionCalculator;

public class GraphFactory {
	
	public static void main(String[] args) throws IOException {
		GraphFactory gf = new GraphFactory();
		double[][] capacity = gf.createArray(40);
		
		String path = "/local/eduardolfalcao/cloudcom2017/fogbow-manager.log";
		
		ReversedLinesFileReader fr = new ReversedLinesFileReader(new File(path));
		String ch;
		int time=0;
		String Conversion="";
		do {
		    ch = fr.readLine();
		    if(ch != null){
		    	if(ch.contains("The max capacity for")){
		    		gf.fulfillGraph(ch, capacity);
		    	}
		    } 
		} while (ch != null && !gf.isEveryPeerFulfilled(capacity));
		fr.close();
		
		gf.printArray(capacity);
		
		String outputFile = "/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/grafo2.csv";
		gf.output(capacity, outputFile);
		
		
	}
	
	private void output(double[][] capacityArray, String outputFile){
		String[] header = new String[capacityArray.length+1];
		header[0] = "";
		for(int i = 0; i < capacityArray.length; i++){
			header[i+1] = "p"+String.valueOf(i+1);
		}
		
		FileWriter fw = CsvGenerator.createHeader(outputFile, header);
		
		for(int i = 0; i < capacityArray.length; i++){
			String[] values = new String[capacityArray[i].length+1];
			values[0] = "p" + String.valueOf(i+1);
			for(int j = 0; j < capacityArray[i].length; j++){
				values[j+1] = String.valueOf(capacityArray[i][j]);
			}
			CsvGenerator.outputValues(fw, values);
		}
		
		CsvGenerator.flushFile(fw);	
	}
	
	private boolean isEveryPeerFulfilled(double[][] capacityArray){
		for(int i = 0; i < capacityArray.length; i++){
			for(int j = 0; j < capacityArray[i].length; j++){
				if(i!=j && capacityArray[i][j]==-1){
					return false;
				}				
			}			
		}
		for(int i = 0; i < capacityArray.length; i++){
			for(int j = 0; j < capacityArray[i].length; j++){
				if(i==j){
					capacityArray[i][j]=0;
				}				
			}			
		}
		
		return true;
	}
	
	private void fulfillGraph(String line, double[][] capacityArray){
		int indexFirstPeer = line.indexOf("<p");
		int indexSecondPeer = line.indexOf("for p");
		int indexCapacity = line.indexOf("is ");
		
		System.out.println(line);
		
		int providing = -1;
		int consuming = -1;
		double capacity = -1;
		
		try{
			providing = Integer.parseInt(line.substring(indexFirstPeer+2,indexFirstPeer+4));
		}catch(Exception e){
			providing = Integer.parseInt(line.substring(indexFirstPeer+2,indexFirstPeer+3));
		}
		
		try{
			consuming = Integer.parseInt(line.substring(indexSecondPeer+5,indexSecondPeer+7));
		}catch(Exception e){
			consuming = Integer.parseInt(line.substring(indexSecondPeer+5,indexSecondPeer+6));
		}
		
		try{
			capacity = Double.parseDouble(line.substring(indexCapacity+3,indexCapacity+7));
		}catch(Exception e){
			capacity = Double.parseDouble(line.substring(indexCapacity+3,indexCapacity+6));
		}	
		
		if(capacityArray[consuming-1][providing-1]==-1){
			capacityArray[consuming-1][providing-1] = capacity;
			if(providing>=41){
				capacityArray[consuming-1][providing-1] = 0;
			}
		}
	}
	
	
	private double[][] createArray(int numPeers){
		double[][] capacity = new double[numPeers][numPeers];
		for(int i = 0; i < capacity.length; i++){
			for(int j = 0; j < capacity[i].length; j++){
				capacity[i][j] = -1;
			}			
		}
		return capacity;
	}
	
	private void printArray(double[][] array){
		for(int i = 0; i < array.length; i++){
			for(int j = 0; j < array[i].length; j++){
				System.out.print(array[i][j]+", ");
			}			
			System.out.println();
		}
	}	
	
}

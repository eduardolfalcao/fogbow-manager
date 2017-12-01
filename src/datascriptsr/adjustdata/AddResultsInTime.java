package adjustdata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import contention.PeerAndTime;
import contention.Status;

public class AddResultsInTime {
	
	public static void main(String[] args) {
		
		String [] nofVars = {"sdnof-10minutes","sdnof-7minutes"};
		int [] cycleVars = {10};
		int time = 60;
		
		for(String nof : nofVars){
			for(int cycle : cycleVars){
				String path = "/home/eduardolfalcao/workspace3/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/weightedNof/cycle"+cycle+"/"+nof+"/";
				readFileAndAddLine(path, time);
			}			
		}
	}
		
	
	private static void readFileAndAddLine(String path, int time){
		String outputPath = path+"with"+time+"sBreaks/";
		new File(path+"with"+time+"sBreaks").mkdir();
		
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();
		for(File file : listOfFiles){
			if(file.isFile()){
				System.out.println("Reading file: "+file.getName());
				try {
					String outputFile = outputPath+file.getName();
					System.out.println("Writing file: "+outputFile);
					new File(outputFile).createNewFile();
					BufferedReader bufReader = new BufferedReader(new FileReader(file));
					
					//skip first line
					String line = bufReader.readLine();
					write(outputFile,line+"\n");
					
					int turn = 1;
					
					PeerAndTime lastKey = null;
					Status lastValue = null;
					
					//read the rest of lines
					while((line = bufReader.readLine())!=null){
						String output = "";
						PeerAndTime key = readKey(line);
						Status value = readValues(line);
						while(key.getT()>turn*time){
							output += lastKey.getId()+","
									+ (turn*time)+","
									+ lastValue.getdTot()+","
									+ lastValue.getdFed()+","
									+ lastValue.getrFed()+","
									+ lastValue.getoFed()+","
									+ lastValue.getsFed()+"\n";
							turn++;		
						}
						if(key.getT()==turn*time){
							turn++;
						}
						output+=line+"\n";
						write(outputFile,output);
						lastKey = key;
						lastValue = value;
					}
					
					bufReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static void write(String outputFile, String output){
		try {
		    Files.write(Paths.get(outputFile), output.getBytes(), StandardOpenOption.APPEND);
		}catch (IOException e) {
		    e.printStackTrace();
		}
	}
	
	private static PeerAndTime readKey(String line){
//		id,t,dTot,dFed,rFed,oFed,sFed
//		p1,0,0,0,0,20,0
		String[] values = line.split(",");
		return new PeerAndTime(values[0], Integer.parseInt(values[1]));
	}
	
	private static Status readValues(String line){
//		id,t,dTot,dFed,rFed,oFed,sFed
//		p1,0,0,0,0,20,0
		String[] values = line.split(",");
		return new Status(Integer.parseInt(values[2]), Integer.parseInt(values[3]), Integer.parseInt(values[4]),
				Integer.parseInt(values[5]), Integer.parseInt(values[6]));
	}

}

package contention;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class DataReader {
	
	public static void main(String[] args) throws FileNotFoundException {
		String file = "/home/eduardo/Área de Trabalho/Experimentos-Doutorado/scripts r/done/40peers-20capacity/cycle10/sdnof/p1.csv";
		DataReader dr = new DataReader();
		Map<PeerAndTime, Status> results = new TreeMap<PeerAndTime, Status>();
		dr.readResults(results, new File(file));
		
		System.out.println(results.get(new PeerAndTime("p1", 92)));
		
		System.out.println("end");
	}
	
	private BufferedReader bufReader;
	
	public void readResults(Map<PeerAndTime, Status> results, File file){		
		try {
			bufReader = new BufferedReader(new FileReader(file));
			
			//skip first line
			String line = bufReader.readLine();
				
			//read the rest of lines
			while((line = bufReader.readLine())!=null){
				PeerAndTime key = readKey(line);
				Status value = readValues(line);
				results.put(key, value);				
			}
			
			bufReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private PeerAndTime readKey(String line){
//		id,t,dTot,dFed,rFed,oFed,sFed
//		p1,0,0,0,0,20,0
		String[] values = line.split(",");
		return new PeerAndTime(values[0], Integer.parseInt(values[1]));
	}
	
	private Status readValues(String line){
//		id,t,dTot,dFed,rFed,oFed,sFed
//		p1,0,0,0,0,20,0
		String[] values = line.split(",");
		return new Status(Integer.parseInt(values[3]), Integer.parseInt(values[5]));
	}	

}
package org.fogbowcloud.manager.experiments.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class CsvReader {
	
	private int time, demand, supply;
	
	public int getTime() {
		return time;
	}
	public int getDemand() {
		return demand;
	}
	public int getSupply() {
		return supply;
	}
	
	private String path;
	
	public CsvReader(String path) {
		this.path = path;
	}
	
	private List<PeerState> readFile(String filename){
		
		List<PeerState> entries = new ArrayList<PeerState>();
		
		String csvFile = path+filename;
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {
            br = new BufferedReader(new FileReader(csvFile));
            br.readLine();	//skip the first
            while ((line = br.readLine()) != null) {                
                String[] info = line.split(cvsSplitBy);	// use comma as separator             
                entries.add(new PeerState(info[0], Integer.parseInt(info[1]), Integer.parseInt(info[2]), Integer.parseInt(info[3]), Integer.parseInt(info[4])));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
		
		return entries;
	}
	
	private File[] getListOfFilenames(){
		File folder = new File(path);
		return folder.listFiles();
	}
	
	private void metricsCalculator(List<PeerState> entries){
		PeerState last = null, current = null;
		for(int i = 0; i < entries.size()-1; i++){
			last = entries.get(i);
			current = entries.get(i+1);
			
			int interval = current.getTime()-last.getTime();
			time += interval;
			demand += interval * last.getDemand();
			supply += interval * last.getSupply();
		}		
	}
	
//	private PeerState getNextTime(List<PeerState> entries){		
//		if(entries.size()>0){
//			long time = entries.get(0).getTime();
//			List<PeerState> entriesWithSameTime = new ArrayList<PeerState>();
//			Iterator<PeerState> it = entries.iterator();
//			while(it.hasNext()){
//				PeerState entry = it.next();
//				if(time == entry.getTime()){
//					entriesWithSameTime.add(entry);
//					it.remove();
//				}
//			}
//			return entriesWithSameTime;
//		}
//		else return null;		
//	}
	
	public static void main(String[] args) throws IOException {
		
		String managerConfigFilePath = args[0];
		File managerConfigFile = new File(managerConfigFilePath);
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(managerConfigFile);
		properties.load(input);
		
		CsvReader csv = new CsvReader(properties.getProperty(MonitorPeerState.OUTPUT_FOLDER));		
		
		for(File f : csv.getListOfFilenames()){
			List<PeerState> entries = new ArrayList<PeerState>();
			entries.addAll(csv.readFile(f.getName()));
			csv.metricsCalculator(entries);
		}
		System.out.println("d(total)="+csv.getDemand()+", s(total)="+csv.getSupply()+", time(total)="+csv.getTime());
		System.out.println("*********************************");
		System.out.println("k="+((double)csv.getDemand())/((double)csv.getSupply()));
		System.out.println("d(média)="+((double)csv.getDemand())/((double)csv.getTime()));
		
		
		
		
	}

}

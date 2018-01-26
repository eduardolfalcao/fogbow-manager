package org.fogbowcloud.manager.experiments.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.manager.experiments.monitor.MonitorPeerStateSingleton;

public class CsvReader {
	
	private int time, dTot, dFed, rFed, oFed, sFed;
	private int consumingTimeFed, consumingTimeTot, supplyingTime;	
	
	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public int getdTot() {
		return dTot;
	}

	public void setdTot(int dTot) {
		this.dTot = dTot;
	}

	public int getdFed() {
		return dFed;
	}

	public void setdFed(int dFed) {
		this.dFed = dFed;
	}
	
	public int getrFed() {
		return rFed;
	}

	public void setrFed(int rFed) {
		this.rFed = rFed;
	}


	public int getoFed() {
		return oFed;
	}

	public void setoFed(int oFed) {
		this.oFed = oFed;
	}

	public int getsFed() {
		return sFed;
	}

	public void setsFed(int sFed) {
		this.sFed = sFed;
	}

	public int getConsumingTimeFed() {
		return consumingTimeFed;
	}

	public void setConsumingTimeFed(int consumingTimeFed) {
		this.consumingTimeFed = consumingTimeFed;
	}
	
	public int getConsumingTimeTot() {
		return consumingTimeTot;
	}

	public void setConsumingTimeTot(int consumingTimeTot) {
		this.consumingTimeTot = consumingTimeTot;
	}

	public int getSupplyingTime() {
		return supplyingTime;
	}

	public void setSupplyingTime(int supplyingTime) {
		this.supplyingTime = supplyingTime;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
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
                entries.add(new PeerState(info[0], Integer.parseInt(info[1]), Integer.parseInt(info[2]), Integer.parseInt(info[3]), 
                		Integer.parseInt(info[4]), Integer.parseInt(info[5]), Integer.parseInt(info[6]), Integer.parseInt(info[7]),
                		Integer.parseInt(info[8]), Integer.parseInt(info[9])));
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
		return folder.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.endsWith(".csv");
		    }
		});
	}
	
	private Results metricsCalculator(List<PeerState> entries){
		PeerState last = null, current = null;
		int dFedI = 0, rFedI = 0, sFedI = 0;
		for(int i = 0; i < entries.size()-1; i++){
			last = entries.get(i);
			current = entries.get(i+1);
			
			int interval = current.getTime()-last.getTime();
			
			//computation of P avg (probability of being consumer)
			time += interval;
			if(last.getdFed()>0)
				consumingTimeFed += interval;
			
			//computation of C avg (offer)
			oFed += interval * last.getoFed();
			if(last.getoFed()>0)
				supplyingTime += interval;
			
			//computation of D avg (total demand)
			dTot += interval * last.getdTot();
			if(last.getdTot()>0)
				consumingTimeTot += interval;
			
			/** Fairness **/
			//received from federation
			rFedI += interval * last.getrFed();
			//donated to federation
			sFedI += interval * last.getsFed();
			
			/** Satisfaction **/
			//computation of D avg (total demand)
			dFedI += interval * last.getdFed();
			
		}
		
		rFed += rFedI;
		sFed += sFedI;
		dFed += dFedI;
		
		double fairness = -1, satisfaction = -1;
		if(sFedI>0)
			fairness = ((double)rFedI/(double)sFedI);
		if(dFedI>0)
			satisfaction = ((double)rFedI/(double)dFedI);
		
		return new Results(entries.get(0).getId(), fairness, satisfaction);
		
	}
	
	public static void main(String[] args) throws IOException {
		
		String managerConfigFilePath = args[0];
		File managerConfigFile = new File(managerConfigFilePath);
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(managerConfigFile);
		properties.load(input);
		
		CsvReader csv = new CsvReader(properties.getProperty(MonitorPeerStateSingleton.OUTPUT_FOLDER));
		
		List<Results> results = new ArrayList<Results>();
		
		for(File f : csv.getListOfFilenames()){
			List<PeerState> entries = new ArrayList<PeerState>();
			entries.addAll(csv.readFile(f.getName()));
			results.add(csv.metricsCalculator(entries));
		}
		
		double p = ((double)csv.getConsumingTimeFed())/((double)csv.getTime());
		double o = ((double)csv.getoFed())/((double)csv.getSupplyingTime());
		double d = ((double)csv.getdTot())/((double)csv.getConsumingTimeTot());
		double k = (p*d)/((1-p)*o);
		
		System.out.println("avg(P)="+p);
		System.out.println("avg(O)="+o);
		System.out.println("avg(D)="+d);
		System.out.println("expected(avg(K))="+k);
		
		double fairness = -1, satisfaction = -1;
		if(csv.getsFed()>0)
			 fairness = ((double)csv.getrFed()/(double)csv.getsFed());		
		if(csv.getdFed()>0)
			satisfaction = ((double)csv.getrFed()/(double)csv.getdFed());
		
		results.add(new Results("global", fairness, satisfaction));
		
		
		System.out.println("\n");
		for(Results r : results)
			System.out.println(r);
	}

}

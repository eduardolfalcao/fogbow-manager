package org.fogbowcloud.manager.experiments.data;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.model.DateUtils;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.compute.fake.FakeCloudComputePlugin;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderState;

public class MonitorPeerState {
	
	private static final String OUTPUT_DATA_MONITORING_PERIOD_KEY = "output_data_monitoring_period";
	public static final String OUTPUT_DATA_ENDING_TIME = "output_data_ending_time";
	public static final String OUTPUT_FOLDER = "output_folder";
	
	private DateUtils date = new DateUtils();
	private long initialTime, lastWrite, outputTime, endingTime;
	
	private Map<ManagerController, List<PeerState>> data;
	private List<ManagerController> fms;
	
	private String path;
	
	public MonitorPeerState(List<ManagerController> fms) {
		
		outputTime = TimeUnit.MILLISECONDS.toSeconds(Long.parseLong(fms.get(0).getProperties().getProperty(OUTPUT_DATA_MONITORING_PERIOD_KEY)));
		endingTime = TimeUnit.MILLISECONDS.toSeconds(Long.parseLong(fms.get(0).getProperties().getProperty(OUTPUT_DATA_ENDING_TIME)));
		path = fms.get(0).getProperties().getProperty(OUTPUT_FOLDER);		
		new File(path).mkdirs();
		
		data = new HashMap<ManagerController, List<PeerState>>();
		
		this.fms = new ArrayList<ManagerController>();	//only cooperative fm matters
		for(ManagerController fm : fms)
			if(!fm.getManagerId().contains("free-rider"))
				this.fms.add(fm);
		
		lastWrite = -1;
		initialTime = date.currentTimeMillis();
		
		for(ManagerController fm : this.fms){
			List<PeerState> states = new ArrayList<PeerState>();
			PeerState temp = getPeerState(fm);
			temp.setTime(0);
			states.add(temp);
			data.put(fm, states);			
			writeStates(fm,states);			
		}
		
		
		lastWrite = TimeUnit.MILLISECONDS.toSeconds(date.currentTimeMillis()-initialTime);
	}
	
	public void savePeerState() {		
		for(ManagerController fm : fms){
			int last = data.get(fm).size()-1;
			PeerState lastState = data.get(fm).get(last);
			PeerState currentState = getPeerState(fm);
			
			if(lastState.getdTot() != currentState.getdTot() ||
					lastState.getdFed() != currentState.getdFed() ||
					lastState.getrFed() != currentState.getrFed() ||
					lastState.getoFed() != currentState.getoFed() ||
					lastState.getsFed() != currentState.getsFed())
				data.get(fm).add(currentState);			
		}
		
		long now = TimeUnit.MILLISECONDS.toSeconds(date.currentTimeMillis()-initialTime);
		if((now - lastWrite)>outputTime){
			write();
			lastWrite = now;	
		}
		if(now >= endingTime){
			for(ManagerController fm : fms){
				List<PeerState> s = new ArrayList<PeerState>();
				s.add(new PeerState(fm.getManagerId(), (int)now, 0, 0, 0, 0, 0));
				s.add(new PeerState(fm.getManagerId(), (int)now, 0, 0, 0, 0, 0));
				writeStates(fm, s);
			}	
			System.exit(0);
		}
	}
	
	private PeerState getPeerState(ManagerController fm){		
		
		List<Order> orders = fm.getManagerDataStoreController().getAllOrders();
		
		int dTot = 0;	//O_r=i + P_r=i + F_r=i&&p=i + F_r=i&&p!=i 
		int dFed = 0;	//1 - max(0,dTot - maxCapacity) ou 2 - max(max(0,dTot - maxCapacity), rFed)
		int rFed = 0;	//F_r=i&&p!=i
		int oFed = 0;	//maxCapacity - F_r=i&&p=i
		int sFed = 0;	//F_r!=i&&p=i
		
		for(Order o : orders){
			if(o.getState().equals(OrderState.FULFILLED)){
				if(o.getRequestingMemberId().equals(fm.getManagerId())){	//F_r=i
					if(o.getProvidingMemberId().equals(fm.getManagerId())){	//F_r=i&&p=i
						dTot++;
						oFed--;
					}
					else{													//F_r=i&&p!=i
						dTot++;
						rFed++;
					}
				}
				else{														//F_r!=i
					if(o.getProvidingMemberId().equals(fm.getManagerId()))	//F_r!=i&&p==i
						sFed++;					
				}
			}
			else if((o.getState().equals(OrderState.OPEN)||o.getState().equals(OrderState.PENDING)) && 
					o.getRequestingMemberId().equals(fm.getManagerId()))	//O_r=i || P_r=i
				dTot++;	
		}
		
		int maxCapacity = fm.getMaxCapacityDefaultUser();
		oFed += maxCapacity;
		dFed = Math.max(0, dTot - maxCapacity);
		
		int now = (int)(TimeUnit.MILLISECONDS.toSeconds(date.currentTimeMillis()-initialTime));
		
		return new PeerState(fm.getManagerId(),now, dTot, dFed, rFed, oFed, sFed);
	}
	
	private void print(){
		Iterator<Entry<ManagerController, List<PeerState>>> it = data.entrySet().iterator();
		while(it.hasNext()){
			Entry<ManagerController, List<PeerState>> e = it.next();			
			int last = e.getValue().size()-1;
			PeerState lastState = e.getValue().get(last);			
			System.out.println(lastState);
		}			
	}
	
	private void write(){		
		Iterator<Entry<ManagerController, List<PeerState>>> it = data.entrySet().iterator();
		while(it.hasNext()){
			Entry<ManagerController, List<PeerState>> e = it.next();			
			ManagerController fm = e.getKey();
			List<PeerState> states = e.getValue();			
			writeStates(fm, states);			
		}			
	}
	
	private void writeStates(ManagerController fm, List<PeerState> states){
		String filePath = path + fm.getManagerId()+".csv";
		FileWriter w = null;
		if(lastWrite == -1)			//first write
			w = CsvGenerator.createHeader(filePath, "id", "t", "dTot", "dFed", "rFed", "oFed", "sFed");
		else if(states.size()>1){	//remove first state (already written), and write the rest
			w = CsvGenerator.getFile(filePath);
			states.remove(0);
		}
		else return;				//if theres only one state, keep updating
		for(PeerState s : states)
			CsvGenerator.outputValues(w, s.getId(), String.valueOf(s.getTime()),String.valueOf(s.getdTot()),
					String.valueOf(s.getdFed()),String.valueOf(s.getrFed()), String.valueOf(s.getoFed()), 
					String.valueOf(s.getsFed()));
		CsvGenerator.flushFile(w);
		
		if(states.size()>1){		//we just need to keep the last state
			PeerState last = states.get(states.size()-1); 
			states.clear();
			states.add(last);
		}
	}

}

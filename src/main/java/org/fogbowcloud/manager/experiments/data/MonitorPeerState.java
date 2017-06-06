package org.fogbowcloud.manager.experiments.data;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.model.DateUtils;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderState;

public class MonitorPeerState {
	
	private static final String OUTPUT_DATA_MONITORING_PERIOD_KEY = "output_data_monitoring_period";
	public static final String OUTPUT_FOLDER = "output_folder";
	private static final int CONVERSION_VALUE = 1000;
	
	private DateUtils date = new DateUtils();
	private long initialTime, lastWrite, outputTime;
	
	private Map<ManagerController, List<PeerState>> data;
	private List<ManagerController> fms;
	
	private String path;
	
	public MonitorPeerState(List<ManagerController> fms) {
		this.fms = new ArrayList<ManagerController>();	//only cooperative fm matters
		for(ManagerController fm : fms)
			if(!fm.getManagerId().contains("free-rider"))
				this.fms.add(fm);
		
		initialTime = date.currentTimeMillis();
		lastWrite = -1;
		data = new HashMap<ManagerController, List<PeerState>>();
		outputTime = Long.parseLong(fms.get(0).getProperties().getProperty(OUTPUT_DATA_MONITORING_PERIOD_KEY))/CONVERSION_VALUE;
		path = fms.get(0).getProperties().getProperty(OUTPUT_FOLDER);
		
		for(ManagerController fm : this.fms){
			List<PeerState> states = new ArrayList<PeerState>();
			PeerState temp = getPeerState(fm);
			temp.setTime(0);
			states.add(temp);
			data.put(fm, states);			
			writeStates(fm,states);			
		}
		lastWrite = (date.currentTimeMillis()-initialTime)/CONVERSION_VALUE;
	}
	
	public void savePeerState() {		
		for(ManagerController fm : fms){
			int last = data.get(fm).size()-1;
			PeerState lastState = data.get(fm).get(last);
			PeerState currentState = getPeerState(fm);
			
			if(lastState.getDemand() != currentState.getDemand() || lastState.getSupply() != currentState.getSupply())
				data.get(fm).add(currentState);			
		}
		//print();
		
		long now = (date.currentTimeMillis()-initialTime)/CONVERSION_VALUE;
		if((now - lastWrite)>outputTime){
			write();
			lastWrite = now;	//debugar o last write
		}
	}
	
	private PeerState getPeerState(ManagerController fm){		
		int demand = 0;
		List<Order> orders = fm.getManagerDataStoreController().getOrdersIn(OrderState.FULFILLED, OrderState.OPEN, OrderState.PENDING, OrderState.SPAWNING);
		for(Order o : orders){
			if(o.getRequestingMemberId().equals(fm.getManagerId()))
				demand++;
		}		
		int supply = Math.max(0, fm.getMaxCapacityDefaultUser() - demand);		
		int now = (int)((date.currentTimeMillis()-initialTime)/CONVERSION_VALUE);
		return new PeerState(fm.getManagerId(),now, demand, supply);
	}
	
	private void print(){
		Iterator<Entry<ManagerController, List<PeerState>>> it = data.entrySet().iterator();
		while(it.hasNext()){
			Entry<ManagerController, List<PeerState>> e = it.next();			
			int last = e.getValue().size()-1;
			PeerState lastState = e.getValue().get(last);			
			System.out.println(e.getKey()+" - time="+lastState.getTime()+", demand="+lastState.getDemand()+", supply="+lastState.getSupply());
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
		if(lastWrite == -1)	//first write
			w = CsvGenerator.createHeader(filePath, "id", "time", "demand", "supply");
		else{ 				//there are new states: write them and keep the last on list
			w = CsvGenerator.getFile(filePath);
			if(states.size()>1){	//here we could check if we should remove the last	
				states.remove(0);	//this one is already written
			}
			else{			//there are not new states: update the time of the last state
				if(states.get(0).getTime()!=0)
					CsvGenerator.removeLastLine(filePath);
				long now = (date.currentTimeMillis()-initialTime)/CONVERSION_VALUE;
				states.get(0).setTime((int) now);
			}
		}
		CsvGenerator.outputPeerStates(w, states);
		CsvGenerator.flushFile(w);
		
		if(states.size()>1){//we just need to keep the last state
			List<PeerState> temp = new ArrayList<PeerState>();
			temp.add(states.get(states.size()-1));
			states = temp;
		}
	}

}

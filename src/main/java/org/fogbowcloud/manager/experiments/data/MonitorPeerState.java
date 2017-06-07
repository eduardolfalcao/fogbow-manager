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
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.compute.fake.FakeCloudComputePlugin;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderState;

public class MonitorPeerState {
	
	private static final String OUTPUT_DATA_MONITORING_PERIOD_KEY = "output_data_monitoring_period";
	private static final String OUTPUT_DATA_ENDING_TIME = "output_data_ending_time";
	public static final String OUTPUT_FOLDER = "output_folder";
	private static final int CONVERSION_VALUE = 1000;
	
	private DateUtils date = new DateUtils();
	private long initialTime, lastWrite, outputTime, endingTime;
	
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
		endingTime = Long.parseLong(fms.get(0).getProperties().getProperty(OUTPUT_DATA_ENDING_TIME))/CONVERSION_VALUE;
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
		if(now >= endingTime){
			for(ManagerController fm : fms){
				List<PeerState> s = new ArrayList<PeerState>();
				s.add(new PeerState(fm.getManagerId(), (int)now, 0, 0, 0));
				s.add(new PeerState(fm.getManagerId(), (int)now, 0, 0, 0));
				writeStates(fm, s);
			}	
			System.exit(0);
		}
	}
	
	private PeerState getPeerState(ManagerController fm){		
		int demand = 0;
		List<Order> orders = fm.getManagerDataStoreController().getOrdersIn(OrderState.FULFILLED, OrderState.OPEN, OrderState.PENDING, OrderState.SPAWNING);
		for(Order o : orders){
			if(o.getRequestingMemberId().equals(fm.getManagerId()))
				demand++;
		}		
		int supply = getSupply(fm);	
		int maxCapacity = fm.getMaxCapacityDefaultUser();
		int now = (int)((date.currentTimeMillis()-initialTime)/CONVERSION_VALUE);
		return new PeerState(fm.getManagerId(),now, demand, supply, maxCapacity);
	}
	
	private int getSupply(ManagerController fm){
		ComputePlugin cp = fm.getComputePlugin();
		if(cp instanceof FakeCloudComputePlugin){
			FakeCloudComputePlugin fcp = (FakeCloudComputePlugin) cp;
			return fcp.getFreeQuota();
		}
		throw new IllegalArgumentException("The compute plugin is not instance of FakeCloudComputePlugin, and thus its not possible to get free quota.");
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
		if(lastWrite == 0)			//first write
			w = CsvGenerator.createHeader(filePath, "id", "time", "demand", "supply", "maxCapacity");
		else if(states.size()>1){	//remove first state (already written), and write the rest
			w = CsvGenerator.getFile(filePath);
			states.remove(0);
		}
		else return;				//if theres only one state, keep updating
		CsvGenerator.outputPeerStates(w, states);
		CsvGenerator.flushFile(w);
		
		if(states.size()>1){		//we just need to keep the last state
			List<PeerState> temp = new ArrayList<PeerState>();
			temp.add(states.get(states.size()-1));
			states = temp;
		}
	}

}

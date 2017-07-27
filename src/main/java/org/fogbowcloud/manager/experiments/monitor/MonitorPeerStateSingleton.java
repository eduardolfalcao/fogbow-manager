package org.fogbowcloud.manager.experiments.monitor;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.model.DateUtils;
import org.fogbowcloud.manager.core.plugins.compute.fake.FakeCloudComputePlugin;
import org.fogbowcloud.manager.experiments.data.CsvGenerator;
import org.fogbowcloud.manager.experiments.data.PeerState;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderState;

public class MonitorPeerStateSingleton{
	
	public static final String OUTPUT_FOLDER = "output_folder";
	public static final String OUTPUT_DATA_ENDING_TIME = "output_data_ending_time";
	
	private static MonitorPeerStateSingleton instance;
	private Map<String, MonitorPeerStateAssync> monitors;
	private boolean usedInit = false;
	
	private MonitorPeerStateSingleton(){}
	
	public static MonitorPeerStateSingleton getInstance(){
		if(instance==null)
			instance = new MonitorPeerStateSingleton();
		return instance;
	}
	
	public void init(List<ManagerController> managers){
		if(!usedInit){
			usedInit = true;
			monitors = new HashMap<String, MonitorPeerStateAssync>();
			for(ManagerController mc : managers)
				monitors.put(mc.getManagerId(), new MonitorPeerStateAssync(mc));			
		}		
	}
	
	public Map<String, MonitorPeerStateAssync> getMonitors() {
		return monitors;
	}
	
	public static final Logger LOGGER = Logger.getLogger(MonitorPeerStateAssync.class);
	
	public class MonitorPeerStateAssync {		
		
		
		public static final String OUTPUT_DATA_ENDING_TIME = "output_data_ending_time";
		public static final String OUTPUT_FOLDER = "output_folder";
		
		private DateUtils date = new DateUtils();
		private long initialTime, lastWrite, outputTime, endingTime;
		
		private Map<Integer,PeerState> states;		//time as key, PeerState as value
		private PeerState lastState;
		private Map<String,Order> currentOrders;
		
		private ManagerController fm;
		private String path;
		
		private boolean firstWrite = true;
		
		public MonitorPeerStateAssync(ManagerController fm) {
			
			this.fm = fm;
			
			endingTime = TimeUnit.MILLISECONDS.toSeconds(Long.parseLong(fm.getProperties().getProperty(OUTPUT_DATA_ENDING_TIME)));
			path = fm.getProperties().getProperty(OUTPUT_FOLDER);		
			new File(path).mkdirs();		
			
			currentOrders = new HashMap<String,Order>();
			states = new LinkedHashMap<Integer,PeerState>();
			int capacity = Integer.parseInt(fm.getProperties().getProperty(FakeCloudComputePlugin.COMPUTE_FAKE_QUOTA));
			lastState = new PeerState(fm.getManagerId(), 0, 0, 0, 0, capacity, 0);
			states.put(0, lastState);
			
			initialTime = date.currentTimeMillis();		
			writeStates(0);		
			lastWrite = TimeUnit.MILLISECONDS.toSeconds(date.currentTimeMillis()-initialTime);
		}
		
		public void savePeerState() {
			long now = TimeUnit.MILLISECONDS.toSeconds(date.currentTimeMillis()-initialTime);
			if(states.size()>1){			
				if((now - lastWrite)>outputTime){
					writeStates(now);
					lastWrite = now;	
				}			
			}
			if(now >= endingTime){
				states.put((int)now, new PeerState(fm.getManagerId(), (int)now, 0, 0, 0, 0, 0));
				writeStates(now);				
			}	
		}
		
		public void monitorOrder(Order o){
			LOGGER.info("<"+fm.getManagerId()+">: orderid("+o.getId()+") changed state to state("+o.getState()+") - requesting("+o.getRequestingMemberId()+") and providing("+o.getProvidingMemberId()+"); "
					+ "other attrs: instanceid("+o.getInstanceId()+"), elapsedTime("+o.getElapsedTime()+"), runtime("+o.getRuntime()+")");
			synchronized(currentOrders){
				if(o.getState().equals(OrderState.CLOSED) ||
						o.getState().equals(OrderState.FAILED) ||
						o.getState().equals(OrderState.DELETED)){
					currentOrders.remove(o.getId());
				} else{
					currentOrders.put(o.getId(), o);
				}
			}
			PeerState currentState = getPeerState();
			if(lastState.getdTot() != currentState.getdTot() ||
					lastState.getdFed() != currentState.getdFed() ||
					lastState.getrFed() != currentState.getrFed() ||
					lastState.getoFed() != currentState.getoFed() ||
					lastState.getsFed() != currentState.getsFed()){
				synchronized(states){
					states.put(currentState.getTime(),currentState);
				}
				lastState = currentState;
			}
			
		}
			
		private PeerState getPeerState() {
			
			
			int dTot = 0;	//O_r=i + P_r=i + F_r=i&&p=i + F_r=i&&p!=i 
			int dFed = 0;	//1 - max(0,dTot - maxCapacity) ou 2 - max(max(0,dTot - maxCapacity), rFed)
			int rFed = 0;	//F_r=i&&p!=i
			int oFed = 0;	//maxCapacity - F_r=i&&p=i
			int sFed = 0;	//F_r!=i&&p=i
			
			Map<String,Order> currentOrdersClone = null;
			synchronized(currentOrders){
				currentOrdersClone = new LinkedHashMap<String, Order>();
				currentOrdersClone.putAll(currentOrders);
			}
			
			for(Entry<String,Order> e : currentOrdersClone.entrySet()){
				Order order = e.getValue();
				OrderState state = order.getState();
				if(state.equals(OrderState.FULFILLED)){
					if(order.getRequestingMemberId().equals(fm.getManagerId())){	//F_r=i
						dTot++;
						if(order.getProvidingMemberId().equals(fm.getManagerId())){	//F_r=i&&p=i	
							oFed--;
						}
						else{														//F_r=i&&p!=i
							rFed++;
						}
					}
					else{														//F_r!=i
						if(order.getProvidingMemberId().equals(fm.getManagerId()))	//F_r!=i&&p==i
							sFed++;					
					}
				}
				else if((state.equals(OrderState.OPEN)||state.equals(OrderState.PENDING)) && 
						order.getRequestingMemberId().equals(fm.getManagerId()))	//O_r=i || P_r=i
					dTot++;	
			}
			
			int maxCapacity = fm.getMaxCapacityDefaultUser();
			oFed += maxCapacity;
			dFed = Math.max(0, dTot - maxCapacity);
			
			
			int now = (int)(TimeUnit.MILLISECONDS.toSeconds(date.currentTimeMillis()-initialTime));
			
			if(oFed<0){
				LOGGER.info("<"+fm.getManagerId()+">: ## time("+now+") current orders:"+currentOrders+"<"+fm.getManagerId()+">FIM\n\n");
				LOGGER.info("<"+fm.getManagerId()+">: FakeCloudComputePlugin instances: "+((FakeCloudComputePlugin)fm.getComputePlugin()).getInstances());
			}
			
			return new PeerState(fm.getManagerId(),now, dTot, dFed, rFed, oFed, sFed);		
		}
		
		private void writeStates(long now){
			String filePath = path + fm.getManagerId()+".csv";
			FileWriter w = null;
			boolean skipFirst = false;
			synchronized(states){
				if(firstWrite){			//first write
					w = CsvGenerator.createHeader(filePath, "id", "t", "dTot", "dFed", "rFed", "oFed", "sFed");
					firstWrite = false;
				}
				else if(states.size()>1){	//remove first state (already written), and write the rest
					w = CsvGenerator.getFile(filePath);
					skipFirst = true;				
				}
				else return;				//if theres only one state, keep updating
				
				Iterator<Entry<Integer, PeerState>> it = states.entrySet().iterator();
				while(it.hasNext()){
					PeerState s = it.next().getValue();
					if(skipFirst){
						it.remove();
						skipFirst = false;
						continue;
					}
					CsvGenerator.outputValues(w, s.getId(), String.valueOf(s.getTime()),String.valueOf(s.getdTot()),
							String.valueOf(s.getdFed()),String.valueOf(s.getrFed()), String.valueOf(s.getoFed()), 
							String.valueOf(s.getsFed()));
					
					if(it.hasNext())	//we just need to keep the last state
						it.remove();			
				}
			}
			CsvGenerator.flushFile(w);
		}

	}	
}



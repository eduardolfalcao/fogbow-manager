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
import org.fogbowcloud.manager.core.ManagerControllerXP;
import org.fogbowcloud.manager.core.model.DateUtils;
import org.fogbowcloud.manager.core.plugins.compute.fake.FakeCloudComputePlugin;
import org.fogbowcloud.manager.experiments.data.CsvGenerator;
import org.fogbowcloud.manager.experiments.data.PeerState;
import org.fogbowcloud.manager.experiments.scheduler.WorkloadScheduler;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderState;
import org.fogbowcloud.manager.occi.order.OrderXP;

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
	
	public void init(List<ManagerControllerXP> managers, boolean forTest){
		if(!usedInit || forTest){
			usedInit = true;
			monitors = new HashMap<String, MonitorPeerStateAssync>();
			for(ManagerControllerXP mc : managers)
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
		private Map<String,OrderState> currentOrderStates;
		
		private ManagerControllerXP fm;
		private String path;
		
		private boolean firstWrite = true;
		private boolean finished = false;
		
		public MonitorPeerStateAssync(ManagerControllerXP fm) {
			
			this.fm = fm;
			
			endingTime = TimeUnit.MILLISECONDS.toSeconds(Long.parseLong(fm.getProperties().getProperty(OUTPUT_DATA_ENDING_TIME)));
			path = fm.getProperties().getProperty(OUTPUT_FOLDER);		
			new File(path).mkdirs();		
			
			currentOrders = new HashMap<String,Order>();
			currentOrderStates = new HashMap<String,OrderState>();
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
				if((now - lastWrite)>outputTime && !finished){
					writeStates(now);
					lastWrite = now;	
				}			
			}
			if(now >= endingTime){
				states.put((int)now, new PeerState(fm.getManagerId(), (int)now, 0, 0, 0, 0, 0));
				LOGGER.info("<"+fm.getManagerId()+">: finishing, now="+now+", endingTime="+endingTime);
				writeStates(endingTime);
				finished = true;
				
			}	
		}
		
		public void monitorOrder(Order order){
			OrderXP o = (OrderXP) order;
			synchronized(currentOrders){
				//do nothing if state hasnt changed
				if(currentOrderStates.get(o.getId())!=null && currentOrderStates.get(o.getId()).equals(o.getState())){					
					return;
				}				
				
				long delay = (o.getPreviousElapsedTime()+o.getCurrentElapsedTime())-o.getRuntime();
				LOGGER.info("<"+fm.getManagerId()+">: changed state of orderid("+o.getId()+")  to "+o.getState()+" - requesting("+o.getRequestingMemberId()+") and providing("+o.getProvidingMemberId()+"); "
						+ "other attrs: instanceid("+o.getInstanceId()+"), elapsedTime("+(o.getPreviousElapsedTime()+o.getCurrentElapsedTime())+"), runtime("+o.getRuntime()+") "
								+ (delay>=0?(", delay("+delay+")"):""));
				
				if(o.getState().equals(OrderState.CLOSED) ||
						o.getState().equals(OrderState.FAILED) ||
						o.getState().equals(OrderState.DELETED)){					
					currentOrders.remove(o.getId());
					currentOrderStates.remove(o.getId());
				} else{					
					currentOrders.put(o.getId(), o);					
					currentOrderStates.put(o.getId(), o.getState());
				}
			}
			PeerState currentState = getPeerState();
			if(lastState.getdTot() != currentState.getdTot() ||
					lastState.getdFed() != currentState.getdFed() ||
					lastState.getrFed() != currentState.getrFed() ||
					lastState.getoFed() != currentState.getoFed() ||
					lastState.getsFed() != currentState.getsFed()){
				LOGGER.info("<"+fm.getManagerId()+">: currentState @@ is different from last state ==> currentState("+currentState+"), lastState("+lastState+")");
				synchronized(states){
					states.put(currentState.getTime(),currentState);
				}
				lastState = currentState;
			}else{
				LOGGER.info("<"+fm.getManagerId()+">: currentState ## is still the same of last state ==> currentState("+currentState+"), lastState("+lastState+")");
			}
			
		}
			
		protected PeerState getPeerState() {
			
			
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
						//the order may be fulfilled but the providing member may be null
						if(order.getProvidingMemberId()==null){
							LOGGER.error("<"+fm.getManagerId()+">: order("+order.getId()+") has no providing member ==> "+(OrderXP)order);
							LOGGER.info("<"+fm.getManagerId()+">: monitorOrder2 #@@# orderId("+order.getId()+") " + order.getAddress());
						}
						if(order.getProvidingMemberId().equals(fm.getManagerId())){	//F_r=i&&p=i	
							oFed--;
						}
						else{														//F_r=i&&p!=i
							rFed++;
						}
					}
					else{															//F_r!=i
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
			oFed = Math.max(oFed, 0);
			dFed = Math.max(0, dTot - maxCapacity);			
			
			int now = (int)(TimeUnit.MILLISECONDS.toSeconds(date.currentTimeMillis()-initialTime));
			
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
		
		protected Map<String, Order> getCurrentOrders() {
			return currentOrders;
		}
		
		protected Map<String, OrderState> getCurrentOrderStates() {
			return currentOrderStates;
		}
		
		protected PeerState getLastState() {
			return lastState;
		}
		
//		private OrderXP cloneOrder(OrderXP o){
//			Token clonedToke = new Token(o.getFederationToken().getAccessId(),o.getFederationToken().getUser(), o.getFederationToken().getExpirationDate(), )
//			
//			OrderXP clonedOrder = new OrderXP(o.getId()
//		}

	}	
}



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
import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.CapacityControllerPlugin;
import org.fogbowcloud.manager.core.plugins.accounting.AccountingInfo;
import org.fogbowcloud.manager.core.plugins.capacitycontroller.fairnessdriven.FairnessDrivenCapacityController;
import org.fogbowcloud.manager.core.plugins.compute.fake.FakeCloudComputePlugin;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderConstants;
import org.fogbowcloud.manager.occi.order.OrderState;

public class MonitorExperimentMetrics {
	
	private static final String SPECIFIC_OUTPUT_FOLDER = "output/results";
	private String path;
	private DateUtils date = new DateUtils();
	private long initialTime, currentTime, lastTime;
	private boolean beginning = true;
	
	private List<ManagerController> fms;
	private Map<ManagerController,Metrics> data;
	
	public MonitorExperimentMetrics(List<ManagerController> fms){
		this.fms = fms;
		data = new HashMap<ManagerController,Metrics>();
		
		path = fms.get(0).getProperties().getProperty(MonitorPeerState.OUTPUT_FOLDER)+SPECIFIC_OUTPUT_FOLDER;
		currentTime = lastTime = initialTime = date.currentTimeMillis();
	}

	public void saveMetrics() {
		long now = (date.currentTimeMillis()-initialTime)/MonitorPeerState.CONVERSION_VALUE;
		for(ManagerController fm : fms)
			data.put(fm, getMetrics(fm), now);		
		print(data, path, now);		
	}
	
	private Metrics getMetrics(ManagerController fm, long now){
		
		List<AccountingInfo> accounting = fm.getAccountingInfo(null, OrderConstants.COMPUTE_TERM);
		double consumed = 0, donated = 0, fairness = 0;
		for(AccountingInfo  acc : accounting){
			if(acc.getProvidingMember().equals(fm.getManagerId()) && !acc.getRequestingMember().equals(fm.getManagerId()))	//then its donation
				donated += acc.getUsage();
			else if(!acc.getProvidingMember().equals(fm.getManagerId()) && acc.getRequestingMember().equals(fm.getManagerId()))
				consumed += acc.getUsage();
		}		
		fairness = FairnessDrivenCapacityController.getFairness(consumed, donated);		
		
		CapacityControllerPlugin capacityControllerPlugin = fm.getCapacityControllerPlugin();
		double maxQuota = 0, globalQuota = 0;
		globalQuota = capacityControllerPlugin.getMaxCapacityToSupply(new FederationMember("any id that doesnt exist"));
		maxQuota = fm.getMaxCapacityDefaultUser();		
		
		//TODO multiplicar pelo tempo!		
		double requested = getRequested(fm);
		
		Metrics last = data.get(fm);
		if(last!=null)
			requested += last.getRequested();
		
		double satisfaction = requested>0? consumed/requested : -1;
		
		return new Metrics(consumed,donated,requested,fairness,satisfaction,maxQuota,globalQuota);
	}
	
	private double getRequested(ManagerController fm){
		int demandFed = 0;
		int open = 0;
		List<Order> orders = fm.getManagerDataStoreController().getOrdersIn(OrderState.FULFILLED, OrderState.OPEN, OrderState.PENDING, OrderState.SPAWNING);
		for(Order o : orders){
			if(o.getState() == OrderState.FULFILLED && o.getState() == OrderState.PENDING && o.getState() == OrderState.SPAWNING){
				if(o.getRequestingMemberId().equals(fm.getManagerId()) && !o.getProvidingMemberId().equals(fm.getManagerId()))
					demandFed++;
			}
			else if(o.getState() == OrderState.OPEN){
				if(o.getRequestingMemberId().equals(fm.getManagerId()))
					open++;
			}
		}		
		int freeQuota = ((FakeCloudComputePlugin)fm.getComputePlugin()).getFreeQuota();		
		return Math.max(0, demandFed + open - freeQuota);		
	}
	
//	private void print(Map<ManagerController,Metrics> fmMap){
//		Iterator<Entry<ManagerController, Metrics>> it = fmMap.entrySet().iterator();
//		while(it.hasNext()){
//			Entry<ManagerController, Metrics> e = it.next();
//			System.out.println(e.getKey()+" - consumed="+e.getValue().getConsumed()+", donated="+e.getValue().getDonated()+", fairness="+e.getValue().getFairness()+
//					", globaQuota="+e.getValue().getGlobalQuota()+", maxQuota="+e.getValue().getMaxQuota());
//		}			
//	}
	
	private void print(Map<ManagerController,Metrics> fmMap, String path, long time){		
		String filePath = path + ".csv";
		FileWriter w = null;
		if(beginning == true){			//first write
			w = CsvGenerator.createHeader(filePath, "id", "time","consumedFed", "donatedFed", "requestedFed", "fairness", "satisfaction", "globaQuota", "maxQuota");
			beginning = false;
		}
		else
			w = CsvGenerator.getFile(filePath);
		
		Iterator<Entry<ManagerController, Metrics>> it = fmMap.entrySet().iterator();
		while(it.hasNext()){
			Entry<ManagerController, Metrics> e = it.next();
			CsvGenerator.outputValues(w, e.getKey().getManagerId(), String.valueOf(time), String.valueOf(e.getValue().getConsumed()), String.valueOf(e.getValue().getDonated()),
					String.valueOf(e.getValue().getRequested()), String.valueOf(e.getValue().getFairness()), String.valueOf(e.getValue().getSatisfaction()), 
					String.valueOf(e.getValue().getGlobalQuota()), String.valueOf(e.getValue().getMaxQuota()));
		}
		CsvGenerator.flushFile(w);
	}

}

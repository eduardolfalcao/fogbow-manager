package org.fogbowcloud.manager.experiments.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.CapacityControllerPlugin;
import org.fogbowcloud.manager.core.plugins.accounting.AccountingInfo;
import org.fogbowcloud.manager.core.plugins.capacitycontroller.fairnessdriven.FairnessDrivenCapacityController;
import org.fogbowcloud.manager.occi.order.OrderConstants;

public class MonitorExperimentMetrics {
	
	private List<ManagerController> fms;
	private Map<Integer, Map<ManagerController,Metrics>> data;
	private int timeStep = 1;
	
	public MonitorExperimentMetrics(List<ManagerController> fms){
		this.fms = fms;
		data = new HashMap<Integer, Map<ManagerController,Metrics>>();
	}

	public void saveMetrics() {
		Map<ManagerController,Metrics> innerMap = new HashMap<ManagerController,Metrics>();
		data.put(timeStep, innerMap);
		for(ManagerController fm : fms){			
			innerMap.put(fm, getMetrics(fm));
		}
		//print(innerMap);
		timeStep++;		
	}
	
	private Metrics getMetrics(ManagerController fm){
		
		List<AccountingInfo> accounting = fm.getAccountingInfo(null, OrderConstants.COMPUTE_TERM);
		double consumed = 0, donated = 0, fairness = 0;
		for(AccountingInfo  acc : accounting){
			if(acc.getProvidingMember().equals(fm.getManagerId()))	//then its donation
				donated += acc.getUsage();
			else
				consumed += acc.getUsage();
		}		
		fairness = FairnessDrivenCapacityController.getFairness(consumed, donated);		
		
		CapacityControllerPlugin capacityControllerPlugin = fm.getCapacityControllerPlugin();
		double maxQuota = 0, globalQuota = 0;
		globalQuota = capacityControllerPlugin.getMaxCapacityToSupply(new FederationMember("any id that doesnt exist"));
		maxQuota = fm.getMaxCapacityDefaultUser();		
		
		return new Metrics(consumed,donated,fairness,maxQuota,globalQuota);
	}
	
	private void print(Map<ManagerController,Metrics> fmMap){
		Iterator<Entry<ManagerController, Metrics>> it = fmMap.entrySet().iterator();
		while(it.hasNext()){
			Entry<ManagerController, Metrics> e = it.next();
			System.out.println(e.getKey()+" - consumed="+e.getValue().getConsumed()+", donated="+e.getValue().getDonated()+", fairness="+e.getValue().getFairness()+
					", globaQuota="+e.getValue().getGlobalQuota()+", maxQuota="+e.getValue().getMaxQuota());
		}			
	}

}

package org.fogbowcloud.manager.experiments.data;

public class Metrics {
	
	
	private double consumed, donated; 	//, requested;
	private double fairness;			//, satisfaction;
	private double maxQuota, globalQuota;
	
	
	
	public Metrics(double consumed, double donated, double fairness,
			double maxQuota, double globalQuota) {
		super();
		this.consumed = consumed;
		this.donated = donated;
		this.fairness = fairness;
		this.maxQuota = maxQuota;
		this.globalQuota = globalQuota;
	}
	
	public double getConsumed() {
		return consumed;
	}
	public void setConsumed(double consumed) {
		this.consumed = consumed;
	}
	public double getDonated() {
		return donated;
	}
	public void setDonated(double donated) {
		this.donated = donated;
	}
//	public double getRequested() {
//		return requested;
//	}
//	public void setRequested(double requested) {
//		this.requested = requested;
//	}
	public double getFairness() {
		return fairness;
	}
	public void setFairness(double fairness) {
		this.fairness = fairness;
	}
//	public double getSatisfaction() {
//		return satisfaction;
//	}
//	public void setSatisfaction(double satisfaction) {
//		this.satisfaction = satisfaction;
//	}
	public double getMaxQuota() {
		return maxQuota;
	}
	public void setMaxQuota(double maxQuota) {
		this.maxQuota = maxQuota;
	}
	public double getGlobalQuota() {
		return globalQuota;
	}
	public void setGlobalQuota(double globalQuota) {
		this.globalQuota = globalQuota;
	}
	
	

}

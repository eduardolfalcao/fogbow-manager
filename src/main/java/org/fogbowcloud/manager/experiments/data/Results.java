package org.fogbowcloud.manager.experiments.data;

public class Results {

	private String id;
	private double fairness, satisfaction;
	
	public Results(String id, double fairness, double satisfaction) {
		super();
		this.id = id;
		this.fairness = fairness;
		this.satisfaction = satisfaction;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getFairness() {
		return fairness;
	}

	public void setFairness(double fairness) {
		this.fairness = fairness;
	}

	public double getSatisfaction() {
		return satisfaction;
	}

	public void setSatisfaction(double satisfaction) {
		this.satisfaction = satisfaction;
	}
	
	@Override
	public String toString() {
		return "id="+id+", fairness="+fairness+", satisfaction="+satisfaction;
	}
	
	
	
}

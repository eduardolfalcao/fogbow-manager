package org.fogbowcloud.manager.core.plugins.accounting;

import org.json.JSONException;
import org.json.JSONObject;


public class AccountingInfo {
	
	private String user;
	private String requestingMember;
	private String providingMember;
	private double usage;
	private int currentInstances;
	private double quota;
	
	public AccountingInfo(String user, String requestingMember, String providingMember) {
		this.user = user;
		this.requestingMember = requestingMember;
		this.providingMember = providingMember;
		this.usage = 0;
		this.currentInstances = 0;
		this.quota = Double.MAX_VALUE;
	}
	
	public AccountingInfo(AccountingInfo clone) {
		this.user = clone.getUser();
		this.requestingMember = clone.getRequestingMember();
		this.providingMember = clone.getProvidingMember();
		this.usage = clone.getUsage();
		this.currentInstances = clone.getCurrentInstances();
		this.quota = clone.getQuota();
	}	
	
	public void addConsumption(double consumption) {
		this.usage += consumption;
	}

	public String getUser() {
		return user;
	}

	public String getRequestingMember() {
		return requestingMember;
	}

	public String getProvidingMember() {
		return providingMember;
	}

	public double getUsage() {
		return usage;
	}
	
	public int getCurrentInstances(){
		return currentInstances;
	}
	
	public void incrementCurrentInstances(){
		this.currentInstances++;
	}
	
	public void setCurrentInstances(int currentInstances) {
		this.currentInstances = currentInstances;
	}
	
	public void setQuota(double quota) {
		this.quota = quota;
	}
	
	public double getQuota() {
		return quota;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AccountingInfo) {
			AccountingInfo other = (AccountingInfo) obj;
			return getUser().equals(other.getUser())
					&& getRequestingMember().equals(other.getRequestingMember())
					&& getProvidingMember().equals(other.getProvidingMember())
					&& (getUsage() - other.getUsage() <= 0.00000001)
					&& (getQuota() - other.getQuota() <= 0.00000001); 
		}
		return false;
	}

	public JSONObject toJSON() throws JSONException {
		return new JSONObject().put("user", user).put("requestingMember", requestingMember)
				.put("providingMember", providingMember).put("usage", usage);
	}
	
	public static AccountingInfo fromJSON(String accountingEntryJSON) throws JSONException {
		JSONObject jsonObject = new JSONObject(accountingEntryJSON);
		AccountingInfo accountingEntry = new AccountingInfo(jsonObject.optString("user"),
				jsonObject.optString("requestingMember"), jsonObject.optString("providingMember"));
		accountingEntry.addConsumption(Double.parseDouble(jsonObject.optString("usage")));
		return accountingEntry;
	}
	
	@Override
	public String toString() {
		return "User=" + getUser() + "; requestingMember=" + getRequestingMember()
				+ "; providingMember=" + getProvidingMember() + "; usage=" + getUsage()
				+ "; currentInstances=" + getCurrentInstances()
				+ "; quota=" + getQuota();
	}
}
package org.fogbowcloud.manager.core.plugins.accounting;

import org.json.JSONException;
import org.json.JSONObject;


public class AccountingInfo {
	
	private String user;
	private String requestingMember;
	private String providingMember;
	private double usage;
	private int currentInstances;
	
	public AccountingInfo(String user, String requestingMember, String providingMember) {
		this.user = user;
		this.requestingMember = requestingMember;
		this.providingMember = providingMember;
		this.usage = 0;
		this.currentInstances = 0;
	}
	
	public void addConsumption(double consuption) {
		this.usage += consuption;
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
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AccountingInfo) {
			AccountingInfo other = (AccountingInfo) obj;
			return getUser().equals(other.getUser())
					&& getRequestingMember().equals(other.getRequestingMember())
					&& getProvidingMember().equals(other.getProvidingMember())
					&& (getUsage() - other.getUsage() <= 0.00000001); 
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
				+ "; providingMember=" + getProvidingMember() + "; usage=" + getUsage();
	}
}
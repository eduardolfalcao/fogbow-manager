package org.fogbowcloud.manager.experiments.scheduler.model;

import java.util.ArrayList;
import java.util.List;

public class Peer {
	
	private String peerId;
	private List<User> users;
	
	public Peer(String peerId){
		this.peerId = peerId;
		users = new ArrayList<User>();
	}
	
	@Override
	public String toString() {
		return "Peer: "+peerId+" - "+users+"\n";
	}
	
	@Override
	public boolean equals(Object obj){
		if(!(obj instanceof Peer))
			return false;
		
		Peer peer = (Peer) obj;
		if(!peerId.equals(peer.getPeerId()))
			return false;
		else
			return true;
	}
	
	@Override
	public int hashCode() {
		String id = peerId.substring(1, peerId.length());
		return Integer.parseInt(id);
	}

	public String getPeerId() {
		return peerId;
	}

	public List<User> getUsers() {
		return users;
	}
	
}
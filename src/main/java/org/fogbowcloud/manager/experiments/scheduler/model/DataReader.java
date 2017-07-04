package org.fogbowcloud.manager.experiments.scheduler.model;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;		

public class DataReader {
	
	public static void main(String[] args) throws FileNotFoundException {
		String file = "/home/eduardolfalcao/Área de Trabalho/Dropbox/Doutorado/Disciplinas/Projeto de Tese 5/workload-generator/tool/workload_clust_5spt_10ups_gwa-t1.txt";
		DataReader dr = new DataReader();
		List<Peer> peers = new ArrayList<Peer>();
		dr.readWorkload(peers, file);
		
		for(Peer peer : peers){
			for(User user : peer.getUsers()){
				Collections.sort(user.getJobs());
				for(Job job : user.getJobs()){
					if(job.getSubmitTime()<600)
						System.out.println(peer.getPeerId()+"; "+user.getUserId()+"; "+job.getId()+"; "+job.getSubmitTime());
				}
			}
		}
		
		System.out.println("end");
	}
	
	private BufferedReader bufReader;
	
	public void readWorkload(List<Peer> peers, String file){		
		try {
			bufReader = new BufferedReader(new FileReader(file));
			
			//skip first line
			String line = bufReader.readLine();
				
			//read the rest of lines
			while((line = bufReader.readLine())!=null){
				Peer peer = readPeer(line);
				addTaskOnPeersList(peers, peer);				
			}
			
			bufReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Peer readPeer(String line){
//		SubmitTime RunTime JobID UserID PeerID TraceID Cluster.IAT Cluster.JRT Cluster.TRT
//		300 1332 1 U251 P26 gwa-t11 C2 C2 C4
		String[] values = line.split(" ");
		String submitTime = values[0];
		String runtime = values[1];
		String jobId = values[2];
		String userId = values[3];
		String peerId = values[4].toLowerCase();
		
		Task task = new Task(Integer.parseInt(runtime));
		Job job = new Job(peerId, userId, Integer.parseInt(jobId), Integer.parseInt(submitTime));		
		job.getTasks().add(task);
		User user = new User(userId);
		user.getJobs().add(job);
		Peer peer = new Peer(peerId);
		peer.getUsers().add(user);
		
		return peer;		
	}
	
	private void addTaskOnPeersList(List<Peer> peers, Peer newPeer){
		
		if(peers.contains(newPeer)){
			Peer peerOfList = peers.get(peers.indexOf(newPeer)); 
			User newUser = newPeer.getUsers().get(0);
			
			if(peerOfList.getUsers().contains(newUser)){				
				int userIndex = peerOfList.getUsers().indexOf(newUser);
				User userOfList = peerOfList.getUsers().get(userIndex);
				Job newJob = newUser.getJobs().get(0);
				
				if(userOfList.getJobs().contains(newJob)){
					int jobIndex = userOfList.getJobs().indexOf(newJob);
					Job jobOfList = userOfList.getJobs().get(jobIndex);
					jobOfList.getTasks().add(new Task(newJob.getTasks().get(0).getRuntime()));
				}
				else{	//the job doesn't exist on the user yet
					userOfList.getJobs().add(newJob);
				}				
			}
			else{		//the user doesn't exist on the peer yet
				peerOfList.getUsers().add(newUser);
			}
		}
		else{			//the peer doesn't exist on the list yet
			peers.add(newPeer);
		}
	}
	
	

}

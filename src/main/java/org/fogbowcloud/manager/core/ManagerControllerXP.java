package org.fogbowcloud.manager.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.model.FederationMember;
import org.fogbowcloud.manager.core.plugins.CapacityControllerPlugin;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.compute.fake.FakeCloudComputePlugin;
import org.fogbowcloud.manager.experiments.monitor.MonitorPeerStateSingleton.MonitorPeerStateAssync;
import org.fogbowcloud.manager.occi.instance.Instance;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.model.ErrorType;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.fogbowcloud.manager.occi.model.ResourceRepository;
import org.fogbowcloud.manager.occi.model.ResponseConstants;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.occi.order.Order;
import org.fogbowcloud.manager.occi.order.OrderAttribute;
import org.fogbowcloud.manager.occi.order.OrderConstants;
import org.fogbowcloud.manager.occi.order.OrderState;
import org.fogbowcloud.manager.occi.order.OrderXP;
import org.fogbowcloud.manager.occi.storage.StorageLink;
import org.fogbowcloud.manager.xmpp.ManagerPacketHelper;

public class ManagerControllerXP extends ManagerController{
	
	
	
	private static final Logger LOGGER = Logger.getLogger(ManagerControllerXP.class);
	
	private ManagerTimer monitorTimer;

	public ManagerControllerXP(Properties properties) {
		this(properties, null);
	}
	
	public ManagerControllerXP(Properties properties, ScheduledExecutorService executor) {
		super(properties, executor,true);
		LOGGER.setLevel(Level.INFO);		
		this.ordersToBeCreated = new ArrayList<Order>();
		this.createOrderOnDBExecutor = new ScheduledThreadPoolExecutor(1);
		this.monitorTimer = new ManagerTimer(executor);
	}

	public ComputePlugin getComputePlugin(){
		return computePlugin;
	}
	
	
	@Override
	public int hashCode() {
		return managerId.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof ManagerControllerXP){
			ManagerControllerXP mc = (ManagerControllerXP) obj;
			if(mc.getManagerId().equals(this.managerId))
				return true;
		}			
		return false;
	}
	
	@Override
	public int getMaxCapacityDefaultUser() {
		if(computePlugin instanceof FakeCloudComputePlugin)
			return ((FakeCloudComputePlugin)computePlugin).getQuota();
		else return CapacityControllerPlugin.MAXIMUM_CAPACITY_VALUE_ERROR;
	}
	
	@Override
	public void removeOrderForRemoteMember(String accessId, String orderId) {
		LOGGER.info("<"+managerId+">: "+"Removing order for remote member: " + orderId);
		Order order = managerDataStoreController.getOrder(orderId, false);
		if (order != null && order.getInstanceId() != null) {
			try {
				Token token = localIdentityPlugin.createToken(mapperPlugin.getLocalCredentials(accessId));
				String instanceId = order.getInstanceId();
				if (order.getResourceKind().equals(OrderConstants.COMPUTE_TERM)) {
					((FakeCloudComputePlugin)computePlugin).removeInstance(token, instanceId, order);					
				} else if (order.getResourceKind().equals(OrderConstants.STORAGE_TERM)) {
					storagePlugin.removeInstance(token, instanceId);
				} else if (order.getResourceKind().equals(OrderConstants.NETWORK_TERM)) {
					networkPlugin.removeInstance(token, instanceId);
				}
			} catch (Exception e) { 
			}
		}
		managerDataStoreController.excludeOrder(order.getId());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, String> getExternalServiceAddresses(String tokenId) {
		return null;
	}	
		
	@Override
	protected void removeInstance(String instanceId, Order order, String resourceKind) {				
		List<StorageLink> storageLinks = this.managerDataStoreController.getAllStorageLinkByInstance(instanceId, resourceKind);
		if (!storageLinks.isEmpty()) {
			throw new OCCIException(ErrorType.BAD_REQUEST,
					ResponseConstants.EXISTING_ATTACHMENT + " Attachment IDs : " 
					+ StorageLink.Util.storageLinksToString(storageLinks));			
		}
				
		Token localToken = getFederationUserToken(order);
		if (isFulfilledByLocalMember(order)) {	
			if (resourceKind.equals(OrderConstants.COMPUTE_TERM)) {
				((FakeCloudComputePlugin)this.computePlugin).removeInstance(localToken, instanceId, order);
			} else if (resourceKind.equals(OrderConstants.STORAGE_TERM)) {
				this.storagePlugin.removeInstance(localToken, instanceId);
			} else if (resourceKind.equals(OrderConstants.NETWORK_TERM)) {
				this.networkPlugin.removeInstance(localToken, instanceId);
			}
		} else {
			LOGGER.info("<"+managerId+">: Trying to remove remote instance with orderId("+order.getId()+"), and instanceId("+order.getInstanceId()+").");
			removeRemoteInstance(order);
		}		
		
		instanceRemoved(order);
	}

	protected void instanceRemoved(Order o) {
		
		OrderXP order = (OrderXP) o;
		
		boolean isRemoving = true;
		order.updateElapsedTime(isRemoving);
		LOGGER.info("<"+managerId+">: "+"checking if the order("+order.getId()+"), with instance("+order.getInstanceId()+"), requested by "+order.getRequestingMemberId()+
				" and provided by "+ order.getProvidingMemberId() +" will be removed or rescheduled. runtime: " + order.getRuntime()+
				", previousElapsedTime: "+order.getPreviousElapsedTime()+", currentElapsedTime: "+order.getCurrentElapsedTime()+", fulfilledTime: "+order.getFulfilledTime());
		
		if (order.getResourceKind().equals(OrderConstants.COMPUTE_TERM)) {
			updateAccounting();
			benchmarkingPlugin.remove(order.getInstanceId());			
		}
		
		String instanceId = order.getInstanceId();
		order.setInstanceId(null);
		order.setProvidingMemberId(null);

		if (order.getState().equals(OrderState.DELETED)  || !order.isLocal()) {			
			managerDataStoreController.excludeOrder(order.getId());
		} else if (isPersistent(order)) {
			boolean finished = (order.getPreviousElapsedTime() + order.getCurrentElapsedTime()) >= order.getRuntime();
			if(!finished){	//and is local
				LOGGER.info("<"+managerId+">: "+"Order: " + order.getId() + ", setting state to " + OrderState.OPEN);
				order.setState(OrderState.OPEN);
				if (!orderSchedulerTimer.isScheduled()) {
					triggerOrderScheduler();
				}
			} else {
				LOGGER.info("<"+managerId+">: "+"Order: " + order.getId() + ", setting state to " + OrderState.CLOSED);
				order.setState(OrderState.CLOSED);
			}
		} 
		
		this.managerDataStoreController.updateOrder(order);
		if (instanceId != null) {
			this.managerDataStoreController.removeAllStorageLinksByInstance(
					normalizeFogbowResourceId(instanceId), order.getResourceKind());			
		}
	}

	@Override
	public void queueServedOrder(String requestingMemberId, List<Category> categories, Map<String, String> xOCCIAtt,
			String orderId, Token requestingUserToken) {		
		
		normalizeBatchId(requestingMemberId, xOCCIAtt);

		LOGGER.info("<"+managerId+">: "+"Queueing order("+orderId+") for requesting member: " + requestingMemberId + " with requestingToken " + requestingUserToken);
		Order order = new OrderXP(orderId, requestingUserToken, categories, xOCCIAtt, false, requestingMemberId, managerId);
		
		if(!isThereEnoughQuota(requestingMemberId)){
			LOGGER.info("<"+managerId+"> i am not donating to "+requestingMemberId);
			ManagerPacketHelper.replyToServedOrder(order, packetSender);
			return;
		}
		
		managerDataStoreController.addOrder(order);

		if (!orderSchedulerTimer.isScheduled()) {
			triggerOrderScheduler();	//1s or 2s of delay
		}
	}

	@Override
	public void preemption(final Order orderToBePreempted) {
		LOGGER.info("<"+managerId+">: preempting "+orderToBePreempted.getId()+" from "+orderToBePreempted.getRequestingMemberId());
		removeInstance(orderToBePreempted.getInstanceId(), orderToBePreempted, OrderConstants.COMPUTE_TERM);
		
		ManagerPacketHelper.preemptOrder(orderToBePreempted.getRequestingMemberId(), orderToBePreempted, packetSender,new AsynchronousOrderCallback() {			
			@Override
			public void success(String instanceId) {
				LOGGER.info("<"+managerId+">: "+"Servered order id " + orderToBePreempted.getId() + " from " + orderToBePreempted.getRequestingMemberId() + " preempted!");
			}
			
			@Override
			public void error(Throwable t) {
				LOGGER.warn("<"+managerId+">: "+"Error while preempting servered order id " + orderToBePreempted.getId() + " from " + orderToBePreempted.getRequestingMemberId());
			}
		});
	}
	
	public void remoteMemberPreemptedOrder(String orderId){
		LOGGER.info("<"+managerId+">: "+"Removing local order (id="+orderId+") preempted by remote member");		
		Order o = managerDataStoreController.getOrder(orderId);
		instanceRemoved(o);		
	}

	@Override
	protected void triggerServedOrderMonitoring() {
		if (forTest) { return; }
		final long servedOrderMonitoringPeriod = ManagerControllerHelper.getServerOrderMonitoringPeriod(this.properties);

		servedOrderMonitoringTimer.scheduleWithFixedDelay(new TimerTask() {
			@Override
			public void run() {	
				try {
					monitorServedOrders();
				} catch (Throwable e) {
					LOGGER.error("<"+managerId+">: "+"Error while monitoring served orders", e);
				}
			}
		}, 1000, servedOrderMonitoringPeriod);	//the faster order has at least 1s
	}	

	@Override
	public Instance getInstanceForRemoteMember(String instanceId) {
		LOGGER.info("<"+managerId+">: "+"Getting instance " + instanceId + " for remote member.");
		try {
			Order servedOrder = managerDataStoreController.getOrderByInstance(instanceId);
			Token federationUserToken = getFederationUserToken(servedOrder);
			String orderResourceKind = servedOrder != null ? servedOrder.getResourceKind(): null;
			Instance instance = null;
			if (orderResourceKind == null || orderResourceKind.equals(OrderConstants.COMPUTE_TERM)) {
				instance = computePlugin.getInstance(federationUserToken, instanceId);
				if (servedOrder != null) {
					// in this experiments there wont exist any VM, and thus no sshService
					Category osCategory = getImageCategory(servedOrder.getCategories());
					if (osCategory != null) {
						instance.addResource(ResourceRepository.createImageResource(osCategory.getTerm()));
					}
				}
				return instance;				
			} else if (orderResourceKind != null && orderResourceKind.equals(OrderConstants.STORAGE_TERM)) {
				instance = storagePlugin.getInstance(federationUserToken, instanceId);
			} else if (orderResourceKind != null && orderResourceKind.equals(OrderConstants.NETWORK_TERM)) {
				instance = networkPlugin.getInstance(federationUserToken, instanceId);
			}
			return instance;
		} catch (OCCIException e) {
			LOGGER.warn("<"+managerId+">: "+"Exception while getting instance " + instanceId + " for remote member.", e);
			if (e.getStatus().getCode() == HttpStatus.SC_NOT_FOUND) {
				return null;
			}
			throw e;
		}
	}
	
	@Override
	public void removeInstanceForRemoteMember(String instanceId) {
		Order order = managerDataStoreController.getOrderByInstance(instanceId);
		String resourceKind = order != null ? order.getAttValue(OrderAttribute.RESOURCE_KIND.getValue()) : null;
		resourceKind = resourceKind == null ? OrderConstants.COMPUTE_TERM : resourceKind;
		LOGGER.info("<"+managerId+">: "+"Removing instance(" + resourceKind + ") with instanceId(" + instanceId + ") "
				+ "and orderId("+order.getId()+") for remote member.");

		Token federationUserToken = getFederationUserToken(order);
		if (OrderConstants.COMPUTE_TERM.equals(resourceKind)) {
			updateAccounting();
			benchmarkingPlugin.remove(instanceId);
			((FakeCloudComputePlugin)computePlugin).removeInstance(federationUserToken, instanceId, order);
		} else if (OrderConstants.STORAGE_TERM.equals(resourceKind)) {
			storagePlugin.removeInstance(federationUserToken, instanceId);
		} else if (OrderConstants.NETWORK_TERM.equals(resourceKind)) {
			networkPlugin.removeInstance(federationUserToken, instanceId);			
		}
		
		instanceRemoved(order);		
	}	
	
	//a thread that each second try to submit	
	private boolean isCreateOrderOnDBTimerScheduled = false;
	private List<Order> ordersToBeCreated;	
	private ScheduledExecutorService createOrderOnDBExecutor;
	protected void triggerOrderCreationOnDB() {	
		Runnable run = new Runnable() {
			public void run() {
				boolean isEmpty = false;
				synchronized(ordersToBeCreated){
					isEmpty = ordersToBeCreated.isEmpty();
				}
				if(!isEmpty){
					createOrdersOnDB();
				}
			}
		};
		createOrderOnDBExecutor.scheduleWithFixedDelay(run, 0, 1000, TimeUnit.MILLISECONDS);
	}
	
	private void createOrdersOnDB(){
		List<Order> ordersToBeCreatedClone = new ArrayList<Order>();
		synchronized(ordersToBeCreated){
			ordersToBeCreatedClone.addAll(ordersToBeCreated);
			ordersToBeCreated.clear();
		}
		for(Order o : ordersToBeCreatedClone){
			managerDataStoreController.addOrder(o);
			LOGGER.info("<"+managerId+">: "+"Just created order("+o.getId()+") on bd");
		}
		
		if (!orderSchedulerTimer.isScheduled()) {
			triggerOrderScheduler();
		}
	}
	
	@Override
	public List<Order> createOrders(String federationAccessTokenStr, List<Category> categories,
			Map<String, String> xOCCIAtt) {
		Token federationToken = getTokenFromFederationIdP(federationAccessTokenStr);
		Integer instanceCount = Integer.valueOf(xOCCIAtt.get(OrderAttribute.INSTANCE_COUNT.getValue()));
		xOCCIAtt.put(OrderAttribute.BATCH_ID.getValue(), String.valueOf(UUID.randomUUID()));

		List<Order> currentOrders = new ArrayList<Order>();
		for (int i = 0; i < instanceCount; i++) {
			String orderId = String.valueOf(UUID.randomUUID());
			Order order = new Order(orderId, federationToken, new LinkedList<Category>(categories),
					new HashMap<String, String>(xOCCIAtt), true, properties.getProperty("xmpp_jid"));
			currentOrders.add(order);
		}
		
		synchronized(ordersToBeCreated){
			ordersToBeCreated.addAll(currentOrders);
		}		
		
		if (!isCreateOrderOnDBTimerScheduled) {
			triggerOrderCreationOnDB();
		}
		
		return currentOrders;
	}

	@Override
	protected void createAsynchronousRemoteInstance(final Order o, List<FederationMember> allowedMembers) {
		
		final OrderXP order = (OrderXP) o;
		
		if (packetSender == null) {
			return;
		}

		FederationMember member = memberPickerPlugin.pick(allowedMembers);
		if (member == null) {
			return;
		}

		final String memberAddress = member.getId();

		Map<String, String> xOCCIAttCopy = new HashMap<String, String>(order.getxOCCIAtt());
		List<Category> categoriesCopy = new LinkedList<Category>(order.getCategories());
		populateWithManagerPublicKey(xOCCIAttCopy, categoriesCopy);
		order.setProvidingMemberId(memberAddress);
		order.setState(OrderState.PENDING);
		this.managerDataStoreController.updateOrder(order);

		LOGGER.info("<"+managerId+">: "+"Submiting order(" + order.getId() + ") with runtime("+order.getRuntime()+") to member " + memberAddress);		
		this.managerDataStoreController.addOrderSyncronous(order.getId(), dateUtils.currentTimeMillis(), order.getProvidingMemberId());
		ManagerPacketHelper.asynchronousRemoteOrder(managerId, order.getId(), categoriesCopy, xOCCIAttCopy, memberAddress, 
				federationIdentityPlugin.getForwardableToken(order.getFederationToken()), 
				packetSender, new AsynchronousOrderCallback() {
					@Override
					public void success(String instanceId) {
						LOGGER.info("<"+managerId+">: "+"The order(" + order.getId() + ")  with runtime("+order.getRuntime()
								+ ") forwarded to " + memberAddress + " gets instance "+ instanceId);
						if (managerDataStoreController.isOrderSyncronous(order.getId()) == false) {
							return;
						}
						if (instanceId == null) {
							if (order.getState().equals(OrderState.PENDING)) {
								order.setState(OrderState.OPEN);
								managerDataStoreController.updateOrder(order);
							}
							return;
						}

						if (order.getState().in(OrderState.DELETED)) {
							return;
						}

						// reseting time stamp
						managerDataStoreController.updateOrderSyncronous(order.getId(), dateUtils.currentTimeMillis());

						order.setInstanceId(instanceId);
						order.setProvidingMemberId(memberAddress);
						
						boolean isStorageOrder = OrderConstants.STORAGE_TERM.equals(order.getResourceKind());
						boolean isNetworkOrder = OrderConstants.NETWORK_TERM.equals(order.getResourceKind());						
						if (isStorageOrder || isNetworkOrder) {
							managerDataStoreController.removeOrderSyncronous(order.getId());
							order.setState(OrderState.FULFILLED);
							managerDataStoreController.updateOrder(order);							
							return;
						}
						
						try {
							LOGGER.info("<"+managerId+">: will execBenchmark on received instance, order "+order.getId()+", and instance "+order.getInstanceId());
							execBenchmark(order);
						} catch (Throwable e) {
							LOGGER.error("<"+managerId+">: "+"Error while executing the benchmark in " + instanceId
									+ " from member " + memberAddress + ".", e);
							if (order.getState().equals(OrderState.PENDING)) {
								order.setState(OrderState.OPEN);
								managerDataStoreController.updateOrder(order);
							}
							return;
						}
					}

					@Override
					public void error(Throwable t) {
						LOGGER.debug("<"+managerId+">: "+"The order " + order + " forwarded to " + memberAddress
								+ " gets error ", t);
						if (order.getState().equals(OrderState.PENDING)) {
							order.setState(OrderState.OPEN);
						}
						order.setProvidingMemberId(null);
						managerDataStoreController.updateOrder(order);
					}
				});
	}

	@Override
	protected void execBenchmark(final Order order) {

		benchmarkExecutor.execute(new Runnable() {
			@Override
			public void run() {
				Instance instance = new Instance(order.getInstanceId());
				instance.addAttribute("occi.compute.cores", "8");
				instance.addAttribute("occi.compute.memory", "16");
				
				//no need to wait for ssh service

				try {
					benchmarkingPlugin.run(order.getGlobalInstanceId(), instance);
				} catch (Exception e) {
					LOGGER.debug("<"+managerId+">: "+"Couldn't run benchmark.", e);
				}
				
				//no need to deal with keys

				if (order.isLocal() && !isFulfilledByLocalMember(order)) {
					removeAsynchronousRemoteOrders(order, false);
					managerDataStoreController.removeOrderSyncronous(order.getId());
				}

				if (!order.isLocal()) {
					ManagerPacketHelper.replyToServedOrder(order, packetSender);
				}

				if (!order.getState().in(OrderState.DELETED)) {
					order.setState(OrderState.FULFILLED);
					managerDataStoreController.updateOrder(order);
				}

				LOGGER.debug("<"+managerId+">: "+"Fulfilled order: " + order);
			}
		});
		
		if (order.isLocal() && !instanceMonitoringTimer.isScheduled()) {
			triggerInstancesMonitor();
		}

		if (!order.isLocal() && !servedOrderMonitoringTimer.isScheduled()) {
			triggerServedOrderMonitoring();
		}
	}	

	
	public void triggerWorkloadMonitor(final MonitorPeerStateAssync monitor) {
		final long monitorPeriod = ManagerControllerHelper.getPeerStateOutputPeriod(properties);
		
		monitorTimer.scheduleWithFixedDelay(new TimerTask() {
			@Override
			public void run() {	
				try {					
					monitor.savePeerState();
				} catch (Throwable e) {
					LOGGER.error("Error while monitoring workload", e);
				}
			}
		}, 0, monitorPeriod);
	}	

	@Override
	protected boolean createLocalInstanceWithFederationUser(Order o) {
		
		OrderXP order = (OrderXP) o;		
		
		LOGGER.info("<"+managerId+">: "+"Submitting order " + order.getId() + " requested by "+order.getRequestingMemberId()+","
				+ "with runtime "+order.getRuntime()+" with federation user locally.");
		
		FederationMember member = null;
		boolean isRemoteDonation = !properties.getProperty("xmpp_jid").equals(order.getRequestingMemberId());
		
		try {
			member = getFederationMember(order.getRequestingMemberId());
		} catch (Exception e) {
		}

		if (isRemoteDonation && !validator.canDonateTo(member, order.getFederationToken())) {
			return false;
		}

		try {
			return createInstance(order);
		} catch (Exception e) {
			LOGGER.warn("<"+managerId+">: "+"Could not create instance for order("+order.getId()+") with federation user locally");
			return false;
		}
	}

	@Override
	protected boolean createInstance(Order order) {

		LOGGER.debug("<"+managerId+">: "+"Submiting order with categories: " + order.getCategories() + " and xOCCIAtt: "
				+ order.getxOCCIAtt() + " for requesting member: " + order.getRequestingMemberId()
				+ " with requestingToken " + order.getRequestingMemberId());

		boolean isComputeOrder = OrderConstants.COMPUTE_TERM.equals(order.getResourceKind());
		
		for (String keyAttributes : OrderAttribute.getValues()) {
			order.getxOCCIAtt().remove(keyAttributes);
		}
		
		Token federationUserToken = getFederationUserToken(order);
		
		if (isComputeOrder) {
			try {
				//	REVERSE TUNNEL MUST NOT BE CREATED SINCE WE ARE NOT WORKING WITH REAL VMS
				
				String localImageId = getLocalImageId(order.getCategories(), federationUserToken);
				List<Category> categories = new LinkedList<Category>();
				for (Category category : order.getCategories()) {
					if (category.getScheme().equals(OrderConstants.TEMPLATE_OS_SCHEME)) {
						continue;
					}
					categories.add(category);
				}
				
				Map<String, String> xOCCIAttCopy = new HashMap<String, String>(order.getxOCCIAtt());

				//there's no public key
				
				String instanceId = computePlugin.requestInstance(federationUserToken, categories, xOCCIAttCopy,
						localImageId);
				order.setState(OrderState.SPAWNING);
				order.setInstanceId(instanceId);
				order.setProvidingMemberId(properties.getProperty(ConfigurationConstants.XMPP_JID_KEY));
				this.managerDataStoreController.updateOrder(order);
				execBenchmark(order);
				return instanceId != null;
			} catch (OCCIException e) {
				ErrorType errorType = e.getType();
				if (errorType == ErrorType.QUOTA_EXCEEDED) {
					LOGGER.warn("<"+managerId+">: "+"Order("+order.getId()+") requested by "+order.getRequestingMemberId()+" "
							+ "and provided by "+order.getProvidingMemberId()+", failed locally for quota exceeded. ==> "+e.getMessage());
					ArrayList<Order> ordersWithInstances = new ArrayList<Order>(
							managerDataStoreController.getOrdersIn(OrderState.FULFILLED, OrderState.DELETED));
					Order orderToPreempt = prioritizationPlugin.takeFrom(order, ordersWithInstances);
					LOGGER.info("<"+managerId+">: "+"Order to be preempted: "+orderToPreempt);
					if (orderToPreempt == null) {
						throw e;
					}
					preemption(orderToPreempt);
					checkInstancePreempted(federationUserToken, orderToPreempt);
					return createInstance(order);
				} else if (errorType == ErrorType.UNAUTHORIZED) {
					LOGGER.warn("<"+managerId+">: "+"Order failed locally for user unauthorized.", e);
					return false;
				} else if (errorType == ErrorType.BAD_REQUEST) {
					LOGGER.warn("<"+managerId+">: "+"Order failed locally for image not found.", e);
					return false;
				} else if (errorType == ErrorType.NO_VALID_HOST_FOUND) {
					LOGGER.warn(
							"<"+managerId+">: "+"Order failed because no valid host was found," + " we will try to wake up a sleeping host.",
							e);
					wakeUpSleepingHosts(order);
					return false;
				} else {
					LOGGER.warn("<"+managerId+">: "+"Order failed locally for an unknown reason.", e);
					return false;
				}
			}
		} else {
			//on this experiment i will not create network or storage orders
			return false;
		}
	}	
}


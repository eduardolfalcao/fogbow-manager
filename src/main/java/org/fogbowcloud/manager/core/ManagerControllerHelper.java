package org.fogbowcloud.manager.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.MainHelper;
import org.fogbowcloud.manager.core.model.DateUtils;
import org.fogbowcloud.manager.occi.order.Order;

public class ManagerControllerHelper {

	private static final long DEFAULT_INSTANCE_MONITORING_PERIOD = 120000; // 2 minutes
	private static final long DEFAULT_SERVED_ORDER_MONITORING_PERIOD = 120000; // 2 minutes
	private static final long DEFAULT_PEER_STATE_MONITORING_PERIOD = 2000; // 2 seconds
	private static final long DEFAULT_SCHEDULER_PERIOD = 1000; // 1 seconds
	private static final long DEFAULT_WORKLOAD_PERIOD = 2000; // 1 seconds
	private static final long DEFAULT_BOOTSTRAPPING_PERIOD_KEY = 10000; // 10 seconds
	
	private static final Logger LOGGER = Logger.getLogger(ManagerControllerHelper.class);
		
	public ManagerControllerHelper() {}
	
	public static long getInstanceMonitoringPeriod(Properties properties) {
		String instanceMonitoringPeriodStr = properties
				.getProperty(ConfigurationConstants.INSTANCE_MONITORING_PERIOD_KEY);
		final long instanceMonitoringPeriod = instanceMonitoringPeriodStr == null ? DEFAULT_INSTANCE_MONITORING_PERIOD
				: Long.valueOf(instanceMonitoringPeriodStr);
		return instanceMonitoringPeriod;
	}
	
	public static long getWorkloadMonitorPeriod(Properties properties) {
		String workloadMonitorPeriodStr = properties
				.getProperty(ConfigurationConstants.WORKLOAD_MONITOR_PERIOD_KEY);
		final long workloadMonitorPeriod = workloadMonitorPeriodStr == null
				? DEFAULT_WORKLOAD_PERIOD : Long.valueOf(workloadMonitorPeriodStr);
		return workloadMonitorPeriod;
	}	
	
	public static long getSchedulerPeriod(Properties properties) {
		String schedulerPeriodStr = properties
				.getProperty(ConfigurationConstants.SCHEDULER_PERIOD_KEY);
		final long schedulerPeriod = schedulerPeriodStr == null
				? DEFAULT_SCHEDULER_PERIOD : Long.valueOf(schedulerPeriodStr);
		return schedulerPeriod;
	}	
	
	public static long getPeerStateMonitoringPeriod(Properties properties) {
		String metricsMonitoringPeriodStr = properties
				.getProperty(ConfigurationConstants.PEER_STATE_MONITORING_PERIOD_KEY);
		final long metricsMonitoringPeriod = metricsMonitoringPeriodStr == null
				? DEFAULT_PEER_STATE_MONITORING_PERIOD : Long.valueOf(metricsMonitoringPeriodStr);
		return metricsMonitoringPeriod;
	}	
	
	public static long getBootstrappingPeriod(Properties properties) {
		String bootstrappingPeriodStr = properties
				.getProperty(ConfigurationConstants.BOOTSTRAPPING_PERIOD_KEY);
		final long bootstrappingPeriod = bootstrappingPeriodStr == null
				? DEFAULT_BOOTSTRAPPING_PERIOD_KEY : Long.valueOf(bootstrappingPeriodStr);
		return bootstrappingPeriod;
	}
	
	public static long getServerOrderMonitoringPeriod(Properties properties) {
		String servedOrderMonitoringPeriodStr = properties
				.getProperty(ConfigurationConstants.SERVED_ORDER_MONITORING_PERIOD_KEY);
		final long servedOrderMonitoringPeriod = servedOrderMonitoringPeriodStr == null
				? DEFAULT_SERVED_ORDER_MONITORING_PERIOD : Long.valueOf(servedOrderMonitoringPeriodStr);
		return servedOrderMonitoringPeriod;
	}
	
	// inner classes
	public class MonitoringHelper {
		
		public static final String MAXIMUM_ORDER_ATTEMPTS_PROPERTIES = "maximum_order_attempts";
		public static final int MAXIMUM_ORDER_ATTEMPTS_DEFAULT = 1000;
		private static final long GRACE_TIME = 1000 * 60 * 20; // 20 minutes 
		
		private DateUtils dateUtils = new DateUtils();
		private Map<String, OrderAttempt> orderAttempts;
		private Integer maximumAttempts;
		
		public MonitoringHelper(Properties properties) {
			this.orderAttempts = new HashMap<String, OrderAttempt>();
			try {
				this.maximumAttempts = Integer.parseInt(properties.getProperty(MAXIMUM_ORDER_ATTEMPTS_PROPERTIES));				
			} catch (Exception e) {
				this.maximumAttempts = MAXIMUM_ORDER_ATTEMPTS_DEFAULT;
			}
		}
		
		public synchronized void addFailedMonitoringAttempt(Order order) {
			String orderId = order.getId();
			OrderAttempt orderAttempt = this.orderAttempts.get(orderId);
			if (orderAttempt == null) {
				this.orderAttempts.put(orderId, new OrderAttempt(1, this.dateUtils.currentTimeMillis())); 
			} else {
				orderAttempt.setAttempts(orderAttempt.getAttempts() + 1);
				LOGGER.debug("Order (" + orderId + ") failed in monitoring: " 
						+ orderAttempt.getAttempts() + " attempts of at most " + this.maximumAttempts);
			}
		}
		
		public synchronized boolean isMaximumFailedMonitoringAttempts(Order order) {
			String orderId = order.getId();			
			OrderAttempt orderAttempt = this.orderAttempts.get(orderId);			
			if (orderAttempt != null && orderAttempt.getAttempts() >= this.maximumAttempts) {
				this.orderAttempts.remove(orderId);
				return true;
			}
			return false;
		}
		
		public synchronized void eraseFailedMonitoringAttempts(Order order) {
			String orderId = order.getId();
			OrderAttempt orderAttempt = this.orderAttempts.get(orderId);
			if (orderAttempt != null) {
				this.orderAttempts.remove(orderId);
				LOGGER.debug("Order (" + orderId + ") reset attempts in monitoring.");
			}
		}
		
		public synchronized void checkFailedMonitoring(long monitorPeriod) {
			try {
				Set<String> keys = new HashSet<String> (this.orderAttempts.keySet());
				for (String orderId : keys) {
					OrderAttempt orderAttempt = this.orderAttempts.get(orderId);
					if (orderAttempt.getInitTime() > this.getTimeout(monitorPeriod)) {
						this.orderAttempts.remove(orderId);
					}
				}				
			} catch (Exception e) {
				LOGGER.warn("Error while checking orders attempts.", e);
			}
		}
		
		protected long getTimeout(long monitorPeriod) {
			return this.dateUtils.currentTimeMillis() + (monitorPeriod * this.maximumAttempts);// + GRACE_TIME;
		}
		
		protected Map<String, OrderAttempt> getOrderFailedAttempts() {
			return orderAttempts;
		}
		
		public void setDateUtils(DateUtils dateUtils) {
			this.dateUtils = dateUtils;
		}
		
		protected class OrderAttempt {

			private Integer attempts;
			private long initTime;

			public OrderAttempt(Integer attempts, long initTime) {
				super();
				this.attempts = attempts;
				this.initTime = initTime;
			}

			public Integer getAttempts() {
				return attempts;
			}
			
			public void setAttempts(Integer attempts) {
				this.attempts = attempts;
			}

			public long getInitTime() {
				return initTime;
			}

		}
		
	}
}

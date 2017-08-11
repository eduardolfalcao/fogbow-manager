package org.fogbowcloud.manager.xmpp;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class PreemptionWarnHandler extends AbstractQueryHandler {
	
	private static final Logger LOGGER = Logger.getLogger(PreemptionWarnHandler.class);

	private ManagerController facade;

	public PreemptionWarnHandler(ManagerController facade) {
		super(ManagerXmppComponent.PREEMPTIONWARN_NAMESPACE);
		this.facade = facade;
	}
	
	@Override
	public IQ handle(IQ query) {
		String orderId = query.getElement().element("query").element(ManagerPacketHelper.ORDER_EL)
				.elementText("id");
		
		LOGGER.info("<"+facade.getManagerId()+">: "+"I had one order/instance preempted. OrderId: " + orderId);
		
		IQ response = IQ.createResultIQ(query);
		try {
			facade.remoteMemberPreemptedOrder(orderId);
		} catch (OCCIException e) {
			response.setError(ManagerPacketHelper.getCondition(e));
		}
		return response;
	}
	
}

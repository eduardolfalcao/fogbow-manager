package org.fogbowcloud.manager.xmpp;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.ManagerControllerXP;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class QuotaExceededHandler extends AbstractQueryHandler {
	
	private static final Logger LOGGER = Logger.getLogger(QuotaExceededHandler.class);

	private ManagerController facade;

	public QuotaExceededHandler(ManagerController facade) {
		super(ManagerXmppComponent.QUOTAEXCEEDED_NAMESPACE);
		this.facade = facade;
	}
	
	@Override
	public IQ handle(IQ query) {
		String orderId = query.getElement().element("query").element(ManagerPacketHelper.ORDER_EL)
				.elementText("id");
		String providingMemberId = query.getFrom().toBareJID();
		
		LOGGER.info("<"+facade.getManagerId()+">: "+providingMemberId+" doesnt have quota for order " + orderId);
		
		IQ response = IQ.createResultIQ(query);
		try {
			((ManagerControllerXP)facade).makeOrderOpen(orderId);
		} catch (OCCIException e) {
			response.setError(ManagerPacketHelper.getCondition(e));
		}
		return response;
	}
	
}

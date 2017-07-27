package org.fogbowcloud.manager.xmpp;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.plugins.benchmarking.VanillaBenchmarkingPlugin;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoveOrderHandler extends AbstractQueryHandler {

	private static final Logger LOGGER = Logger.getLogger(RemoveOrderHandler.class);
	private ManagerController facade;

	public RemoveOrderHandler(ManagerController facade) {
		super(ManagerXmppComponent.REMOVEORDER_NAMESPACE);
		this.facade = facade;
	}

	@Override
	public IQ handle(IQ query) {
		String orderId = query.getElement().element("query").element(ManagerPacketHelper.ORDER_EL)
				.elementText("id");
		String accessId = query.getElement().element("query").element("token").elementText("accessId");
		
		LOGGER.info("<"+facade.getManagerId()+">: "+"Someone asked me to remove order with id: " + orderId);
		
		IQ response = IQ.createResultIQ(query);
		try {
			facade.removeOrderForRemoteMember(accessId, orderId);
		} catch (OCCIException e) {
			response.setError(ManagerPacketHelper.getCondition(e));
		}
		return response;
	}
}

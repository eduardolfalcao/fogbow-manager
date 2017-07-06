package org.fogbowcloud.manager.xmpp;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.plugins.accounting.FCUAccountingPlugin;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.model.Token;
import org.jamppa.component.handler.AsyncQueryHandler;
import org.xmpp.packet.IQ;

public class OrderInstanceHandler extends AsyncQueryHandler {

	private static final Logger LOGGER = Logger.getLogger(OrderInstanceHandler.class);
	
	private ManagerController facade;
	private String managerId;

	public OrderInstanceHandler(ManagerController facade) {
		super(ManagerXmppComponent.ORDER_NAMESPACE);
		this.facade = facade;
		this.managerId = facade.getManagerId();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleAsync(IQ iq) {
		Element queryEl = iq.getElement().element("query");
		List<Category> categories = new LinkedList<Category>();
		List<Element> categoriesEl = queryEl.elements("category");
		for (Element categoryEl : categoriesEl) {
			Category category = new Category(categoryEl.elementText("term"),
					categoryEl.elementText("scheme"), categoryEl.elementText("class"));
			categories.add(category);
		}
		Map<String, String> xOCCIAtt = new HashMap<String, String>();
		List<Element> attributesEl = queryEl.elements("attribute");
		for (Element attributeEl : attributesEl) {
			xOCCIAtt.put(attributeEl.attributeValue("var"), attributeEl.element("value").getText());
		}	
		
		String orderId = queryEl.element(ManagerPacketHelper.ORDER_EL)
				.element("id").getText();

		Element tokenEl = queryEl.element(ManagerPacketHelper.TOKEN_EL);
		Token userToken = null;
		if (tokenEl != null) {
			Element userEl = tokenEl.element(ManagerPacketHelper.USER_EL);
			userToken = new Token(tokenEl.elementText("accessId"), new Token.User(userEl.elementText(ManagerPacketHelper.ID_EL), 
					userEl.elementText(ManagerPacketHelper.NAME_EL)), null, new HashMap<String, String>());
		}
		
		LOGGER.warn("<"+managerId+">: Received request with "+orderId+" from "+iq.getFrom().toBareJID());
		
		facade.queueServedOrder(iq.getFrom().toBareJID(), categories, xOCCIAtt, orderId,
				userToken);
	}
}

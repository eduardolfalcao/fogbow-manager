package org.fogbowcloud.manager.occi.order;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.fogbowcloud.manager.occi.model.Category;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestOrderXP {
	
	Map<String, String> xOCCIAttributes;
	
	@Before
	public void setupXOCCI(){
		xOCCIAttributes = new HashMap<String, String>();
		xOCCIAttributes.put(OrderAttribute.RESOURCE_KIND.getValue(), OrderConstants.COMPUTE_TERM);
		xOCCIAttributes.put(OrderAttribute.INSTANCE_COUNT.getValue(), "1");
		xOCCIAttributes.put(OrderAttribute.DATA_PUBLIC_KEY.getValue(), "public key");
		xOCCIAttributes.put(OrderAttribute.TYPE.getValue(), OrderType.PERSISTENT.getValue());
		xOCCIAttributes.put(OrderAttribute.PREVIOUS_ELAPSED_TIME.getValue(), "0");
		xOCCIAttributes.put(OrderAttribute.CURRENT_ELAPSED_TIME.getValue(), "0");
		xOCCIAttributes.put(OrderAttribute.RUNTIME.getValue(), "1000");
	}
	
	@Test
	public void testAddCategoryTwice() {
		Order order = new OrderXP("id", null, new LinkedList<Category>(),
				xOCCIAttributes, true, null);
		order.addCategory(new Category(OrderConstants.USER_DATA_TERM,
				OrderConstants.SCHEME, OrderConstants.MIXIN_CLASS));
		Assert.assertEquals(1, order.getCategories().size());
		order.addCategory(new Category(OrderConstants.USER_DATA_TERM,
				OrderConstants.SCHEME, OrderConstants.MIXIN_CLASS));
		Assert.assertEquals(1, order.getCategories().size());		
		
	}	
	
	@Test
	public void testOrderContructorSetResourceKind() {
		Order order = new Order("id", null, null, xOCCIAttributes, true, "requestingMemberId");		
		Assert.assertEquals(OrderConstants.COMPUTE_TERM, order.getResourceKind());
		
		xOCCIAttributes.put(OrderAttribute.RESOURCE_KIND.getValue(), OrderConstants.STORAGE_TERM);
		order = new OrderXP("id", null, null, xOCCIAttributes, true, "requestingMemberId");		
		Assert.assertEquals(OrderConstants.STORAGE_TERM, order.getResourceKind());
	}

}

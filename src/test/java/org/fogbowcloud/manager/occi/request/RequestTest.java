package org.fogbowcloud.manager.occi.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.manager.occi.model.Category;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;


public class RequestTest {

	@Test
	public void testAddCategoryTwice() {
		Request request = new Request("id", null, new LinkedList<Category>(),
				new HashMap<String, String>(), true, null);
		request.addCategory(new Category(RequestConstants.USER_DATA_TERM,
				RequestConstants.SCHEME, RequestConstants.MIXIN_CLASS));
		Assert.assertEquals(1, request.getCategories().size());
		request.addCategory(new Category(RequestConstants.USER_DATA_TERM,
				RequestConstants.SCHEME, RequestConstants.MIXIN_CLASS));
		Assert.assertEquals(1, request.getCategories().size());
	}
	
	@Test
	public void testFromCategoriesJSON() throws JSONException {
		List<Category> categories = new ArrayList<Category>();
		categories.add(new Category("term", "scheme", "catClass"));
		categories.add(new Category("termTwo", "schemeTwo", "catClassTwo"));
		Request request = new Request(null, null, null, null, null, 0, 
				false, null, categories, null);
		
		JSONArray categoriesJSON = request.getCategoriesInJSONFormat();
		List<Category> categoriesFromJSON = Request.getCategoriesFromJSON(categoriesJSON.toString());
		
		Assert.assertEquals(categories, categoriesFromJSON);
	}
	
	@Test
	public void testFromCategoriesJSONNullValue() throws JSONException {
		Request request = new Request(null, null, null, null, null, 0, 
				false, null, null, null);
		
		JSONArray categoriesJSON = request.getCategoriesInJSONFormat();
		List<Category> categoriesFromJSON = Request.getCategoriesFromJSON(categoriesJSON.toString());
		
		Assert.assertTrue(categoriesFromJSON.isEmpty());
	}	
	
	@Test
	public void testGetXOCCIAttrToJSON() throws JSONException {
		Map<String, String> xOCCIAttributes = new HashMap<String, String>();
		xOCCIAttributes.put("keyOne", "valueOne");
		xOCCIAttributes.put("keyTwo", "valueTwo");
		Request request = new Request(null, null, null, null, null, 0, 
				false, null, null, xOCCIAttributes);
		
		JSONObject xocciAttrToJSON = request.getXOCCIAttrInJSONFormat();
		Map<String, String> fromXOCCIAttrJSON = Request.getXOCCIAttrFromJSON(xocciAttrToJSON.toString());
		
		Assert.assertEquals(xOCCIAttributes, fromXOCCIAttrJSON);
	}
	
	@Test
	public void testGetXOCCIAttrToJSONNUllValue() throws JSONException {
		Request request = new Request(null, null, null, null, null, 0, 
				false, null, null, null);
		
		JSONObject xocciAttrToJSON = request.getXOCCIAttrInJSONFormat();
		Map<String, String> fromXOCCIAttrJSON = Request.getXOCCIAttrFromJSON(xocciAttrToJSON.toString());
		
		Assert.assertTrue(fromXOCCIAttrJSON.isEmpty());
	}
}

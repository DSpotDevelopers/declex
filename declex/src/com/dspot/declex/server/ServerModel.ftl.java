<#--

    Copyright (C) 2016 DSpot Sp. z o.o

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed To in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.

-->
<@class_head>
package com.dspot.declex.localdb;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.activeandroid.Model;
import com.dspot.declex.api.server.ServerJsonParseException;
import com.dspot.declex.api.server.ServerResponseException;
import com.dspot.declex.api.util.CastUtility;
import com.google.gson.FieldAttributes;
import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class User extends Model {
</@class_head>
	<#if client=="">
	<#assign client="okHttpClient">
	<@class_fields>
	private final static OkHttpClient okHttpClient = new OkHttpClient();
	</@class_fields>	
	</#if>

	//============================================================
	//						@ServerModel
	//============================================================

	<#if !offline>
	private static String requestToServer(String query, String orderBy, String fields, ${className} inst) {
		
		Matcher matcher = Pattern.compile("@(\\w+)\\(([^)]+)\\)").matcher(query);
		while (matcher.find()) {
			if (matcher.group(1).equals("server"))
				query = query.replace(matcher.group(0), matcher.group(2));
			else
				query = query.replace(matcher.group(0), "");
		}
		
		matcher = Pattern.compile("@(\\w+)\\(([^)]+)\\)").matcher(orderBy);
		if (matcher.find()) {
			if (matcher.group(1).equals("server"))
				orderBy = orderBy.replace(matcher.group(0), matcher.group(2));
			else
				orderBy = orderBy.replace(matcher.group(0), "");
		} 
		
		Response response = null;
		Request request = null;
		String json<#if hasMock>, mock</#if>;
		
		try {
			<#if hasMock>
			mock = getMock(query, orderBy, fields, inst);
			if (mock == null) {
			</#if>
				request = getRequest(query, orderBy, fields, inst);
				if (request == null) 
					if (orderBy.equals("")) return "";
					else return null;

	            response = ${client}.newCall(request).execute();
	            
			    <#if response!="">
		    	${response}(
	    			<#list responseParams as param>
	    			<#if param=="e">
	    			null<#if param_has_next>,</#if>
	    			<#else>
	    			${param}<#if param_has_next>,</#if>
	    			</#if>
	    			</#list>
				);
	            </#if>
	            <#if !processUnsuccessful>
	            
			    if (!response.isSuccessful()) {
			    	throw new ServerResponseException(response);
			    }
			    </#if>
			
				json = response.body().string();
			<#if hasMock>
			} else {
				json = mock;
			}
			</#if>
					
			return processResponse(json, query, orderBy, fields, inst);			
		
		} catch (java.io.IOException e) {
			
			<#if response!="">
	    	${response}(
    			<#list responseParams as param>
    			${param}<#if param_has_next>,</#if>
    			</#list>
			);
            </#if>
			throw new RuntimeException(e);
			
		} catch (JsonParseException e) {
			
			<#if response!="">
	    	${response}(
    			<#list responseParams as param>
    			${param}<#if param_has_next>,</#if>
    			</#list>
			);
            </#if>
			throw new ServerJsonParseException(response);
			
		}
		
	}
	
	private static ${className} getServerModel(Context context, String query, String orderBy, String fields) {
		String json = requestToServer(query, orderBy, fields, null);
		if (json == null) return null;
		
		try {
		
			JsonElement elem = new JsonParser().parse(json);
			if (elem.isJsonArray()) {
				if (elem.getAsJsonArray().size() == 0) return null;
				elem = elem.getAsJsonArray().get(0);
			}
			
			if (!elem.isJsonObject()) return null;
	        
			${className} instance = getGson().fromJson(elem, ${className}.class);
	        if (instance!= null) {
	            instance.rebind(context);
	        }
			
	        return instance;
	        
		} catch (JsonParseException e) {
			throw new ServerJsonParseException(json, e);
		}
        	        
	}
	
	private ${className} putServerModel(String query, String orderBy, String fields) {
		if (query.equals("server-ignore")) return this;
		
		String json = requestToServer(query, orderBy, fields, this);
		if (json == null) return null;
		
		return this;
	}
	
	private static java.util.List<${className}> getServerModels(Context context, String query, String orderBy, String fields) {
		String json = requestToServer(query, orderBy, fields, null);
		if (json == null) return new ArrayList<${className}>();
		
		try {
			
			JsonElement elem = new JsonParser().parse(json);
			if (elem.isJsonObject()) {
				java.util.List<${className}> models = new ArrayList<${className}>();
				
				${className} instance = getGson().fromJson(elem, ${className}.class);
		        if (instance!= null) {
		            instance.rebind(context);
		        }
		        
				models.add(instance);
				return models;
			}
			
			if (!elem.isJsonArray()) return new ArrayList<${className}>();
	        
			Type listType = new TypeToken<java.util.List<${className}>>(){}.getType();
			java.util.List<${className}> models = getGson().fromJson(elem, listType);
			
			//Rebind each model to the current context
			for (${className} model : models) {
	            model.rebind(context);
	        }
			
	        return models;
	        
		} catch (JsonParseException e) {
			throw new ServerJsonParseException(json, e);
		}
	}

	private java.util.Map<String, String> getAllFields(String fields) {
        java.util.Map<String, String> allFields = new java.util.HashMap<>();
		
		try {
			JsonElement elem = getGson(this, fields).toJsonTree(this);
			for (Entry<String, JsonElement> entry : elem.getAsJsonObject().entrySet()) {
				String value = entry.getValue().toString();
				if (value.startsWith("\"")) value = value.substring(1, value.length()-1);
				allFields.put(entry.getKey(), value);
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		return allFields;
	}
	
	</#if>
<@class_footer>	
}
</@class_footer>
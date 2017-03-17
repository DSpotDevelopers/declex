<#--

    Copyright (C) 2016-2017 DSpot Sp. z o.o

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

import com.google.gson.FieldAttributes;
import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class User extends Model {
</@class_head>

	//============================================================
	//						   @JsonModel
	//============================================================

	public String toJson() {
		return this.toJson(null);
	}
	
	public String toJson(String fields) {
		return getGson(this, fields).toJson(this);
	}
	
	public static ${className} fromJson(String json) {
		return getGson().fromJson(json, ${className}.class);
	}
	
	public static ${className} fromJson(JsonElement jsonElement) {
		return getGson().fromJson(jsonElement, ${className}.class);
	}

	public static List<${className}> listFromJson(String json) {
		Type listType = new TypeToken<java.util.List<${className}>>(){}.getType();
		return getGson().fromJson(json, listType);
	}
	
	public static List<${className}> listFromJson(JsonElement jsonElement) {
		Type listType = new TypeToken<java.util.List<${className}>>(){}.getType();
		return getGson().fromJson(jsonElement, listType);
	}
	
	private static Gson getGson() {
		return getGson(null, null);
	}
	
	private static Gson getGson(${className} inst, String fields) {
		return getGsonBuilder(inst, fields).create();		
	}
		
	public static class ModelExclusionStrategy implements ExclusionStrategy {

		private ${className} inst;
		private List<String> fields;		
		<#if (jsonSerializedModels?size > 0)>

		private ExclusionStrategy currentExclusion;
		
		<#list jsonSerializedModels as fieldName, classInfo>
		private ${classInfo.generatorClassName}_.ModelExclusionStrategy exclussionFor_${fieldName};
		</#list>
		</#if>
		
		private static ModelExclusionStrategy nullExclussionInstance;
		
		public static ModelExclusionStrategy nullExclussion() {
			if (nullExclussionInstance == null) {
				nullExclussionInstance = new ModelExclusionStrategy(null, null);
			}			
			return nullExclussionInstance;
		}
		<#if (jsonSerializedModels?size > 0)>
		
		private void initializeSerializedModels(${className} inst) {			
			<#list jsonSerializedModels as fieldName, classInfo>
			<#if !(classInfo.list)>

			exclussionFor_${fieldName} = new ${classInfo.generatorClassName}_.ModelExclusionStrategy(inst.${fieldName}, null);
			</#if>
			</#list>
			
		}		
		</#if>
		
		public void clearExclusion() {
			<#if (jsonSerializedModels?size > 0)>
			currentExclusion = null;
			</#if>
		}
		
		public ModelExclusionStrategy(${className} inst, String fields) {
			this.inst = inst;
			
			if (fields == null || fields.trim().equals("")) {
				this.fields = null;
			} else {
				this.fields = Arrays.asList(fields.split("\\s*[,]\\s*"));				
			}		
			<#if (jsonSerializedModels?size > 0)>
			
			if (inst != null) {
				initializeSerializedModels(inst);
			}
			</#if>
		}
		
		@Override
		public boolean shouldSkipClass(Class<?> arg0) {
			return false;
		}

		@Override
		public boolean shouldSkipField(FieldAttributes field) {
			
			if (fields != null && field.getDeclaringClass().getName().equals("${fromClassFull}")) {
				if (!fields.contains(field.getName())) return true;
			}
			<#if (jsonSerializedModels?size > 0)>
			
			if (currentExclusion != null) {
				if (currentExclusion.shouldSkipField(field)) return true;
			}
			
			if (field.getDeclaringClass().getName().equals("${fromClassFull}")) {
				<#list jsonSerializedModels as fieldName, classInfo>
				
				if (field.getName().equals("${fieldName}")) {
					if (exclussionFor_${fieldName} == null) 
						exclussionFor_${fieldName} = ${classInfo.generatorClassName}_.ModelExclusionStrategy.nullExclussion();
					
					currentExclusion = exclussionFor_${fieldName};
					exclussionFor_${fieldName}.clearExclusion();
				} else if (currentExclusion == exclussionFor_${fieldName}) {
					currentExclusion = null;
				}			
				</#list>
			}
			
			</#if>
			<#if (serializeConditions?size > 0)>
			
			<#list serializeConditions as field, cond>
			if ("${field}".equals(field.getName())<#if cond!="!(false)"> && (${cond})</#if>) return true;
			</#list>
            </#if>
			
            //Exclude from serialization any generated class fields (ending with "_") and 
			//Active Android Model fields, if any
			return field.getDeclaringClass().getName().endsWith("_") ||
					field.getDeclaringClass().getCanonicalName().equals(com.activeandroid.Model.class.getCanonicalName()) ||
					field.hasModifier(java.lang.reflect.Modifier.FINAL) || field.hasModifier(java.lang.reflect.Modifier.STATIC) ||
					field.hasModifier(java.lang.reflect.Modifier.ABSTRACT) || field.hasModifier(java.lang.reflect.Modifier.TRANSIENT);
		}
		
	}	
<@class_footer>	
}
</@class_footer>
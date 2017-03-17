/**
 * Copyright (C) 2016-2017 DSpot Sp. z o.o
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dspot.declex.api.action.process;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ActionInfo {
	public Map<String, Object> metaData;
	
	public List<ActionProcessor> processors = new LinkedList<>();
	public Map<String, List<ActionMethod>> methods = new HashMap<>();
	
	public String holderClass;
	public String references;
	public boolean isGlobal;
	
	public boolean generated;
	
	public ActionInfo(String holderClass) {
		this.holderClass = holderClass;
		this.generated = true;
	}
	
	public void clearMetaData() {
		this.metaData = new HashMap<>();
		
		for (Entry<String, List<ActionMethod>> methodList : methods.entrySet()) {
			for (ActionMethod method : methodList.getValue()) {
				method.metaData = null;
				
				for (ActionMethodParam param : method.params) {
					param.metaData = null;
				}
				
			}
		}
	}
	
	public void setReferences(String references) {
		this.references = references;
	}
	
	public void addMethod(String name, String resultClass) {
		addMethod(name, resultClass, new ArrayList<ActionMethodParam>(0));
	}
	
	public void addMethod(String name, String resultClass, List<ActionMethodParam> params) {
		addMethod(name, resultClass, params, new ArrayList<Annotation>(0));
	}

	public void addMethod(String name, String resultClass, List<ActionMethodParam> params, List<Annotation> annotations) {
		addMethod(name, resultClass, null, params, annotations);
	}
	
	public void addMethod(String name, String resultClass, String javaDoc, List<ActionMethodParam> params, List<Annotation> annotations) {
		ActionMethod actionMethod = new ActionMethod(name, resultClass, javaDoc, params, annotations);
		
		List<ActionMethod> methodList = methods.get(name);
		if (methodList == null) {
			methodList = new LinkedList<>();
			methods.put(name, methodList);
		}
		
		methodList.add(actionMethod);
	}

	public void validateProcessors() {
		for (ActionProcessor processor : processors) {
			processor.validate(this);
		}
	}
	
	public void callProcessors() {
		for (ActionProcessor processor : processors) {
			processor.process(this);
		}
	}
}

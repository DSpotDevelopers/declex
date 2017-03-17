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
import java.util.List;
import java.util.Map;

public class ActionMethod {
	public Map<String, Object> metaData;
	
	public String name;
	public List<ActionMethodParam> params;
	public List<Annotation> annotations; 
	
	public String javaDoc;
	
	public String resultClass;
	
	ActionMethod(String name, String resultClass, String javaDoc, List<ActionMethodParam> params, List<Annotation> annotations) {
		super();
		
		if (name == null) {
			throw new IllegalArgumentException("\"name\" cannot be null");
		}
		
		if (resultClass == null) {
			throw new IllegalArgumentException("\"resultClass\" cannot be null");
		}
		
		this.name = name;
		this.params = params;
		this.annotations = annotations;
		this.resultClass = resultClass;
		this.javaDoc = javaDoc;
	}
}

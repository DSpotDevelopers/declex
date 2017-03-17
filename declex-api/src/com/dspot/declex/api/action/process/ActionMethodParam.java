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
import java.util.List;
import java.util.Map;

import com.helger.jcodemodel.AbstractJType;

public class ActionMethodParam {
	public Map<String, Object> metaData;
	public Object internal;
	
	public String name;
	public AbstractJType clazz;
	public List<Annotation> annotations;
	
	public ActionMethodParam(String name, AbstractJType clazz) {
		this(name, clazz, new ArrayList<Annotation>(0));
	}
	
	public ActionMethodParam(String name, AbstractJType clazz, List<Annotation> annotations) {
		super();
		this.name = name;
		this.clazz = clazz;
		this.annotations = annotations;
	}
}

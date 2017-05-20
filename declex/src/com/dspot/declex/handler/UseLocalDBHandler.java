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
package com.dspot.declex.handler;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.EApplication;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.BaseGeneratedClassHolder;
import org.androidannotations.holder.EApplicationHolder;
import org.androidannotations.holder.EComponentHolder;

import com.dspot.declex.annotation.LocalDBModel;
import com.dspot.declex.annotation.UseLocalDB;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JInvocation;

public class UseLocalDBHandler extends BaseAnnotationHandler<BaseGeneratedClassHolder> {

	public UseLocalDBHandler(AndroidAnnotationsEnvironment environment) {
		super(UseLocalDB.class, environment);
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Class<? extends Annotation>> dependencies) {
		dependencies.put(element, EApplication.class);
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {
	}

	@Override
	public void process(Element element, BaseGeneratedClassHolder holder)
			throws Exception {
		
		if (holder instanceof EApplicationHolder) {
			AbstractJClass ActiveAndroid = getJClass("com.activeandroid.ActiveAndroid");
			AbstractJClass ConfigurationBuilder = getJClass("com.activeandroid.Configuration.Builder");
			
			IJExpression configuration = JExpr._new(ConfigurationBuilder)
          		  							  .arg(((EApplicationHolder) holder).getContextRef());

			Collection<String> models = SharedRecords.getDBModelGeneratedClasses(getEnvironment());
			for (String model : models) {
				
				LocalDBModel localDBModel = TypeUtils.getClassAnnotation(model, LocalDBModel.class, getEnvironment()); 
				if ( localDBModel != null && localDBModel.hasTable()) {
					
					String modelClass = TypeUtils.getGeneratedClassName(model, getEnvironment());
					configuration = configuration.invoke("addModelClass")
							           .arg(getJClass(modelClass).dotclass());
					
				}
				
			}
			
			JInvocation invokeInitialize = ActiveAndroid.staticInvoke("initialize")
                            .arg(configuration.invoke("create"));
			
			((EComponentHolder) holder).getInitBody().add(invokeInitialize);
		}
	}

}

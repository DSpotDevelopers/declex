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

import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.EBean;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EBeanHolder;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;

import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.UseModel;
import org.androidannotations.helper.FilesCacheHelper;
import com.dspot.declex.holder.UseModelHolder;
import com.helger.jcodemodel.JMethod;

public class UseModelHandler extends BaseAnnotationHandler<EComponentHolder> {

	protected static final Logger LOGGER = LoggerFactory.getLogger(UseModelHandler.class);
	
	public UseModelHandler(AndroidAnnotationsEnvironment environment) {
		super(UseModel.class, environment);
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Object> dependencies) {
		if (element.getKind().equals(ElementKind.CLASS)) {
			dependencies.put(element, EBean.class);
		} else {
			dependencies.put(element, Model.class);
		}
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {
	}
	
	@Override
	public void process(Element element, EComponentHolder holder) {
		
		if (element.getKind().isField()) return;
		
		UseModelHolder useModelHolder = holder.getPluginHolder(new UseModelHolder(holder));
		useModelHolder.getConstructorMethod();
	
		useModelHolder.getWriteObjectMethod();
		useModelHolder.getReadObjectMethod();
	
		useModelHolder.getExistsVar();
		useModelHolder.getFields();
		
		//This avoids cross references if Cache Files is enabled
		if (FilesCacheHelper.isCacheFilesEnabled()) {
			useModelHolder.getGetModelListMethod();
			useModelHolder.getLoadModelMethod();
			useModelHolder.getModelInitMethod();
		}
		
		//Get Instance will invoke getModel_
		JMethod factoryMethod = ((EBeanHolder)holder).getFactoryMethod();
		codeModelHelper.removeBody(factoryMethod);
		factoryMethod.body()._return(
			invoke(useModelHolder.getGetModelMethod()).arg(ref("context")).arg(_null()).arg(_null())
		);
		
	}
	
}

/**
 * Copyright (C) 2016 DSpot Sp. z o.o
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
package com.dspot.declex.localdb;

import java.util.Collection;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.EApplication;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.BaseGeneratedClassHolder;
import org.androidannotations.holder.EApplicationHolder;
import org.androidannotations.holder.EComponentHolder;

import com.dspot.declex.api.localdb.LocalDBModel;
import com.dspot.declex.api.localdb.UseLocalDB;
import com.dspot.declex.util.SharedRecords;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JInvocation;

public class UseLocalDBHandler extends BaseAnnotationHandler<BaseGeneratedClassHolder> {

	public UseLocalDBHandler(AndroidAnnotationsEnvironment environment) {
		super(UseLocalDB.class, environment);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		validatorHelper.typeHasAnnotation(EApplication.class, element, valid);		
	}

	@Override
	public void process(Element element, BaseGeneratedClassHolder holder)
			throws Exception {
		
		if (holder instanceof EApplicationHolder) {
			AbstractJClass ActiveAndroid = getJClass("com.activeandroid.ActiveAndroid");
			AbstractJClass ConfigurationBuilder = getJClass("com.activeandroid.Configuration.Builder");
			
			IJExpression configuration = JExpr._new(ConfigurationBuilder)
          		  							  .arg(((EApplicationHolder) holder).getContextRef());

			Collection<String> models = SharedRecords.getModelGeneratedClasses(getEnvironment());
			for (String model : models) {
				TypeElement elem = getProcessingEnvironment().getElementUtils().getTypeElement(model);
				LocalDBModel localDBModel = elem.getAnnotation(LocalDBModel.class); 
				if ( localDBModel != null && localDBModel.hasTable()) {
					configuration = configuration.invoke("addModelClass")
							           .arg(getJClass(model + ModelConstants.generationSuffix()).dotclass());
				}
			}
			
			JInvocation invokeInitialize = ActiveAndroid.staticInvoke("initialize")
                            .arg(configuration.invoke("create"));
			
			((EComponentHolder) holder).getInitBody().add(invokeInitialize);
		}
	}

}

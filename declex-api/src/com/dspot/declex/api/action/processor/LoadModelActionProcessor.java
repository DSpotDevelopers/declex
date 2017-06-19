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
package com.dspot.declex.api.action.processor;

import static com.helger.jcodemodel.JExpr.invoke;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EFragment;

import com.dspot.declex.annotation.External;
import com.dspot.declex.annotation.ExternalPopulate;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.Populate;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.util.FormatsUtils;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;

public class LoadModelActionProcessor extends BaseActionProcessor {

	@Override
	public void validate(ActionInfo actionInfo) {
		super.validate(actionInfo);
		
		ActionMethod init = getActionMethod("init");
				
		if (init.metaData != null) {
			ActionMethodParam initParam = init.params.get(0);
			Element field = (Element) initParam.metaData.get("field");
			
			if (field != null) {
				Model modelAnnotation = getAnnotation(field, Model.class);
				if (modelAnnotation == null) {
					throw new IllegalStateException("The field " + field + " is not annotated with @Model");
				}
				
				actionInfo.isTimeConsuming = modelAnnotation.async();
				
				ActionMethod noPopulate = getActionMethod("noPopulate");
				if (noPopulate.metaData != null) {
					Populate populatorAnnotation = getAnnotation(field, Populate.class);
					ExternalPopulate externalPopulatorAnnotation = getAnnotation(field, ExternalPopulate.class);
					if (populatorAnnotation == null && externalPopulatorAnnotation == null) {
						throw new IllegalStateException("The field " + field + " is not annotated with @Populate");
					}
				}
			}
		}
	}
	
	@Override
	public void process(ActionInfo actionInfo) {
		super.process(actionInfo);
		
		ActionMethod init = getActionMethod("init");
		ActionMethod query = getActionMethod("query");
		ActionMethod orderBy = getActionMethod("orderBy");
		ActionMethod fields = getActionMethod("fields");
				
		if (init.metaData != null) {
			ActionMethodParam initParam = init.params.get(0);
			Element field = (Element) initParam.metaData.get("field");
			
			if (field != null) {
				
				Model modelAnnotation = getAnnotation(field, Model.class);
					
				if (field.getEnclosingElement().getAnnotation(EFragment.class) == null
					&& field.getEnclosingElement().getAnnotation(EActivity.class) == null
					&& getAnnotation(getElement(), External.class) == null
					&& getAnnotation(getAnnotatedElement(), External.class) == null)
				{
					if (getActionMethod("keepCallingThread").metaData == null) {
						addPreBuildBlock(getAction().invoke("keepCallingThread"));
					}
				}
				
				actionInfo.isTimeConsuming = modelAnnotation.async();
				
				JMethod getModelMethod = getMethodInHolder(
						"getLoadModelMethod", "com.dspot.declex.holder.ModelHolder", field
					);
				
				if (query.metaData == null) {
					IJExpression queryExp = FormatsUtils.expressionFromString(modelAnnotation.query());
					addPostInitBlock(getAction().invoke("query").arg(queryExp));
				} 
				
				if (orderBy.metaData == null) {
					IJExpression orderByExp = FormatsUtils.expressionFromString(modelAnnotation.orderBy());
					addPostInitBlock(getAction().invoke("orderBy").arg(orderByExp));
				}

				if (fields.metaData == null) {
					IJExpression fieldsExp = FormatsUtils.expressionFromString(modelAnnotation.fields());
					addPostInitBlock(getAction().invoke("fields").arg(fieldsExp));
				}
				
				JInvocation invoke = invoke(getModelMethod);
				if (field.getModifiers().contains(Modifier.STATIC)) {
					IJExpression context = (IJExpression)getMethodInHolder("getContextRef");
					invoke.arg(context);
				}
				invoke = invoke.arg(getAction().invoke("getArgs"))
						       .arg(getAction().invoke("getDone"))
						       .arg(getAction().invoke("getFailed"));
				
				addPostBuildBlock(invoke);					
			}
		}
	}

}

/**
 * Copyright (C) 2016-2018 DSpot Sp. z o.o
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
package com.dspot.actions.processors;

import com.dspot.declex.annotation.ExportRecollect;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.Recollect;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.action.processor.BaseActionProcessor;
import com.dspot.declex.api.util.FormatsUtils;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.export.Export;

import javax.lang.model.element.Element;

import static com.helger.jcodemodel.JExpr.invoke;

public class PutModelActionProcessor extends BaseActionProcessor {

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
				
				actionInfo.isTimeConsuming = modelAnnotation.asyncPut();
				
				ActionMethod noRecollecte = getActionMethod("noRecollect");
				if (noRecollecte.metaData != null) {
					Recollect recollectorAnnotation = getAnnotation(field, Recollect.class);
					ExportRecollect externalRecollectorAnnotation = getAnnotation(field, ExportRecollect.class);
					if (recollectorAnnotation == null && externalRecollectorAnnotation == null) {
						throw new IllegalStateException("The field " + field + " is not annotated with @Recollect");
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
					&& getAnnotation(getElement(), Export.class) == null
					&& getAnnotation(getAnnotatedElement(), Export.class) == null)
				{
					if (getActionMethod("keepCallingThread").metaData == null) {
						addPreBuildBlock(getAction().invoke("keepCallingThread"));
					}
				}
				
				actionInfo.isTimeConsuming = modelAnnotation.async();
				
				JMethod putModelMethod = getMethodInHolder(
						"getPutModelMethod", "com.dspot.declex.holder.ModelHolder", field
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
				
				JInvocation invoke = invoke(putModelMethod)
										.arg(getAction().invoke("getArgs"))
						                .arg(getAction().invoke("getDone"))
						                .arg(getAction().invoke("getFailed"));
				
				addPostBuildBlock(invoke);				
			}
		}
	}

}

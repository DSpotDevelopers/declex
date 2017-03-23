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

import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.assign;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;

import javax.lang.model.element.Element;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EFragment;
import org.apache.commons.lang3.StringUtils;

import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.model.Model;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.api.viewsinjection.Populate;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JDefinedClass;
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
				Model modelAnnotation = field.getAnnotation(Model.class);
				if (modelAnnotation == null) {
					throw new IllegalStateException("The field " + field + " is not annotated with @Model");
				}
				
				actionInfo.isTimeConsuming = modelAnnotation.async();
				
				ActionMethod noPopulate = getActionMethod("noPopulate");
				if (noPopulate.metaData != null) {
					Populate populatorAnnotation = field.getAnnotation(Populate.class);
					if (populatorAnnotation == null) {
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
			String fieldName = (String) initParam.metaData.get("fieldName");
			
			if (field != null) {
				Model modelAnnotation = field.getAnnotation(Model.class);
					
				if (field.getEnclosingElement().getAnnotation(EFragment.class) == null
						&& field.getEnclosingElement().getAnnotation(EActivity.class) == null)
				{
					if (getActionMethod("keepCallingThread").metaData == null) {
						addPreBuildBlock(getAction().invoke("keepCallingThread"));
					}
				}
				
				JMethod getModelMethod = getMethodInHolder(
						"getLoadModelMethod", "com.dspot.declex.model.ModelHolder", field
					);
				
				IJExpression queryExp = null;
				if (query.metaData == null) {
					queryExp = FormatsUtils.expressionFromString(modelAnnotation.query());
				} else {
					queryExp = getAction().invoke("getQuery");
				}
				
				IJExpression orderByExp = null;
				if (orderBy.metaData == null) {
					orderByExp = FormatsUtils.expressionFromString(modelAnnotation.orderBy());
				} else {
					orderByExp = getAction().invoke("getOrderBy");
				}
				
				IJExpression fieldsExp = null;
				if (fields.metaData == null) {
					fieldsExp = FormatsUtils.expressionFromString(StringUtils.join(modelAnnotation.fields(), ", "));
				} else {
					fieldsExp = getAction().invoke("getFields");
				}
				
				IJExpression contextExpr = getMethodInHolder("getContextRef");
				if (contextExpr == _this()) {
					JDefinedClass generatedClass = getMethodInHolder("getGeneratedClass");
					contextExpr = generatedClass.staticRef("this");
				}
				
				JInvocation invoke = invoke(getModelMethod)
										.arg(contextExpr)
										.arg(queryExp).arg(orderByExp).arg(fieldsExp)
						                .arg(getAction().invoke("getAfterLoad"))
						                .arg(getAction().invoke("getFailed"));
				
				ActionMethod noPopulate = getActionMethod("noPopulate");
				if (noPopulate.metaData != null) {
					addPostBuildBlock(assign(ref("_populate_" + fieldName), lit(false)));
				}
				
				addPostBuildBlock(invoke);					
			}
		}
	}

}

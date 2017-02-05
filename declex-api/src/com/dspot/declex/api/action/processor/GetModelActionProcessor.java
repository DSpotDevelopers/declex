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
package com.dspot.declex.api.action.processor;

import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.invoke;

import javax.lang.model.element.Element;

import org.apache.commons.lang3.StringUtils;

import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.model.Model;
import com.dspot.declex.api.util.FormatsUtils;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JVar;

public class GetModelActionProcessor extends BaseActionProcessor {

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
			Object holder = actionInfo.metaData.get("holder");
			JVar action = (JVar) actionInfo.metaData.get("action");
			
			if (field != null && holder != null && action != null) {
				Model modelAnnotation = field.getAnnotation(Model.class);
				if (modelAnnotation != null) {
					
					Boolean validating = (Boolean) actionInfo.metaData.get("validating");
					if (validating) return;
					
					JMethod getModelMethod = ActionProcessorUtil.getMethodInHolder(
							"getGetModelMethod", holder, "com.dspot.declex.model.ModelHolder", field
						);
					
					IJExpression queryExp = null;
					if (query.metaData == null) {
						queryExp = FormatsUtils.expressionFromString(modelAnnotation.query());
					} else {
						queryExp = action.invoke("getQuery");
					}
					
					IJExpression orderByExp = null;
					if (orderBy.metaData == null) {
						orderByExp = FormatsUtils.expressionFromString(modelAnnotation.orderBy());
					} else {
						orderByExp = action.invoke("getOrderBy");
					}
					
					IJExpression fieldsExp = null;
					if (fields.metaData == null) {
						fieldsExp = FormatsUtils.expressionFromString(StringUtils.join(modelAnnotation.fields(), ", "));
					} else {
						fieldsExp = action.invoke("getFields");
					}
					
					IJExpression contextExpr = ActionProcessorUtil.getMethodInHolder("getContextRef", holder);
					if (contextExpr == _this()) {
						JDefinedClass generatedClass = ActionProcessorUtil.getMethodInHolder("getGeneratedClass", holder);
						contextExpr = generatedClass.staticRef("this");
					}
					
					JInvocation invoke = invoke(getModelMethod)
											.arg(contextExpr)
											.arg(queryExp).arg(orderByExp).arg(fieldsExp)
							                .arg(action.invoke("getAfterLoad"))
							                .arg(action.invoke("getFailed"));
					
					addPostBuildBlock(invoke);
					
				} else {
					throw new IllegalStateException("The field " + field + " is not annotated with @Model");
				}
			}
		}
	}

}

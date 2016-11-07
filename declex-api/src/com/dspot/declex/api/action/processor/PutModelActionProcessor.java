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

import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.invoke;

import javax.lang.model.element.Element;

import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.model.Model;
import com.dspot.declex.api.util.FormatsUtils;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JVar;

public class PutModelActionProcessor extends BaseActionProcessor {

	@Override
	public void process(ActionInfo actionInfo) {
		super.process(actionInfo);
		
		ActionMethod init = getActionMethod("init");
		ActionMethod query = getActionMethod("query");
		ActionMethod orderBy = getActionMethod("orderBy");
		
		IJExpression queryExp = null;
		if (query.metaData != null) {
			queryExp = direct((String) query.params.get(0).metaData.get("value"));
		}
		
		IJExpression orderByExp = null;
		if (orderBy.metaData != null) {
			orderByExp = direct((String) orderBy.params.get(0).metaData.get("value"));
		}
				
		if (init.metaData != null) {
			ActionMethodParam initParam = init.params.get(0);
			Element field = (Element) initParam.metaData.get("field");
			Object holder = actionInfo.metaData.get("holder");
			JVar action = (JVar) actionInfo.metaData.get("action");
			
			if (field != null && holder != null && action != null) {
				Model modelAnnotation = field.getAnnotation(Model.class);
				if (modelAnnotation != null) {
					
					JMethod putModelMethod = ActionProcessorUtil.getMethodInHolder(
							"getPutModelMethod", holder, "com.dspot.declex.model.ModelHolder", field
						);
					
					if (queryExp==null) 
						queryExp = FormatsUtils.expressionFromString(modelAnnotation.query());
					
					if (orderByExp==null) 
						orderByExp = FormatsUtils.expressionFromString(modelAnnotation.orderBy());

					JInvocation invoke = invoke(putModelMethod)
											.arg(queryExp).arg(orderByExp)
							                .arg(action.invoke("getAfterPut"));
					
					addPostBuildBlock(invoke);
					
				} else {
					throw new IllegalStateException("The field " + field + " is not annotated with @Model");
				}
			}
		}
	}

}

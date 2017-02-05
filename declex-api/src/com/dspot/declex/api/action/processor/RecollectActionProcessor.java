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

import static com.helger.jcodemodel.JExpr.invoke;

import javax.lang.model.element.Element;

import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.populator.Recollector;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JVar;

public class RecollectActionProcessor extends BaseActionProcessor {

	@Override
	public void process(ActionInfo actionInfo) {
		super.process(actionInfo);
		
		ActionMethod init = getActionMethod("init");
				
		if (init.metaData != null) {
			ActionMethodParam initParam = init.params.get(0);
			Element field = (Element) initParam.metaData.get("field");
			JVar action = (JVar) actionInfo.metaData.get("action");
			
			if (field != null && action != null) {
				
				Recollector recollectorAnnotation = field.getAnnotation(Recollector.class);
				if (recollectorAnnotation != null) {
					
					Boolean validating = (Boolean) actionInfo.metaData.get("validating");
					if (validating) return;
					
					JInvocation invoke = invoke("_recollect_" + field.getSimpleName().toString())
											.arg(action.invoke("getDone"))
											.arg(action.invoke("getFailed"));
					addPostBuildBlock(invoke);
					
				} else {
					throw new IllegalStateException("The field " + field + " is not annotated with @Recollector");
				}
			}
		}
	}

}

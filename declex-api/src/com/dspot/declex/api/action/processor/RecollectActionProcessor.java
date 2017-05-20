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
import static com.helger.jcodemodel.JExpr.ref;

import javax.lang.model.element.Element;

import com.dspot.declex.annotation.Recollect;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JVar;

public class RecollectActionProcessor extends BaseActionProcessor {

	@Override
	public void validate(ActionInfo actionInfo) {
		super.validate(actionInfo);
		
		ActionMethod init = getActionMethod("init");
				
		if (init.metaData != null) {
			ActionMethodParam initParam = init.params.get(0);
			Element field = (Element) initParam.metaData.get("field");
			
			if (field != null) {
				Recollect recollectorAnnotation = field.getAnnotation(Recollect.class);
				if (recollectorAnnotation == null) {
					throw new IllegalStateException("The field " + field + " is not annotated with @Recollect");
				}
			}
		}
	}
	
	@Override
	public void process(ActionInfo actionInfo) {
		super.process(actionInfo);
		
		ActionMethod init = getActionMethod("init");
				
		if (init.metaData != null) {
			ActionMethodParam initParam = init.params.get(0);
			Element field = (Element) initParam.metaData.get("field");
			JVar action = (JVar) actionInfo.metaData.get("action");
			
			if (field != null && action != null) {		
				
				JInvocation invoke = invoke("_recollect_" + field.getSimpleName().toString())
										.arg(action.invoke("getDone"))
										.arg(action.invoke("getFailed"));
				addPostBuildBlock(invoke);
				
			} else { //Recollect "this" call
				
				if (getGeneratedClass().containsField("recollectThis")) {
					JFieldRef listenerField = ref("recollectThis");
					
					JBlock block = new JBlock();
					JConditional ifNeNull = block._if(listenerField.neNull());
					ifNeNull._then().invoke(listenerField, "recollectModel")
					           .arg(getAction().invoke("getDone"))
					           .arg(getAction().invoke("getFailed"));
					
					addPostBuildBlock(block);					
				} else {
					JInvocation invoke = invoke("_recollect_this")
							.arg(getAction().invoke("getDone"))
							.arg(getAction().invoke("getFailed"));
					addPostBuildBlock(invoke);	
				}
				
			}
		}
	}

}

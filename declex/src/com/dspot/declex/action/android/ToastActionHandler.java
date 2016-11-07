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
package com.dspot.declex.action.android;

import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.ref;

import javax.lang.model.element.Element;

import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.action.BaseSystemClassActionHandler;
import com.dspot.declex.api.util.FormatsUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJAssignmentTarget;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMod;

public class ToastActionHandler extends BaseSystemClassActionHandler {

	@Override
	protected IJStatement getStatement(AbstractJClass Toast, JFieldRef runnableRef, String[] parameters,
			Element element, EComponentWithViewSupportHolder holder) {
		
		IJExpression context = holder.getContextRef();
		if (context == _this()) {
			context = holder.getGeneratedClass().staticRef("this");
		}

		JInvocation invocation = Toast.staticInvoke("makeText").arg(context);
		IJAssignmentTarget assignToastTo = null;
		
		for (String value : parameters) {
			if (value.equals("")) continue;
			
			char lastChar = value.charAt(value.length()-1);
			if (lastChar == ';' || lastChar == '?' || lastChar == '!') {
				continue;
			}
						
			if (value.startsWith(":")) {
				final String fieldName = value.substring(1);
				
				assignToastTo = holder.getGeneratedClass().fields().get(fieldName);
				if (assignToastTo == null) {
					assignToastTo = holder.getGeneratedClass().field(JMod.PRIVATE, Toast, fieldName);
				}
				continue;
			}
			
			if (value.startsWith("=")) {
				assignToastTo = ref(value.substring(1));
				continue;
			}
			
			if (value.startsWith("R.string.")) invocation.arg(direct(value));
			else invocation.arg(FormatsUtils.expressionFromString(value));
		}

		invocation.arg(Toast.staticRef("LENGTH_SHORT"));
		
		JBlock block = new JBlock();
		
		if (assignToastTo != null) {
			block.assign(assignToastTo, invocation);
			block.invoke(assignToastTo, "show");
		} else {
			block.add(invocation.invoke("show"));
		}
		
		if (runnableRef != null) {
			block.invoke(runnableRef, "run");
		}
		
		return block;
	}

	@Override
	public String getClassName() {
		return "android.widget.Toast";
	}
	
}

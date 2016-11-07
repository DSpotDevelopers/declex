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

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;

import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.action.BaseSystemClassActionHandler;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class ProgressDialogActionHandler extends BaseSystemClassActionHandler {

	@Override
	protected IJStatement getStatement(AbstractJClass DialogClass, JFieldRef runnableRef, String[] parameters,
			Element element, EComponentWithViewSupportHolder holder) {
		
		IJExpression context = holder.getContextRef();
		if (context == _this()) {
			context = holder.getGeneratedClass().staticRef("this");
		}
		
		JBlock creationBlock = new JBlock();
		
		JVar progressDialog = creationBlock.decl(DialogClass, "progressDialogLocalReference", _new(DialogClass).arg(context));
		
		boolean calledNextRunnable = false;
		for (String value : parameters) {
			if (value.equals("")) continue;
			
			char lastChar = value.charAt(value.length()-1);
			if (lastChar == ';' || lastChar == '?' || lastChar == '!') {
				continue;
			}
			
			if (value.startsWith(":")) {
				final String fieldName = value.substring(1);
				
				JFieldVar assignTo = holder.getGeneratedClass().fields().get(fieldName);
				if (assignTo == null) {
					assignTo = holder.getGeneratedClass().field(JMod.PRIVATE, DialogClass, fieldName);
				}
				
				creationBlock.assign(assignTo, progressDialog);
				
				continue;
			}
			
			if (value.startsWith("=")) {
				creationBlock.assign(ref(value.substring(1)), progressDialog);
				continue;
			}
			
			Matcher matcher = Pattern.compile("\\A\\s*(\\w+)\\s*=").matcher(value);
			if (matcher.find()) {
				String method = matcher.group(1);
				if (!method.startsWith("set")) {
					method = "set" + method.substring(0, 1).toUpperCase() + method.substring(1);
				}
				
				JInvocation setAction = progressDialog.invoke(method);
				
				String[] params = value.substring(matcher.group(0).length()).split("\\s*,\\s*");
				for (int i = 0; i < params.length; i++) { 
					String param = params[i];
					
					JInvocation contextGetString = null;
					if (param.startsWith("R.string.")) {
						contextGetString = context.invoke("getString");
					}
					
					if (i == params.length-1) 
						switch (method) {
						case "setButton":
						case "setButton2":
						case "setButton3":
							AbstractJClass OnClickListener = environment.getJClass("android.content.DialogInterface.OnClickListener");
							JDefinedClass AnonymousOnClickListener = environment.getCodeModel().anonymousClass(OnClickListener);
							
							JMethod innerMethod = AnonymousOnClickListener.method(JMod.PUBLIC, environment.getCodeModel().VOID, "onClick");
							innerMethod.param(environment.getJClass("android.content.DialogInterface"), "dialog");
							innerMethod.param(environment.getCodeModel().INT, "position");
							innerMethod.annotate(Override.class);
							
							if (param.trim().length() > 1 ) {
								innerMethod.body().directStatement(param + ";");
							}
							
							if (runnableRef != null) {
								innerMethod.body().invoke(runnableRef, "run");
								calledNextRunnable = true;
							}
							
							setAction = setAction.arg(_new(AnonymousOnClickListener));
							
							continue;
		
						case "setOnKeyListener":
							break;
							
						case "setOnShowListener":
							break;
							
						case "setOnDismissListener":
							break;
							
						case "setOnCancelListener":
							break;
							
						}
					
					if (contextGetString != null) {
						setAction = setAction.arg(contextGetString.arg(direct(param)));
					} else {
						setAction = setAction.arg(direct(param));
					}
					
				}
				
				creationBlock.add(setAction);
			}			
			
			
		}
		
		creationBlock.invoke(progressDialog, "show");
		
		//If the next runnable wasn't called, it means this is probably a loading dialog
		//call it directly
		if (!calledNextRunnable && runnableRef != null) {
			creationBlock.invoke(runnableRef, "run");
		}
		
		return creationBlock;
	}
	
	@Override
	public String getClassName() {
		return "android.app.ProgressDialog";
	}
	
}

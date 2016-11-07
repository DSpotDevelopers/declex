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
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;

import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.action.BaseSystemClassActionHandler;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJAssignmentTarget;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class AlertDialogActionHandler extends BaseSystemClassActionHandler {

	@Override
	protected IJStatement getStatement(AbstractJClass DialogClass, JFieldRef runnableRef, String[] parameters,
			Element element, EComponentWithViewSupportHolder holder) {
		
		IJExpression context = holder.getContextRef();
		if (context == _this()) {
			context = holder.getGeneratedClass().staticRef("this");
		}
		
		AbstractJClass AlertDialogBuilder = environment.getJClass("android.app.AlertDialog.Builder");
		JInvocation showDialog = _new(AlertDialogBuilder).arg(context);
		
		boolean calledNextRunnable = false;
		IJAssignmentTarget assignDialogTo = null;
		JBlock onDismissBlock = null;
		
		for (String value : parameters) {
			if (value.equals("")) continue;
			
			char lastChar = value.charAt(value.length()-1);
			if (lastChar == ';' || lastChar == '?' || lastChar == '!') {
				continue;
			}
			
			if (value.startsWith(":")) {
				final String fieldName = value.substring(1);
				
				assignDialogTo = holder.getGeneratedClass().fields().get(fieldName);
				if (assignDialogTo == null) {
					assignDialogTo = holder.getGeneratedClass().field(JMod.PRIVATE, DialogClass, fieldName);
				}
				
				continue;
			}

			if (value.startsWith("=")) {
				assignDialogTo = ref(value.substring(1));
				continue;
			}
			
			Matcher matcher = Pattern.compile("\\A\\s*(\\w+)\\s*=").matcher(value);
			if (matcher.find()) {
				String method = matcher.group(1);
				
				if (method.equals("onDismiss")) {
					method = method + "Listener";
				}

				if (!method.startsWith("set")) {
					method = "set" + method.substring(0, 1).toUpperCase() + method.substring(1);
				}
								
				showDialog = showDialog.invoke(method);
				
				String[] params = value.substring(matcher.group(0).length()).split("\\s*,\\s*");
				for (int i = 0; i < params.length; i++) { 
					String param = params[i];
					
					if (i == params.length-1) 
						switch (method) {
						case "setView":
							//If is not a method, then remove the view from the parent
							//in order to permit the same view to be used more than once
							//with the same dialog
							if (!param.contains("(")) {
								if (onDismissBlock == null) onDismissBlock = new JBlock();
								
							    JVar parent = onDismissBlock.decl(
										environment.getClasses().VIEW_GROUP, 
										"parent", 
										cast(environment.getClasses().VIEW_GROUP, ref(param).invoke("getParent"))
								);
								
								onDismissBlock.invoke(parent, "removeView").arg(ref(param));
							}
							
							break;
						
						case "setAdapter":
						case "setSingleChoiceItems":
						case "setPositiveButton":
						case "setNeutralButton":
						case "setNegativeButton":
						case "setItems":
							AbstractJClass OnClickListener = environment.getJClass("android.content.DialogInterface.OnClickListener");
							JDefinedClass AnonymousOnClickListener = environment.getCodeModel().anonymousClass(OnClickListener);
							
							JMethod innerMethod = AnonymousOnClickListener.method(JMod.PUBLIC, environment.getCodeModel().VOID, "onClick");
							innerMethod.param(environment.getJClass("android.content.DialogInterface"), "dialog");
							innerMethod.param(environment.getCodeModel().INT, "position");
							innerMethod.annotate(Override.class);
							
							if (param.trim().length() > 1 ) {
								innerMethod.body().directStatement(param + ";");
							}
							
							if (runnableRef != null && !method.equals("setNegativeButton")) {
								innerMethod.body().invoke(runnableRef, "run");
								calledNextRunnable = true;
							}
							
							showDialog = showDialog.arg(_new(AnonymousOnClickListener));
							
							continue;
		
						case "setMultiChoiceItems":
							break;
							
						case "setOnKeyListener":
							break;
							
						case "setOnItemSelectedListener":
							break;
						
						case "setOnDismiss":
						case "setOnDismissListener":
							if (onDismissBlock == null) onDismissBlock = new JBlock();
							
							if (param.trim().length() > 1 ) {
								onDismissBlock.directStatement(param + ";");
							}
														
							continue;
							
						case "setOnCancelListener":
							break;
							
						}
					
					showDialog = showDialog.arg(direct(param));
					
				}
			}			
			
			
		}
		
		if (onDismissBlock != null) {
			AbstractJClass OnDismissListener = environment.getJClass("android.content.DialogInterface.OnDismissListener");
			JDefinedClass AnonymousOnDismissListener = environment.getCodeModel().anonymousClass(OnDismissListener);
			
			JMethod innerMethod = AnonymousOnDismissListener.method(JMod.PUBLIC, environment.getCodeModel().VOID, "onDismiss");
			innerMethod.param(environment.getJClass("android.content.DialogInterface"), "dialog");
			innerMethod.annotate(Override.class);
			
			innerMethod.body().add(onDismissBlock);
			
			showDialog = showDialog.invoke("setOnDismissListener").arg(_new(AnonymousOnDismissListener));
		}
		
		
		JBlock block = new JBlock();
		
		if (assignDialogTo != null) {
			block.assign(assignDialogTo, showDialog.invoke("create"));
			block.invoke(assignDialogTo, "show");
		} else {
			block.add(showDialog.invoke("create").invoke("show"));
		}
		
		//If the next runnable wasn't called, it means this is probably a loading dialog
		//call it directly
		if (!calledNextRunnable && runnableRef != null) {
			block.invoke(runnableRef, "run");
		}
		
		return block;
	}
	
	@Override
	public String getClassName() {
		return "android.app.AlertDialog";
	}
	
}

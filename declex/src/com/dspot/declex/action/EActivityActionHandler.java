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
package com.dspot.declex.action;

import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.lit;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.holder.HasOnActivityResult;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMod;

public class EActivityActionHandler extends BaseActionHandler {

	@Override
	public boolean canProcessElement(Element element, AndroidAnnotationsEnvironment environment) {
		super.canProcessElement(element, environment);
		
		String elementClass = element.asType().toString();
		TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(elementClass);
		if (typeElement == null) return false;
		
		EActivity annotation = typeElement.getAnnotation(EActivity.class); 
		if (annotation != null) return true;
		
		return false;
	}

	@Override
	protected IJStatement getStatement(AbstractJClass ActivityClass, JFieldRef runnableRef, String[] parameters,
			Element element, EComponentWithViewSupportHolder holder) {
		
		IJExpression context = holder.getContextRef();
		if (element.getEnclosingElement().getAnnotation(EFragment.class) != null) {
			context = _this();
		}
		
		if (context == _this()) {
			context = holder.getGeneratedClass().staticRef("this");
		}
		
		JInvocation activityStart = ActivityClass.staticInvoke("intent").arg(context);
		
		boolean startForResult = false;
		for (String value : parameters) {
			if (value.trim().toLowerCase().startsWith("result:")) {
				value = value.substring(7);
				startForResult = true;
			}
			
			if (value.equals("")) continue;
			
			char lastChar = value.charAt(value.length()-1);
			if (lastChar == ';' || lastChar == '?' || lastChar == '!') {
				continue;
			}
			
			Matcher matcher = Pattern.compile("\\A\\s*(\\w+)\\s*=").matcher(value);
			if (matcher.find()) {
				String assignment = value.substring(matcher.group(0).length());
				if (assignment.trim().equals("")) assignment = matcher.group(1);
				
				activityStart = activityStart.invoke(matcher.group(1)).arg(direct(assignment));
			}					
		}
		
		if (startForResult) {
			int random = new Random().nextInt(0xFFFF);
			JFieldVar requestCode = holder.getGeneratedClass().field(
					JMod.PRIVATE | JMod.FINAL, environment.getCodeModel().INT, "RequestCode" + random, lit(random)
				);
			
			if (runnableRef != null && holder instanceof HasOnActivityResult) {
				HasOnActivityResult h = (HasOnActivityResult) holder;
				
				JBlock block = h.getOnActivityResultCaseBlock(random);
				block = block._if(h.getOnActivityResultResultCodeParam().eq(environment.getClasses().ACTIVITY.staticRef("RESULT_OK")))._then();
				block.invoke(runnableRef, "run");
			}
			
			return activityStart.invoke("startForResult").arg(requestCode);
		}
		
		return activityStart.invoke("start");
	}
	
}

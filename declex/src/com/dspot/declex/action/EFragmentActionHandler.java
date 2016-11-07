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
package com.dspot.declex.action;

import static com.helger.jcodemodel.JExpr.direct;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;

public class EFragmentActionHandler extends BaseActionHandler {
	
	@Override
	public boolean canProcessElement(Element element, AndroidAnnotationsEnvironment environment) {
		super.canProcessElement(element, environment);
		
		String elementClass = element.asType().toString();
		TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(elementClass);
		if (typeElement == null) return false;
		
		EFragment annotation = typeElement.getAnnotation(EFragment.class); 
		if (annotation != null) return true;
		
		return false;
	}

	@Override
	protected IJStatement getStatement(AbstractJClass FragmentClass, JFieldRef runnableRef, String[] parameters,
			Element element, EComponentWithViewSupportHolder holder) {
		
		JInvocation fragmentBuild = FragmentClass.staticInvoke("builder");
		
		for (String value : parameters) {
			if (value.equals("")) continue;
			
			char lastChar = value.charAt(value.length()-1);
			if (lastChar == ';' || lastChar == '?' || lastChar == '!') {
				continue;
			}
			
			Matcher matcher = Pattern.compile("\\A\\s*(\\w+)\\s*=").matcher(value);
			if (matcher.find()) {
				String assignment = value.substring(matcher.group(0).length());
				if (assignment.trim().equals("")) assignment = matcher.group(1);
				
				fragmentBuild = fragmentBuild.invoke(matcher.group(1)).arg(direct(assignment));
			}					
		}
		
		String getFragmentManager = "getFragmentManager";
		String elementSuperClass = holder.getGeneratedClass().fullName();
		elementSuperClass = elementSuperClass.substring(0, elementSuperClass.length()-1);
		if (TypeUtils.isSubtype(elementSuperClass, "android.support.v7.app.ActionBarActivity", processingEnv)) {
			getFragmentManager = "getSupportFragmentManager";
		}
		if (TypeUtils.isSubtype(elementSuperClass, "android.support.v7.app.AppCompatActivity", processingEnv)) {
			getFragmentManager = "getSupportFragmentManager";
		}
		
		JFieldRef thiz = holder.getGeneratedClass().staticRef("this");
		JInvocation transaction = thiz.invoke(getFragmentManager)
			    .invoke("beginTransaction")
			    .invoke("replace").arg(direct("R.id.container")).arg(fragmentBuild.invoke("build")); 
		
		for (String value : parameters) {
			if (value.equals("")) continue;
			
			char lastChar = value.charAt(value.length()-1);
			if (lastChar == ';' || lastChar == '?' || lastChar == '!') {
				continue;
			}
			
			Matcher matcher = Pattern.compile("\\A\\s*\\+(\\w+)\\s*=").matcher(value);
			if (matcher.find()) {
				String assignment = value.substring(matcher.group(0).length());
				if (assignment.trim().equals("")) assignment = matcher.group(1);
				
				transaction = transaction.invoke(matcher.group(1)).arg(direct(assignment));
			}					
		}
		
		return transaction.invoke("commit");
	}
	
}

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

import static com.helger.jcodemodel.JExpr.direct;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.util.SharedRecords;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;

public class EventsActionHandler extends BaseActionHandler {

	@Override
	public boolean canProcessElement(Element element, AndroidAnnotationsEnvironment environment) {
		super.canProcessElement(element, environment);
		
		String elementClass = element.asType().toString();
		if (SharedRecords.getEvent(elementClass, environment) != null)
			return true;
		
		return false;
	}

	@Override
	protected IJStatement getStatement(AbstractJClass EventClass, JFieldRef runnableRef, String[] parameters,
			Element element, EComponentWithViewSupportHolder holder) {
		
		JInvocation event = EventClass.staticInvoke("create");
		
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
				
				String method = matcher.group(1);
				if (!method.startsWith("set")) {
					method = "set" + method.substring(0, 1).toUpperCase() + method.substring(1);
				} 
				
				event = event.invoke(method).arg(direct(assignment));
			}					
		}
		
		if (runnableRef != null) {
			event = event.invoke("setValues").arg(runnableRef);
		}
		
		return event.invoke("postEvent");
	}

}

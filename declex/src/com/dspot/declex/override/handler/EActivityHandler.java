/**
 * Copyright (C) 2016-2019 DSpot Sp. z o.o
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
package com.dspot.declex.override.handler;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import com.dspot.declex.helper.ActionHelper;
import com.dspot.declex.holder.EventHolder;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EActivityHolder;

import com.dspot.declex.override.holder.ActivityActionHolder;


public class EActivityHandler extends org.androidannotations.internal.core.handler.EActivityHandler {

	public EActivityHandler(AndroidAnnotationsEnvironment environment) {
		super(environment);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		if (element.getKind().equals(ElementKind.CLASS)) {
			ActionHelper.getInstance(getEnvironment()).validate(element, this);
		}
		
		super.validate(element, valid);
		
		if (!getEnvironment().getValidatedElements().isAncestor(element)) {
			ActivityActionHolder.createInformationForActionHolder(element, getEnvironment());
		}
	}
	
	@Override
	public void process(Element element, EActivityHolder holder) {
		super.process(element, holder);
		
		if (!getEnvironment().getValidatedElements().isAncestor(element)) {
			ActivityActionHolder actionHolder = holder.getPluginHolder(new ActivityActionHolder(holder));
			actionHolder.getActivityAction();
		}

		EventHolder eventHolder = holder.getPluginHolder(new EventHolder(holder));
		eventHolder.setEventRegisteringBlock(holder.getOnResumeAfterSuperBlock());
		eventHolder.setEventUnregisteringBlock(holder.getOnPauseBeforeSuperBlock());
	}

}

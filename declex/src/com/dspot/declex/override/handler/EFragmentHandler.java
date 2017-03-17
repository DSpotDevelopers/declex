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
package com.dspot.declex.override.handler;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EFragmentHolder;

import com.dspot.declex.override.holder.FragmentActionHolder;


public class EFragmentHandler extends org.androidannotations.internal.core.handler.EFragmentHandler {

	public EFragmentHandler(AndroidAnnotationsEnvironment environment) {
		super(environment);
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {
		super.validate(element, valid);
		
		FragmentActionHolder.createInformationForActionHolder(element, getEnvironment());
	}
	
	@Override
	public void process(Element element, EFragmentHolder holder) {
		super.process(element, holder);
		
		FragmentActionHolder actionHolder = holder.getPluginHolder(new FragmentActionHolder(holder));
		actionHolder.getFragmentAction();
	}

}

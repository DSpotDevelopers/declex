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
package com.dspot.declex.handler;

import java.lang.annotation.Annotation;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EComponentHolder;

import com.dspot.declex.annotation.JsonModel;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.PreferenceModel;
import com.dspot.declex.handler.base.BaseTemplateHandler;

public class PreferenceModelHandler extends BaseTemplateHandler<EComponentHolder> {
		
	public PreferenceModelHandler(AndroidAnnotationsEnvironment environment) {
		super(PreferenceModel.class, environment, 
				"com/dspot/declex/template/", "PreferenceModel.ftl.java");
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Object> dependencies) {
		if (element.getKind().equals(ElementKind.CLASS)) {
			dependencies.put(element, JsonModel.class);
		} else {
			dependencies.put(element, Model.class);
		}
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {
		
	}

	
	@Override
	public void process(Element element, EComponentHolder holder) {
		
		if (element.getKind().isField()) return;

		
		super.process(element, holder);
		
	}
}

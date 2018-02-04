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
package com.dspot.declex.handler;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.IdValidatorHelper.FallbackStrategy;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.annotation.ExportRecollect;
import com.dspot.declex.annotation.PutOnAction;
import com.dspot.declex.annotation.Recollect;

public class PutOnActionHandler extends BaseAnnotationHandler<EComponentHolder> {

	public PutOnActionHandler(AndroidAnnotationsEnvironment environment) {
		super(PutOnAction.class, environment);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		
		if (adiHelper.getAnnotation(element, ExportRecollect.class) != null) {
			valid.addError("@PutOnAction is not available in @ExportRecollect annotated fields");
		}
		
		if (adiHelper.hasAnnotation(element, Recollect.class)) {
			valid.addError("@PutOnAction should be used in fields annotated with @Recollect");
		}
		
		validatorHelper.resIdsExist(element, Res.ID, FallbackStrategy.NEED_RES_ID, valid);
	}

	@Override
	public void process(Element element, EComponentHolder holder) throws Exception {
		
	}
	
}

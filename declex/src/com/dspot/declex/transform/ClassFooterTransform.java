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
package com.dspot.declex.transform;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.androidannotations.holder.GeneratedClassHolder;

import com.dspot.declex.transform.writer.BaseTemplateTransformWriter;

import freemarker.template.TemplateModelException;

public class ClassFooterTransform<T extends GeneratedClassHolder> extends BaseTemplateTransform<T> {

	public ClassFooterTransform(T holder) {
		super(holder);
	}

	@Override
	public Writer getWriter(Writer out, Map args)
			throws TemplateModelException, IOException {
		return new BaseTemplateTransformWriter<T>(out, holder);
	}
}

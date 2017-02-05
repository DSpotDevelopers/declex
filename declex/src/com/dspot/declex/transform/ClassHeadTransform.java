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
package com.dspot.declex.transform;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.androidannotations.holder.BaseGeneratedClassHolder;

import com.dspot.declex.share.holder.EnsureImportsHolder;
import com.dspot.declex.transform.writer.BaseTemplateTransformWriter;

import freemarker.template.TemplateModelException;

public class ClassHeadTransform<T extends BaseGeneratedClassHolder> extends BaseTemplateTransform<T> {
	
	public ClassHeadTransform(T holder) {
		super(holder);
	}

	@Override
	public Writer getWriter(Writer out, Map args)
			throws TemplateModelException, IOException {
		return new ClassHeadWriter(out, holder);
	}

	private class ClassHeadWriter extends BaseTemplateTransformWriter<T> {
		
		public ClassHeadWriter(Writer out, T holder) {
			super(out, holder);
		}
		
		@Override
		public void close() {
			super.close();
			
			String[] code = strCode.split("\\r?\\n");
			
			//Catch the imports
			EnsureImportsHolder importsHolder = holder.getPluginHolder(new EnsureImportsHolder(holder));
			Pattern pattern = Pattern.compile("\\s*import\\s+((\\w|\\.)+)\\s*;");
			
			for (String line : code) {
				Matcher match = pattern.matcher(line);
				
				if (match.find()) 
					try {
						String importClass = match.group(1);
						importsHolder.ensureImport(importClass);
					} catch (Exception e){}
			}
			
			
			//Catch the implements 
			pattern = Pattern.compile(".+\\s(implements)\\s((\\w|\\s|,)+)\\{");
			Matcher match = pattern.matcher(strCode);
			
			if (match.find()) 
				try {
					String[] implements_ = match.group(2).replaceAll("\\s", "").split(",");
					
					for (String impl : implements_) 
						for (String importClass : importsHolder.getImportedClasses()) 
							if (importClass.endsWith("." + impl)) {
								holder.getGeneratedClass()._implements(holder.getEnvironment().getJClass(importClass));
							}
					
				} catch (Exception e){}
		}
		
	}
}

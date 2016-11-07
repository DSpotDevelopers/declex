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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.androidannotations.holder.BaseGeneratedClassHolder;

import com.dspot.declex.share.holder.EnsureImportsHolder;
import com.dspot.declex.transform.writer.BaseTemplateTransformWriter;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JMod;

import freemarker.template.TemplateModelException;

public class ClassFieldsTransform extends BaseTemplateTransform {
	
	public ClassFieldsTransform(BaseGeneratedClassHolder holder) {
		super(holder);
	}

	@Override
	public Writer getWriter(Writer out, Map args)
			throws TemplateModelException, IOException {
		return new ClassFieldsWriter(out, holder);
	}

	private class ClassFieldsWriter extends BaseTemplateTransformWriter {

		public ClassFieldsWriter(Writer out, BaseGeneratedClassHolder holder) {
			super(out, holder);
		}
		
		@Override
		public void close() {
			super.close();
			
			//Get all the imported classes
			EnsureImportsHolder importsHolder = holder.getPluginHolder(new EnsureImportsHolder(holder));
			final List<String> importClasses = importsHolder.getImportedClasses();
			
			//Catch the normal declaration
			Pattern pattern = Pattern.compile("((?:private\\s+|public\\s+|protected\\s+)*(?:final\\s+|static\\s+)*(?:final\\s+|static\\s+)*)" +
											  "(\\w+(?:<\\?>|<\\w+>)?(?:\\[\\])?)\\s+(\\w+)\\s*;");
			Matcher match = pattern.matcher(strCode);
				
			while (match.find()) 
				try {
					int mods = 0;
					if (match.group(1).contains("private")) mods |= JMod.PRIVATE;
					if (match.group(1).contains("protected")) mods |= JMod.PROTECTED;
					if (match.group(1).contains("public")) mods |= JMod.PUBLIC;
					if (match.group(1).contains("final")) mods |= JMod.FINAL;
					if (match.group(1).contains("static")) mods |= JMod.STATIC;
					
					String type = match.group(2);
					for (String importClass : importClasses) 
						if (importClass.endsWith("." + type)) {
							type = importClass;
							break;
						}
					
					holder.getGeneratedClass().field(
							mods, 
							holder.getEnvironment().getJClass(type), 
							match.group(3)
						);
				} catch (Exception e){}
			
			
			//Catch declarations with expresions
			pattern = Pattern.compile("((?:private\\s+|public\\s+|protected\\s+)*(?:final\\s+|static\\s+)*(?:final\\s+|static\\s+)*)" +
					  "(\\w+(?:<\\?>|<\\w+>)?(?:\\[\\])?)\\s+(\\w+)\\s*=\\s*([^;]+);");
			match = pattern.matcher(strCode);
			
			while (match.find()) 
				try {
					int mods = 0;
					if (match.group(1).contains("private")) mods |= JMod.PRIVATE;
					if (match.group(1).contains("protected")) mods |= JMod.PROTECTED;
					if (match.group(1).contains("public")) mods |= JMod.PUBLIC;
					if (match.group(1).contains("final")) mods |= JMod.FINAL;
					if (match.group(1).contains("static")) mods |= JMod.STATIC;
					
					String type = match.group(2);
					for (String importClass : importClasses) 
						if (importClass.endsWith("." + type)) {
							type = importClass;
							break;
						}
					
					holder.getGeneratedClass().field(
							mods, 
							holder.getEnvironment().getJClass(type), 
							match.group(3),
							JExpr.direct(match.group(4))
						);
				} catch (Exception e){}
			
		}
		
	}
}

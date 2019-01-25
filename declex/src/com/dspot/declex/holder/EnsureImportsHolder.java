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
package com.dspot.declex.holder;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.androidannotations.holder.GeneratedClassHolder;
import org.androidannotations.plugin.PluginClassHolder;

import com.helger.jcodemodel.JAnnotationUse;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;

public class EnsureImportsHolder extends PluginClassHolder<GeneratedClassHolder> {

	private List<String> importedClasses = new LinkedList<>();
	private JMethod importsMethod;
	
	public EnsureImportsHolder(GeneratedClassHolder holder) {
		super(holder);
	}
	
	public List<String> getImportedClasses() {
		return Collections.unmodifiableList(importedClasses);
	}
	
	public void ensureImport(String clazz) {
		if (importsMethod == null) {
			setImportsMethod();
		}
		
		if (!importedClasses.contains(clazz)) {
			importsMethod.body().decl(
	    			getJClass(clazz), 
	    			"importEnsure" + importsMethod.body().getContents().size(),
	    			JExpr._null()
	    		);
			importedClasses.add(clazz);
		}
	}
	
	private void setImportsMethod() {
		importsMethod = getGeneratedClass().method(JMod.PRIVATE, getCodeModel().VOID, "ensureImports");
		importsMethod.body().directStatement("//This ensures that all the imports be added to the class");
		
		JAnnotationUse annotation = importsMethod.annotate(getJClass("SuppressWarnings"));
		annotation.paramArray("value").param("unused").param("rawtypes");
	}

}

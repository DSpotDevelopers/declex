package com.dspot.declex.share.holder;

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

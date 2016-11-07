package com.dspot.declex.event.holder;

import java.util.Set;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;

public interface ViewListenerHolder {
	public void addDecl(String viewFieldName, int mods, AbstractJClass cls, String name, IJExpression statement);
	public void addStatement(String viewFieldName, IJStatement expression);
	
	public Set<String> getViewFieldNames();
	
	public JBlock createListener(String viewFieldName, JBlock block);
}

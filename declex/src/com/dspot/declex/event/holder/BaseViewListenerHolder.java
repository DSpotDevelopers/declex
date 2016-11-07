package com.dspot.declex.event.holder;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.VariableElement;

import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.internal.core.handler.AbstractViewListenerHandler;
import org.androidannotations.plugin.PluginClassHolder;

import com.dspot.declex.util.DeclexConstant;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JMethod;

public class BaseViewListenerHolder extends PluginClassHolder<EComponentWithViewSupportHolder> 
	implements ViewListenerHolder {

	private Map<String, ListenerInfo> listenerInfos = new HashMap<>();
	
	protected AbstractViewListenerHandler viewListenerHandler;
	
	public BaseViewListenerHolder(AbstractViewListenerHandler viewListenerHandler, EComponentWithViewSupportHolder holder) {
		super(holder);
		this.viewListenerHandler = viewListenerHandler;
	}
	
	private ListenerInfo getListenerInfo(String viewFieldName) {
		ListenerInfo listenerInfo = listenerInfos.get(viewFieldName);
		if (listenerInfo == null) {
			listenerInfo = new ListenerInfo();
			listenerInfos.put(viewFieldName, listenerInfo);
		}
		
		return listenerInfo;
	}
	
	protected IJExpression returnedExpression() {
		return null;
	}
	
	@Override
	public Set<String> getViewFieldNames() {
		return Collections.unmodifiableSet(listenerInfos.keySet());
	}

	@Override
	public void addDecl(String viewFieldName, int mods, AbstractJClass cls,
			String name, IJExpression expression) {
		ListenerInfo listenerInfo = getListenerInfo(viewFieldName);

		DeclData declData = new DeclData(mods, cls, name, expression);
		listenerInfo.decls.add(declData);
		
		//If the listener block was already created, simply add the statement
		if (listenerInfo.block != null) {
			listenerInfo.block.decl(mods, cls, name, expression);
		}
	}
	
	@Override
	public void addStatement(String viewFieldName, IJStatement statement) {
		ListenerInfo listenerInfo = getListenerInfo(viewFieldName);
		
		listenerInfo.statements.add(statement);
		
		//If the listener block was already created, simply add the statement
		if (listenerInfo.block != null) {
			listenerInfo.block.add(statement);
		}
	}

	@Override
	public JBlock createListener(String viewFieldName, JBlock block) {
				
		String[] viewFields = viewFieldName.split("\\.");
		JFieldRef view = null;
		for (String viewField : viewFields) {
			if (view == null) view = ref(viewField);
			else view = view.ref(viewField);
			
			//viewFieldName will have the last field name
			viewFieldName = viewField;
		}

		if (viewFieldName.endsWith(DeclexConstant.VIEW)) 
			viewFieldName = viewFieldName.substring(0, viewFieldName.length() - DeclexConstant.VIEW.length());
		
		ListenerInfo listenerInfo = getListenerInfo(viewFieldName);
		if (listenerInfo.block == null) {
			
			JDefinedClass listenerAnonymousClass = getCodeModel().anonymousClass(viewListenerHandler.getListenerClass(holder()));
			JMethod listenerMethod = viewListenerHandler.createListenerMethod(listenerAnonymousClass);
			listenerMethod.annotate(Override.class);

			JBlock listenerMethodBody = listenerMethod.body();
			
			viewListenerHandler.processParameters(holder(), listenerMethod, null, new ArrayList<VariableElement>());
			
			block.invoke(view, viewListenerHandler.getSetterName()).arg(_new(listenerAnonymousClass));
			
			listenerInfo.block = listenerMethodBody;
			
			IJExpression returnedExpression = returnedExpression();
			if (returnedExpression != null) {
				listenerInfo.block = listenerMethodBody.block();
				listenerMethodBody._return(returnedExpression);
			}
			
			//Add all the pre-added declarations
			for (DeclData decl : listenerInfo.decls) {
				listenerInfo.block.decl(decl.mods, decl.cls, decl.name, decl.expression);
			}
			
			//Add all the pre-added statements
			for (IJStatement statement : listenerInfo.statements) {
				listenerInfo.block.add(statement);
			}
		}		
		
		return listenerInfo.block;
	}
	
	private class ListenerInfo {
		List<IJStatement> statements = new LinkedList<>();
		List<DeclData> decls = new LinkedList<>();
		JBlock block; 
	}
	
	private class DeclData {
		int mods; 
		AbstractJClass cls;
		String name;
		IJExpression expression;
		
		public DeclData(int mods, AbstractJClass cls, String name, IJExpression expression) {
			super();
			this.mods = mods;
			this.cls = cls;
			this.name = name;
			this.expression = expression;
		}
	}
	
}

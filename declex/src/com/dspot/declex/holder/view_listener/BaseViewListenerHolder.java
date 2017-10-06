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
package com.dspot.declex.holder.view_listener;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
			
			if (viewFieldName.contains(".")) {
				String[] viewFields = viewFieldName.split("\\.");
				ListenerInfo globalListenerInfo = listenerInfos.get(viewFields[viewFields.length-1]);
				
				//Add all the pre-added declarations
				for (DeclData decl : globalListenerInfo.decls) {
					addDecl(viewFieldName, decl.mods, decl.cls, decl.name, decl.expression);
				}
				
				//Add all the pre-added statements
				for (IStatementCreator statementCreator : globalListenerInfo.statementCreators) {
					addStatement(viewFieldName, statementCreator);
				}
			}
		}
		
		return listenerInfo;
	}
	
	protected IJExpression returnedExpression() {
		return null;
	}
	
	@Override
	public Set<String> getViewFieldNames() {
		Set<String> set = new HashSet<>();
		
		for (String viewFieldName : listenerInfos.keySet()) {			
			String[] viewFields = viewFieldName.split("\\.");
			set.add(viewFields[viewFields.length-1]);
		}
		
		return Collections.unmodifiableSet(set);
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
	public void addStatement(String viewFieldName, IStatementCreator statementCreator) {
		ListenerInfo listenerInfo = getListenerInfo(viewFieldName);
		
		listenerInfo.statementCreators.add(statementCreator);
		
		//If the listener block was already created, simply add the statement
		if (listenerInfo.block != null) {
			IJStatement statement = statementCreator.getStatement();
			if (statement != null) {
				listenerInfo.block.add(statement);
			}
		}
	}

	@Override
	public JBlock createListener(String viewFieldName, JBlock block) {
		
		String[] viewFields = viewFieldName.split("\\.");
		JFieldRef view = null;
		for (String viewField : viewFields) {
			if (view == null) view = ref(viewField);
			else view = view.ref(viewField);
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
			for (IStatementCreator statementCreator : listenerInfo.statementCreators) {
				IJStatement statement = statementCreator.getStatement();
				if (statement != null) {
					listenerInfo.block.add(statement);
				}
			}
		}		
		
		return listenerInfo.block;
	}
	
	private class ListenerInfo {
		List<IStatementCreator> statementCreators = new LinkedList<>();
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

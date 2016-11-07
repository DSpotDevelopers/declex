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
package com.dspot.declex.populator.adapter;

import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.List;

import javax.lang.model.element.Element;

import org.androidannotations.holder.GeneratedClassHolder;

import com.dspot.declex.plugin.HolderClassCreator;
import com.dspot.declex.plugin.JClassPlugin;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JClassAlreadyExistsException;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class AdapterClassCreator extends HolderClassCreator {
	AbstractJClass BaseAdapter;
	final AbstractJClass Model;
	
	final String className;
	
	public AdapterClassCreator(String modelClassName, String className, Element element, GeneratedClassHolder holder, 
			List<JClassPlugin> adapterPlugins) {
		super(element, holder);

		BaseAdapter = getJClass("android.widget.BaseAdapter");
		Model = getJClass(modelClassName);
		this.className = className;
		
		for (JClassPlugin plugin : adapterPlugins) {
			AbstractJClass newBaseAdapter = plugin.getBaseAdapter(element);
			if (newBaseAdapter != null) {
				BaseAdapter = newBaseAdapter;
			}
			this.addPlugin(plugin);
		}
	}

	@Override
	public JDefinedClass getDefinedClass() throws JClassAlreadyExistsException {
		
		JDefinedClass AdapterClass = holder.getGeneratedClass()._class(JMod.PRIVATE, className)._extends(BaseAdapter);
		
		JFieldVar models = AdapterClass.field(JMod.PRIVATE, getClasses().LIST.narrow(Model), "models");
		
		JMethod constructor = AdapterClass.constructor(JMod.PUBLIC);
		constructor.body().assign(_this().ref(models), constructor.param(getClasses().LIST.narrow(Model), "models"));
		
		//setModels() METHOD
		JMethod setModels = AdapterClass.method(JMod.PUBLIC, getCodeModel().VOID, "setModels");
		JVar modelsParam = setModels.param(getClasses().LIST.narrow(Model), "models");
		JConditional ifModels = setModels.body()._if(modelsParam.ne(_null()));
		ifModels._then().assign(_this().ref(models), modelsParam);
		ifModels._else().invoke(_this().ref(models), "clear");
		
		//getCount() METHOD
		JMethod getCountMethod = AdapterClass.method(JMod.PUBLIC, getCodeModel().INT, "getCount");
		getCountMethod.annotate(Override.class);
		getCountMethod.body()._if(models.eq(_null()))._then()._return(lit(0));
		
		//getItem() METHOD
		JMethod getItemMethod = AdapterClass.method(JMod.PUBLIC, getClasses().OBJECT, "getItem");
		getItemMethod.annotate(Override.class);
		getItemMethod.param(getCodeModel().INT, "position");
		
		//getItemId() METHOD
		JMethod getItemIdMethod = AdapterClass.method(JMod.PUBLIC, getCodeModel().LONG, "getItemId");
		getItemIdMethod.annotate(Override.class);
		getItemIdMethod.param(getCodeModel().INT, "position");
		
		//inflate() METHOD
		JMethod inflateMethod = AdapterClass.method(JMod.PUBLIC, getClasses().VIEW, "inflate");
		inflateMethod.param(getCodeModel().INT, "position");
		inflateMethod.param(getClasses().VIEW, "convertView");
		inflateMethod.param(getClasses().LAYOUT_INFLATER, "inflater");
		
		//getView() METHOD
		JMethod getViewMethod = AdapterClass.method(JMod.PUBLIC, getClasses().VIEW, "getView");
		getViewMethod.annotate(Override.class);
		getViewMethod.param(JMod.FINAL, getCodeModel().INT, "position");
		getViewMethod.param(getClasses().VIEW, "convertView");
		JVar parent = getViewMethod.param(JMod.FINAL, getClasses().VIEW_GROUP, "parent");
		
		//Declare a inflater
		getViewMethod.body().decl(
				JMod.FINAL, 
				getJClass("android.view.LayoutInflater"), 
				"inflater", 
				cast(
						getJClass("android.view.LayoutInflater"), 
						parent.invoke("getContext").invoke("getSystemService")
							   .arg(getClasses().CONTEXT.staticRef("LAYOUT_INFLATER_SERVICE"))
					)
			);
		
		for (JClassPlugin plugin : plugins) {
			plugin.process(element, AdapterClass);
		}
		
		getCountMethod.body()._return(models.invoke("size"));
		getItemMethod.body()._return(models.invoke("get").arg(ref("position")));
		getItemIdMethod.body()._return(lit(0));
		
		return AdapterClass;
	}

}

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
package com.dspot.declex.override.handler;

import java.util.Map;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.annotations.ItemLongClick;
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.event.holder.ItemLongClickHolder;
import com.dspot.declex.event.holder.ViewListenerHolder;
import com.dspot.declex.share.holder.ViewsHolder;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;

public class ItemLongClickHandler extends BaseViewListenerHandler {

	public ItemLongClickHandler(AndroidAnnotationsEnvironment environment) {
		super(ItemLongClick.class, environment);
	}
	
	@Override
	protected boolean isList() {
		return true;
	}
	
	@Override
	protected ViewListenerHolder getListenerHolder(String elementName,
			String elementClass, Map<AbstractJClass, IJExpression> declForListener,
			Element element, ViewsHolder viewsHolder, EComponentWithViewSupportHolder holder) {
		createDeclarationForLists(elementName, declForListener, element, viewsHolder);
		return holder.getPluginHolder(new ItemLongClickHolder(holder));
	}
}

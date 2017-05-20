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

import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.internal.core.handler.EditorActionHandler;

import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JExpr;


public class EditorActionHolder extends BaseViewListenerHolder {
		
	public EditorActionHolder(EComponentWithViewSupportHolder holder) {
		super(new EditorActionHandler(holder.getEnvironment()), holder);
	}
	
	@Override
	protected IJExpression returnedExpression() {
		return JExpr.TRUE;
	}
}

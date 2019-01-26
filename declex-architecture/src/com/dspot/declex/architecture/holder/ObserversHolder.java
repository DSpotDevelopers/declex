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
package com.dspot.declex.architecture.holder;

import com.helger.jcodemodel.*;
import org.androidannotations.holder.GeneratedClassHolder;
import org.androidannotations.plugin.PluginClassHolder;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.dspot.declex.architecture.ArchCanonicalNameConstants.OBSERVER;

public class ObserversHolder extends PluginClassHolder<GeneratedClassHolder> {

	private JMethod removeObserverMethod;
	private JVar removeObserverMethodParam;

	public ObserversHolder(GeneratedClassHolder holder) {
		super(holder);
	}

	public JMethod getRemoveObserverMethod() {
		if (removeObserverMethod == null) {
			setRemoveObserverMethod();
		}
		return removeObserverMethod;
	}

	public JVar getRemoveObserverMethodParam() {
		if (removeObserverMethodParam == null) {
			setRemoveObserverMethod();
		}
		return removeObserverMethodParam;
	}

	private void setRemoveObserverMethod() {
		removeObserverMethod = getGeneratedClass().method(JMod.PRIVATE, getCodeModel().VOID, "removeObserver_");
		removeObserverMethodParam = removeObserverMethod.param(getJClass(OBSERVER), "observer");
	}

}

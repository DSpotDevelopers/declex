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

import com.dspot.declex.architecture.api.MethodCall;
import com.helger.jcodemodel.*;
import org.androidannotations.holder.EComponentHolder;

import static com.dspot.declex.architecture.ArchCanonicalNameConstants.*;
import static com.helger.jcodemodel.JExpr.*;
import static com.helger.jcodemodel.JMod.PRIVATE;

public class ViewPresenterHolder extends BaseArchitecturalHolder {

	public static final String PERFORM_METHOD_CALL_NAME = "performMethodCall";

	private JFieldVar presenterLiveDataField;
	private JMethod performMethodCallMethod;
	private JBlock performMethodCallBlock;
	private JVar performMethodCallField;

	private JFieldVar presenterLiveDataObserverField;

	public ViewPresenterHolder(EComponentHolder holder) {
		super(holder);
	}

	public JFieldVar getPresenterLiveDataField() {
		if (presenterLiveDataField == null) {
			setPresenterLiveData();
		}
		return presenterLiveDataField;
	}

	public JBlock getPerformMethodCallBlock() {
		if (performMethodCallBlock == null) {
			setPerformMethodCall();
		}
		return performMethodCallBlock;
	}

	public JVar getPerformMethodCallField() {
		if (performMethodCallField == null) {
			setPerformMethodCall();
		}
		return performMethodCallField;
	}

	private void setPresenterLiveData() {
		AbstractJClass SingleLiveEvent = getJClass(SINGLE_LIVE_EVENT);

		//Create the Presenter LiveData field
		presenterLiveDataField = holder().getGeneratedClass().field(PRIVATE, SingleLiveEvent.narrow(MethodCall.class), "presenterLiveData_");

		//Create the live data object
		holder().getInitBodyInjectionBlock().assign(presenterLiveDataField, _new(SingleLiveEvent));

	}

	private void setPerformMethodCall() {
		performMethodCallMethod = holder()
				.getGeneratedClass().method(PRIVATE, getCodeModel().VOID, PERFORM_METHOD_CALL_NAME);

		performMethodCallField = performMethodCallMethod.param(getJClass(MethodCall.class), "methodCall");

		performMethodCallBlock = performMethodCallMethod.body();

		setViewPresenterObserver();
	}

	private void setViewPresenterObserver() {

		final AbstractJClass Observer = getJClass(OBSERVER);
		final AbstractJClass MethodCall = getJClass(com.dspot.declex.architecture.api.MethodCall.class);

		//Create Observer field
		presenterLiveDataObserverField = holder().getGeneratedClass().field(PRIVATE, Observer.narrow(MethodCall), "presenterLiveDataObserver_");

		JDefinedClass AnonymousObserver = getCodeModel().anonymousClass(Observer.narrow(MethodCall));
		JMethod anonymousOnChanged = AnonymousObserver.method(JMod.PUBLIC, getCodeModel().VOID, "onChanged");
		anonymousOnChanged.annotate(Override.class);
		JVar param = anonymousOnChanged.param(MethodCall, "value");

		//Call this method
		anonymousOnChanged.body().invoke(performMethodCallMethod).arg(param);

		holder().getInitBodyInjectionBlock().assign(presenterLiveDataObserverField, _new(AnonymousObserver));

		holder().getInitBodyAfterInjectionBlock()
				.invoke(getPresenterLiveDataField(), "observe")
				.arg(cast(getJClass(LIFECYCLE_OWNER), getRootViewField())).arg(presenterLiveDataObserverField);

	}

}

/**
 * Copyright (C) 2016-2018 DSpot Sp. z o.o
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
package com.dspot.declex.util;

import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.ref;

import com.dspot.declex.holder.ViewsHolder;
import com.helger.jcodemodel.JInvocation;

public class ParamUtils {
	public static JInvocation injectParam(String paramName, String paramType, JInvocation invocation) {
		return injectParam(paramName, paramType, invocation, null);
	}
	
	public static JInvocation injectParam(String paramName, String paramType, JInvocation invocation, ViewsHolder viewsHolder) {
		if (viewsHolder != null) {
			return viewsHolder.checkFieldNameInInvocation(paramName, paramType, invocation);
		} else {
			if (paramName.equals("_this")) return invocation.arg(_this());
			return invocation.arg(ref(paramName));
		}
	}
}

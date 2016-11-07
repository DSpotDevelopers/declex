package com.dspot.declex.util;

import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.ref;

import com.dspot.declex.share.holder.ViewsHolder;
import com.helger.jcodemodel.JInvocation;

public class ParamUtils {
	public static void injectParam(String paramName, JInvocation invocation) {
		injectParam(paramName, invocation, null);
	}
	
	public static void injectParam(String paramName, JInvocation invocation, ViewsHolder viewsHolder) {
		if (viewsHolder != null) {
			viewsHolder.checkFieldNameInInvocation(paramName, invocation);
		} else {
			if (paramName.equals("$this")) invocation.arg(_this());
			else invocation.arg(ref(paramName));
		}
	}
}

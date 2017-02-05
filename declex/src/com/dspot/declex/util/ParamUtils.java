package com.dspot.declex.util;

import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.ref;

import com.dspot.declex.share.holder.ViewsHolder;
import com.helger.jcodemodel.JInvocation;

public class ParamUtils {
	public static JInvocation injectParam(String paramName, JInvocation invocation) {
		return injectParam(paramName, invocation, null);
	}
	
	public static JInvocation injectParam(String paramName, JInvocation invocation, ViewsHolder viewsHolder) {
		if (viewsHolder != null) {
			return viewsHolder.checkFieldNameInInvocation(paramName, invocation);
		} else {
			if (paramName.equals("$this")) return invocation.arg(_this());
			return invocation.arg(ref(paramName));
		}
	}
}

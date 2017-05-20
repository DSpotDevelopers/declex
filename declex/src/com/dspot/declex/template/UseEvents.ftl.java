<#--

    Copyright (C) 2016-2017 DSpot Sp. z o.o

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed To in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.

-->
<@class_head>
package com.dspot.declex.eventbus;

import org.greenrobot.eventbus.EventBus;

public class Event  {
</@class_head>
<@class_fields>
	private Object[] values;
</@class_fields>


	//============================================================
	//						@UseEvents
	//============================================================

	public ${className}() {
		values = new Object[]{};
	}
	
	public ${className}(Object ... values) {
		this.values = values;
	}
	
	public Object[] getValues() {
		return values;
	}
	
	public ${className} setValues(Object ... values) {
		this.values = values;
		return this;
	}
	
	public static void post(Object ... values) {
		EventBus.getDefault().post(new ${className}(values));
	}
	
	public static void post() {
		EventBus.getDefault().post(new ${className}());
	}
	
	public static ${className} create() {
		return new ${className}();
	}
	
	public void postEvent() {
		EventBus.getDefault().post(this);
	}
	
<@class_footer>
}
</@class_footer>
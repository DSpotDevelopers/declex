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

import com.dspot.declex.api.action.runnable.OnFailedRunnable;

public class Event  {
</@class_head>
<@class_fields>
	private Runnable nextListener;
	private OnFailedRunnable failedListener; 
</@class_fields>


	//============================================================
	//						@UseEvents
	//============================================================

	public ${className}() {
	}
		
	public Runnable getNextListener() {
		return nextListener;
	}
	
	public void setNextListener(Runnable nextListener) {
		this.nextListener = nextListener;
	}
	
	public OnFailedRunnable getFailedListener() {
		return failedListener;
	}
	
	public void setFailedListener(OnFailedRunnable failedListener) {
		this.failedListener = failedListener;
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
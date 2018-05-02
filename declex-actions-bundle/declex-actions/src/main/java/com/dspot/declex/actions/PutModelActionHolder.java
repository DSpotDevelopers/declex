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
package com.dspot.declex.actions;

import com.dspot.actions.processors.PutModelActionProcessor;
import com.dspot.declex.actions.base.BaseModelActionHolder;
import com.dspot.declex.annotation.action.ActionFor;
import com.dspot.declex.annotation.action.Field;

/**
 * An Action to put a {@link com.dspot.declex.annotation.Model @Model} 
 * annotated field. The Action will force a Recollect if the field is also 
 * annotated with {@link com.dspot.declex.annotation.Recollect @Recollect}.
 * 
 * <br><br>
 * The model will be put in background by default. If it was specified 
 * {@code "asyncPut=false"} then  it is directly loaded in the calling thread 
 * where the action is executed. 
 * 
 * <br><br>
 * By default, after the Action executes, it returns 
 * to the main thread, so, it can be savely assigned values to View components.
 * This applies to {@link com.dspot.declex.actions.Action.$PutModel#Done Done} and
 * {@link com.dspot.declex.actions.Action.$PutModel#Failed Failed} Action selectors).
 * To avoid this behavior use {@link com.dspot.declex.actions.Action.$PutModel#keepCallingThread keepCallingThread()}.
 * 
 * @see com.dspot.declex.actions.Action.$LoadModel $LoadModel
 * @see com.dspot.declex.actions.Action.$Populate $Populate
 * @see com.dspot.declex.actions.Action.$Recollect $Recollect
 */

@ActionFor(value="PutModel", processors=PutModelActionProcessor.class)
public class PutModelActionHolder extends BaseModelActionHolder {

	/**
	 *@param field The field annotated with {@link com.dspot.declex.annotation.Model @Model}.
	 */
	@Override
	protected void init(@Field Object field) {
		args.put("recollect", true);
		super.init(field);
	}
	
    /**
     * No recollect the Model after it is loaded
     */
    public PutModelActionHolder noRecollect() {
    	args.put("recollect", false);
    	return this;
    }
 
}
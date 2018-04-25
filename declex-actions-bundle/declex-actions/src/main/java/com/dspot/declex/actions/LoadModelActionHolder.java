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

import com.dspot.actions.processors.LoadModelActionProcessor;
import com.dspot.declex.actions.base.BaseModelActionHolder;
import com.dspot.declex.annotation.action.ActionFor;
import com.dspot.declex.annotation.action.Field;

/**
 * An Action to load a {@link com.dspot.declex.annotation.Model @Model} 
 * annotated field. The Action will force a Populate if the field is also 
 * annotated with {@link com.dspot.declex.annotation.Populate @Populate}.
 * 
 * <br><br>
 * The model will be loaded in background if it was specified {@code "async=true"},
 * otherwise, it is directly loaded in the calling thread where the action
 * is executed. 
 * 
 * <br><br>
 * By default, after the Action executes, it returns to the main
 * thread, so, it can be savely assigned values to View components (This applies to 
 * {@link com.dspot.declex.actions.Action.$LoadModel#Done Done} and
 * {@link com.dspot.declex.actions.Action.$LoadModel#Failed Failed} Action selectors).
 * To avoid this behavior use {@link com.dspot.declex.actions.Action.$LoadModel#keepCallingThread keepCallingThread()}.
 * 
 * @see com.dspot.declex.actions.Action.$PutModel $PutModel
 * @see com.dspot.declex.actions.Action.$Populate $Populate
 * @see com.dspot.declex.actions.Action.$Recollect $Recollect
 */

@ActionFor(value="LoadModel", processors=LoadModelActionProcessor.class)
public class LoadModelActionHolder extends BaseModelActionHolder {

	/**
	 *@param field The field annotated with {@link com.dspot.declex.annotation.Model @Model}.
	 */
	@Override
	protected void init(@Field Object field) {
		args.put("populate", true);
		super.init(field);
	}
	
    /**
     * No populate the Model after it is loaded
     */
    public LoadModelActionHolder noPopulate() {
    	args.put("populate", false);
    	return this;
    }
        
}
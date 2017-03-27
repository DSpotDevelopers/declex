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
package com.dspot.declex.api.action.builtin;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.builtin.base.BaseModelActionHolder;
import com.dspot.declex.api.action.processor.PutModelActionProcessor;

/**
 * An Action to put a {@link com.dspot.declex.api.model.Model @Model} 
 * annotated field. The Action will force a Recollect if the field is also 
 * annotated with {@link com.dspot.declex.api.viewsinjection.Recollect @Recollect}.
 * 
 * <br><br>
 * The model will be put in background by default. If it was specified 
 * {@code "asyncPut=false"} then  it is directly loaded in the calling thread 
 * where the action is executed. 
 * 
 * <br><br>
 * By default, after the Action executes, it returns 
 * to the main thread, so, it can be savely assigned values to View components.
 * This applies to {@link com.dspot.declex.Action.$PutModel#AfterPut AfterPut} and 
 * {@link com.dspot.declex.Action.$PutModel#Failed Failed} Action selectors). 
 * To avoid this behavior use {@link keepCallingThread()}.
 * 
 * @see com.dspot.declex.Action.$LoadModel $LoadModel
 * @see com.dspot.declex.Action.$Populate $Populate
 * @see com.dspot.declex.Action.$Recollect $Recollect
 */

@ActionFor(value="PutModel", processors=PutModelActionProcessor.class)
public class PutModelActionHolder extends BaseModelActionHolder {
    
    /**
     * No recollect the Model after it is loaded
     */
    public PutModelActionHolder noRecollect() {
    	return this;
    }
 
}
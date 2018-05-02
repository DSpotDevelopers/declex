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

import com.dspot.actions.processors.RecollectActionProcessor;
import com.dspot.declex.actions.base.BaseFieldActionHolder;
import com.dspot.declex.annotation.action.ActionFor;
import com.dspot.declex.annotation.action.Field;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;

/**
 * An Action to recollect a {@link com.dspot.declex.annotation.Recollect @Recollect} 
 * annotated field.
 * 
 * <br><br>
 * The recollection process will link the user interface (layout) with the field, reading all
 * the matching "ids" Views in the layout into the fields and methods in the Model. 
 * 
 * <br><br>
 * <b>More Info in </b><a href="https://github.com/smaugho/declex/wiki/Recollecting%20Data">Recollecting Data</a>
 *
 * @see com.dspot.declex.actions.Action.$Populate $Populate
 * @see com.dspot.declex.actions.Action.$LoadModel $LoadModel
 * @see com.dspot.declex.actions.Action.$PutModel $PutModel
 */

@ActionFor(value="Recollect", processors=RecollectActionProcessor.class, timeConsuming = false)
public class RecollectActionHolder extends BaseFieldActionHolder {
	
	/**
	 *@param field The field annotated with {@link com.dspot.declex.annotation.Recollect @Recollect}.
	 */
	@Override
    protected void init(@Field(ignoreExpression="this") Object object) {
    	super.init(object);
    }

    /**
     * @param Done <i><b>(default)</b></i> It will be executed after the 
     * {@link com.dspot.declex.annotation.Recollect @Recollect}  annotated
     * field is used to recollect from the user interface
     * 
     * @param Failed It will be executed if the 
     * {@link com.dspot.declex.annotation.Recollect @Recollect}  
     * annotated field fails recollecting.
     */
	@Override
    protected void build(Runnable Done, OnFailedRunnable Failed) {
    	super.build(Done, Failed);
    }
}
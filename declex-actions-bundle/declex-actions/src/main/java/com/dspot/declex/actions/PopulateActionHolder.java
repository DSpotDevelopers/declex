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

import com.dspot.actions.processors.PopulateActionProcessor;
import com.dspot.declex.actions.base.BaseFieldActionHolder;
import com.dspot.declex.annotation.action.ActionFor;
import com.dspot.declex.annotation.action.Field;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;


/**
 * An Action to populate a {@link com.dspot.declex.annotation.Populate @Populate} 
 * annotated field.
 * 
 * <br><br>
 * The population process will link the user interface (layout) with the field, assigning all
 * the matching "ids" in the layout with the fields and methods in the Model. 
 * 
 * It can be used to populate from Strings and Lists also.
 * 
 * <br><br>
 * <b>More Info in </b><a href="https://github.com/smaugho/declex/wiki/Populating%20Views">Populating Views</a>
 * 
 * @see com.dspot.declex.actions.Action.$Recollect $Recollect
 * @see com.dspot.declex.actions.Action.$LoadModel $LoadModel
 * @see com.dspot.declex.actions.Action.$PutModel $PutModel
 */

@ActionFor(value="Populate", processors=PopulateActionProcessor.class, timeConsuming = false)
public class PopulateActionHolder extends BaseFieldActionHolder {
	/**
	 *@param field The field annotated with {@link com.dspot.declex.annotation.Populate @Populate}.
	 */
	@Override
    protected void init(@Field(ignoreExpression="this") Object field) {
    	super.init(field);
    }

    /**
     * @param Done <i><b>(default)</b></i> It will be executed after the 
     * {@link com.dspot.declex.annotation.Populate @Populate}  annotated
     * field is used to populate the user interface
     * 
     * @param Failed It will be executed if the 
     * {@link com.dspot.declex.annotation.Populate @Populate}  
     * annotated field fails populating.
     */
	@Override
    protected void build(Runnable Done, OnFailedRunnable Failed) {
    	super.build(Done, Failed);
    }
}
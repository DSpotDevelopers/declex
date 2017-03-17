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
package com.dspot.declex.api.action;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Actions are a powerful way to describe what to do after a specific action occurs.
 * Typically, actions are bind to UI components, but they can be bind to events
 * or methods. 
 * 
 * If you have an already built layout where you declare a button, for instance, to Login with Facebook
 * and after this login is done, you want the MainActivity to be opened, this is easily archieve with the following 
 * declarative instruction:<br>
 * <code>
 * @Action
 * S2<FacebookSDKLogin, MainActivity> btnFacebookLogin;
 * </code>
 * <br>
 * here, "btnFacebookLogin" is the id of the button in the layout.
 * <br>
 * 
 * for UIActions, in the declaration name you can use postfix to select a specific action over the View...
 * By default, it's assumed that the action will be executed after the user clicks the View. But you can use the following
 * postfixes to indicate different kind of actions:
 * <pre>
 * -Click, Press, Clicked or Pressed
 * -LongClick, LongPress, LongClicked or LongPressed
 * 
 * -ItemClick, ItemPress, ItemClicked, or ItemPressed
 * -ItemLongClick, ItemLongPress, ItemLongClicked or ItemLongPressed
 * </pre>
 * all these postfixes are valid. For instance:
 * 
 * @Action("user=model")
 * MainActivity gridViewItemClicked;
 * 
 * In this case we also pass a parameter. In this case, MainActivity should accept an extra parameter named "user". 
 * "model" references the default model of the gridView... This is used because typically GridViews (or any other AdapterView) 
 * are used together a model to populate the View Items. 
 * 
 * Each action has its specific parameters, but some of them are globals. 
 * <pre>
 *  ;instruction;	This executes the "instruction" code before the Action
 *  instruction; 	Executes the "instruction" code after the Action.
 * </pre>
 *
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Action {
	String[] value() default "";
	
	boolean debug() default false;
}

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
package com.dspot.declex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to inject Models (Classes annotated with 
 * {@link com.dspot.declex.annotation.UseModel @UseModel}).
 * 
 * <br><br>
 * The injection process loads the models from different means (Ex. DB or Network), depending
 * of the description provided to the model itself when it was declared. It is automatically
 * executed when the Enhanced Component is loaded (at least that {@link lazy()} be set to true, 
 * or {@link com.dspot.declex.annotation.LoadOnEvent @LoadOnEvent}}) be provided).
 * This process can be triggered with the action {@link com.dspot.declex.Action.$LoadModel $LoadModel}.
 * It can be also triggered with {@link com.dspot.declex.annotation.UpdateOnEvent @UpdateOnEvent}
 * or {@link com.dspot.declex.annotation.PutOnEvent @PutOnEvent}
 * 
 * <br><br>
 * Models can be also "put". This mechanism permits to store or send the model
 * through different interfaces (Ex. DB or Network). This mechanism can be triggered
 * with the action {@link com.dspot.declex.Action.$PutModel $PutModel}. 
 * It can be also triggered with {@link com.dspot.declex.annotation.PutOnEvent @PutOnEvent}
 * or {@link com.dspot.declex.annotation.PutOnAction @PutOnAction}.
 * 
 * <br><br>
 * <b>Read more in the documentation: <a href="https://github.com/smaugho/declex/wiki/Models">Models</a></b>
 *
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Model {
	/**
	 * The query to fetch the Model 
	 */
	String query() default "";
	
	/**
	 * The orderBy parameter of the fetch
	 */
	String orderBy() default "";
	
	/**
	 * The fields parameter of the fetch
	 */
	String fields() default "";
	
	/**
	 * Determines if this model is going to be loaded asynchronously. By default is false.
	 */
	boolean async() default false;
	
	/**
	 * Determines if this model is going to be put asynchronously. By default is true.
	 */
	boolean asyncPut() default true;
	
	/**
	 * Determines if load this @Model when it is requested, lazily. 
	 * This is permitted only when declared inside another model object (Annotated with @UseModel) 
	 */
	boolean lazy() default false;	
	
	/**
	 * If not default handler is provided, any exception will be handled by the framework.
	 * Current behavior: nothing would be reported.
	 */
	boolean handleExceptions() default true;	
}

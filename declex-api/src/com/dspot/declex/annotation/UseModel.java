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

import com.dspot.declex.annotation.modifier.ModelParams;

/**
 * This annotation is used to prepare a Model for injection. 
 * 
 * <br><br>
 * <b>Read more in the documentation <a href="https://github.com/smaugho/declex/wiki/Models">Models</a></b>
 *
 */
@ModelParams(value = {})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface UseModel {
	
	/**
	 * When "query" is not specified
	 */
	String defaultQuery() default "";
	
	boolean custom() default false;
	boolean debug() default false;
}

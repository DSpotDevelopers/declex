/**
 * Copyright (C) 2016 DSpot Sp. z o.o
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
package com.dspot.declex.api.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.dspot.declex.api.server.ServerRequest.RequestType;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface ServerModel {
	String baseUrl() default "";
	
	String get() default "";
	String post() default "";
	
	String[] getHeaders() default "";
	String[] postHeaders() default "";
	
	RequestType postType() default RequestType.Body;
	String[] postFields() default "";
	
	String model() default "";
	Class<?> modelClass() default Object.class;
	
	boolean mock() default false;
	String mockResult() default "";
	
	boolean avoidExceptions() default true;
	
	String defaultQuery() default "";
	
	boolean custom() default false;
	
	ServerRequest[] load() default @ServerRequest(action="NONE");
	ServerRequest[] put() default @ServerRequest(action="NONE");
}

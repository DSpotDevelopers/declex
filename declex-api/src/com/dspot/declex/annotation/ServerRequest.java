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

public @interface ServerRequest {
	String name() default "";
	
	RequestMethod method() default RequestMethod.Default;
	RequestType type() default RequestType.Default;
	
	String action();
	
	String[] headers() default {};
	String fields() default "";
	
	String model() default "";
	Class<?> modelClass() default Object.class;
	
	boolean mock() default false;
	String mockResult() default "";
	
	public enum RequestMethod {Default, Delete, Get, Head, Post, Put, Patch}
	public enum RequestType {Default, Json, Form}
}

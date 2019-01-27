/**
 * Copyright (C) 2016-2019 DSpot Sp. z o.o
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
package com.dspot.declex.architecture.annotation.listener;

import org.androidannotations.annotations.ResId;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Listen(listeners = "OnLongClickListener -> return true;", validEndings = {"LongClick", "LongClicked", "LongTap", "LongTapped"})
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface ListenLongClick {

    /**
     * The R.id.* fields which refer to the Views.
     *
     * @return the ids of the Views
     */
    int[] value() default ResId.DEFAULT_VALUE;

    /**
     * The resource names as strings which refer to the Views.
     *
     * @return the resource names of the Views
     */
    String[] resName() default {};

}

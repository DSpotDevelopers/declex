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
package com.dspot.declex.test.model;

import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.UseModel;
import com.dspot.declex.test.model.usemodel.model.ModelUser;
import com.dspot.declex.test.model.usemodel.model.ModelUser_;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

import java.util.List;

@EBean
public class ModelBean {

    @Bean
    ModelUser beanUser;

    @Model
    ModelUser user;

    @Model(lazy = true)
    ModelUser lazyUser;

    @Model(async = true)
    ModelUser asyncUser;

    @Model(asyncPut = false)
    ModelUser asyncPutUser;

    @Model(handleExceptions = false)
    ModelUser wihtoutExceptionUser;

    @UseModel
    ModelUser forcedUseModelUser;

    @Model
    ModelUser_ enhancedUser;

    @Model
    List<ModelUser> userList;

    @Model
    List<ModelUser_> enhancedUserList;

    @UseModel
    List<ModelUser> forcedUseModelUserList;

    @Model(query = "age=45")
    List<ModelUser_> enhancedQueryUserList;

    String yourName = "Lisa";
    @Model(query = "name={yourName}", async = true)
    List<ModelUser_> enhancedQueryAsyncUserList;
}

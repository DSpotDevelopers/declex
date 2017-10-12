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
package com.dspot.declex.test.model.servermodel;

import com.dspot.declex.annotation.Model;
import com.dspot.declex.test.model.servermodel.model.ModelPlaceHolder;
import com.dspot.declex.test.model.servermodel.model.ModelPlaceHolder_;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

import java.util.List;

@EBean
public class ModelBean {

    int post_id = 1;

    @Bean
    ModelPlaceHolder beanModelPlaceHolder;

    /**
     *  List Posts
     * **/
    @Model(async = true)
    List<ModelPlaceHolder> listPosts;

    @Model(async = true)
    List<ModelPlaceHolder_> enhancedListPosts;

    /**
     *  Read Posts
     * **/
    @Model(async = true, query = "{post_id}", orderBy = "read")
    List<ModelPlaceHolder> readPost;

    @Model(async = true, query = "{post_id}", orderBy = "read")
    List<ModelPlaceHolder_> enhancedReadPost;
}

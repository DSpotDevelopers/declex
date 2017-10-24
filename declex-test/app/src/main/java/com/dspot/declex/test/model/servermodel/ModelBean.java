/**
 * Copyright (C) 2016-2017 DSpot Sp. z o.o
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dspot.declex.test.model.servermodel;

import static com.dspot.declex.Action.*;

import com.dspot.declex.annotation.Event;
import com.dspot.declex.annotation.Model;

import com.dspot.declex.test.model.servermodel.model.ModelPlaceHolder;
import com.dspot.declex.test.model.servermodel.model.ModelPlaceHolder_;
import com.dspot.declex.test.util.Calc;

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
    @Model(async = true, orderBy = "list")
    List<ModelPlaceHolder> listPosts;

    @Model(async = true, orderBy = "list")
    List<ModelPlaceHolder_> enhancedListPosts;

    /**
     *  Read Posts
     * **/
    @Model(async = true,  orderBy = "read")
    List<ModelPlaceHolder> readPost;

    @Model(async = true,  orderBy = "read")
    List<ModelPlaceHolder_> enhancedReadPost;

    /**
     *  Posts
     * **/
    @Model(async = true, orderBy = "create")
    ModelPlaceHolder_ post;

    public void downloadListPosts() {
        $LoadModel(listPosts);
    }

    public void downloadEnhancedListPosts() {
        $LoadModel(enhancedListPosts);
    }

    public  void downloadReadPosts() {
        $LoadModel(readPost).query("{post_id}");
    }

    public void downloadEnhancedReadPost() {
        $LoadModel(enhancedReadPost).query("{post_id}");
    }

    public void createPost() {
        $PutModel(post);
        if ($PutModel.Done) {
            $CalculateBasic(4, 5);
        }
    }

    @Event
    void onCalculateBasic(int first, int second) {
        $CalcBasic(0).operation(Calc.SUM).numberFirst(first).numberSecond(second);
        if ($CalcBasic.Done) {
            downloadListPosts();
        }
    }
}

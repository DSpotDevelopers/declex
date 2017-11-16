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
package com.dspot.declex.test.model.service.model;

import com.dspot.declex.annotation.ServerModel;
import com.dspot.declex.annotation.ServerRequest;
import com.dspot.declex.annotation.UseModel;
import com.dspot.declex.test.model.service.ServerModelConfig;

import okhttp3.OkHttpClient;

@ServerModel(
        baseUrl = ServerModelConfig.SERVER,
        getHeaders = "Content-type=application/json; charset=UTF-8",

        load = {
                @ServerRequest(
                        name = "list",
                        method = ServerRequest.RequestMethod.Get,
                        action = "posts"
                )
        },
        put = {
                @ServerRequest(
                        name = "create",
                        method = ServerRequest.RequestMethod.Post,
                        action = "posts",
                        model = "this"
                ),
        }
)

@UseModel
public class ServerModelEntity {
    int id;
    int userId;
    String title;
    String body;

    @ServerModel
    static OkHttpClient okHttpClient = ServerModelConfig.OK_HTTP_CLIENT;
}

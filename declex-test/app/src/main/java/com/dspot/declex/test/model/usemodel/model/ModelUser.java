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
package com.dspot.declex.test.model.usemodel.model;

import com.dspot.declex.annotation.AfterLoad;
import com.dspot.declex.annotation.AfterPut;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.UseModel;
import com.dspot.declex.test.model.usemodel.model.ModelAddress_;

import java.util.List;

@UseModel
public class ModelUser {

    @Model
    static ModelStatic modelStatic;

    String name;
    String email;
    int age;

    boolean special;

    String propertyToSet;
    String propertyToGet;
    String propertyToSetInSubclass;
    String propertyToGetInSubclass;

    ModelAddress_ address;

    List<ModelUser> contacts;

    private String privateField;

    transient String transientField;

    static String staticField;

    public String getPropertyToGet() {
        return propertyToGet;
    }

    public void setPropertyToSet(String propertyToSet) {
        this.propertyToSet = propertyToSet;
    }

    public String getEmail() {
        return  this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @AfterLoad
    void saveLoadData() {
        setEmail("email@after.load");
    }

    @AfterPut
    void savePutData() {
        setEmail("email@after.put");
    }
}

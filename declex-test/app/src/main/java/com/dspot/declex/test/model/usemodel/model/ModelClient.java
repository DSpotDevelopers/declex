/**
 * Copyright (C) 2016-2018 DSpot Sp. z o.o
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

import com.dspot.declex.annotation.UseModel;
import com.dspot.declex.test.model.usemodel.model.ModelAddress_;

@UseModel
public class ModelClient extends ModelUser {

    String business_name;
    String business_email;

    ModelAddress_ business_address;

    public String getPropertyToGetInSubclass() {
        return propertyToGetInSubclass;
    }

    public void setPropertyToSetInSubclass(String propertyToSetInSubclass) {
        this.propertyToSetInSubclass = propertyToSetInSubclass;
    }

}

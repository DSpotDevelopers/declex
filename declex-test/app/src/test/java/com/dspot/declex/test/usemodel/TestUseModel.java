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
package com.dspot.declex.test.usemodel;

import com.dspot.declex.test.action.ActionMainActivityActionHolder_;
import com.dspot.declex.test.action.ActionMainFragmentActionHolder_;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ActionMainActivityActionHolder_.class, ActionMainFragmentActionHolder_.class})
public class TestUseModel {

    @Test
    public void testConstructorsWithExists() {

        //Models created through direct constructors should "exists"
        //This is used when the object is loaded from DB or inflated from JSON
        ModelUser_ user = new ModelUser_();
        assertTrue(user.exists());

        //Default constructor for user as a Bean for simple injection
        user = ModelUser_.getInstance_(RuntimeEnvironment.application);
        assertFalse(user.exists());

        //Construct as a Model
        user = ModelUser_.getModel_(RuntimeEnvironment.application, null, null);
        assertFalse(user.exists()); //A model only using UseModel will be always created
    }

}
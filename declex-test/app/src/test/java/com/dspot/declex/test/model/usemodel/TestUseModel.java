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
package com.dspot.declex.test.model.usemodel;

import android.content.Context;

import com.dspot.declex.test.model.usemodel.model.ModelAddress_;
import com.dspot.declex.test.model.usemodel.model.ModelClient_;
import com.dspot.declex.test.model.usemodel.model.ModelUser;
import com.dspot.declex.test.model.usemodel.model.ModelUser_;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

@RunWith(RobolectricTestRunner.class)
@Config(
    manifest = "app/src/main/AndroidManifest.xml",
    sdk = 25
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*" })
@PrepareForTest({ModelUser_.class})
public class TestUseModel {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void testConstructorsWithExists() {
        //Models created through direct constructors should "exists"
        //This is used when the object is loaded from DB or inflated from JSON
        ModelUser_ user = new ModelUser_();
        assertTrue(user.exists());

        //Construct as a Model
        user = ModelUser_.getModel_(RuntimeEnvironment.application, null, null);
        assertFalse(user.exists()); //A model only using UseModel will be always created
    }

    @Test
    public void testCreateBeanInvokesModelCreation() {

        ModelUser_ user = mock(ModelUser_.class);

        //Call to getInstance should call getModel_, this is used to construct as a Bean
        stub(method(ModelUser_.class, "getModel_", Context.class, Map.class, List.class)).toReturn(user);

        {
            //Default constructor for user as a Bean for simple injection
            ModelUser_ bean = ModelUser_.getInstance_(RuntimeEnvironment.application);
            assertEquals(user, bean);
        }
    }

    @Test
    public void testGettersAndSetters() {
        ModelUser_ user = new ModelUser_();

        user.setName("Some Name");
        assertEquals("Some Name", user.getName());

        user.setEmail("email@example.com");
        assertEquals("email@example.com", user.getEmail());

        user.setAge(50);
        assertEquals(50, user.getAge());

        user.setSpecial(true);
        assertEquals(true, user.getSpecial());
        assertEquals(true, user.isSpecial());

        ModelAddress_ address = new ModelAddress_();
        user.setAddress(address);
        assertEquals(address, user.getAddress());

        List<ModelUser> contacts = new ArrayList<>();
        user.setContacts(contacts);
        assertEquals(contacts, user.getContacts());

    }

    @Test
    public void testGettersAndSettersInSubclass() {
        ModelClient_ client = new ModelClient_();

        client.setName("Some Name");
        assertEquals("Some Name", client.getName());

        client.setEmail("email@example.com");
        assertEquals("email@example.com", client.getEmail());

        client.setAge(50);
        assertEquals(50, client.getAge());

        client.setSpecial(true);
        assertEquals(true, client.getSpecial());
        assertEquals(true, client.isSpecial());

        client.setName("Some Name");
        assertEquals("Some Name", client.getName());

        ModelAddress_ address = new ModelAddress_();
        client.setAddress(address);
        assertEquals(address, client.getAddress());

        List<ModelUser> contacts = new ArrayList<>();
        client.setContacts(contacts);
        assertEquals(contacts, client.getContacts());

        client.setBusinessName("Some Business Name");
        assertEquals("Some Business Name", client.getBusinessName());

        client.setBusinessEmail("email_business@example.com");
        assertEquals("email_business@example.com", client.getBusinessEmail());

        ModelAddress_ businessAddress = new ModelAddress_();
        client.setBusinessAddress(businessAddress);
        assertEquals(businessAddress, client.getBusinessAddress());
    }

    @Test
    public void testSerialization() {
        //TODO
    }

    @Test
    public void testSerializationInSubclass() {
        //TODO
    }

    @Test
    public void testGetModelListInjectList() {
        List<ModelUser_> listModelUser = ModelUser_.getModelList_(RuntimeEnvironment.application, null, null);
        assertNotNull(listModelUser);
    }

    @Test
    public void testGettersAndSettersNotStatic() {
        
    }
}
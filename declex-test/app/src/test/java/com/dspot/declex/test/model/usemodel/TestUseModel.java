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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

import static org.hamcrest.Matchers.*;

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
    public void testGetModelListInjectList() {
        List<ModelUser_> listModelUser = ModelUser_.getModelList_(RuntimeEnvironment.application, null, null);
        assertNotNull(listModelUser);
    }

    @Test(expected = NoSuchMethodException.class)
    public void testGettersAndSettersNotPrivate() throws NoSuchMethodException {
        ModelUser_.class.getMethod("getPrivateField");
        ModelUser_.class.getMethod("setPrivateField", String.class);
    }

    @Test(expected = NoSuchMethodException.class)
    public void testGettersAndSettersNotTransient() throws NoSuchMethodException {
        ModelUser_.class.getMethod("getTransientField");
        ModelUser_.class.getMethod("setTransientField", String.class);
    }

    @Test(expected = NoSuchMethodException.class)
    public void testGettersAndSettersNotStatic() throws NoSuchMethodException {
        ModelUser_.class.getMethod("getStaticField");
        ModelUser_.class.getMethod("setStaticField", String.class);
    }

    @Test(expected = NoSuchMethodException.class)
    public void testGetterCreatedManually()  throws NoSuchMethodException {
        ModelUser_.class.getDeclaredMethod("getEmail");
    }

    @Test
    public void testSetterCreatedManually()  throws NoSuchMethodException {
        ModelUser_.class.getDeclaredMethod("setEmail", String.class);
    }

    @Test
    public void testSerializableEnhancedClass() throws IOException, ClassNotFoundException {
        ModelUser_ userSerialize = ModelUser_.getInstance_(RuntimeEnvironment.application);

        // Save the attributes in the object
        userSerialize.setName("Some Name");
        userSerialize.setEmail("email@example.com");
        userSerialize.setAge(50);
        userSerialize.setSpecial(true);

        List<ModelUser> contacts = new ArrayList<>();
        userSerialize.setContacts(contacts);

        {
            // Serialize ModelUser class [implement writeObject]
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream (byteOutputStream);
            objectOutputStream.writeObject(userSerialize);
            objectOutputStream.close();

            // Deserialize ModelUser class [implement readObject]
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream);
            ModelUser_ userDeserialize = (ModelUser_) objectInputStream.readObject();
            objectInputStream.close();

            // Check result test
            assertThat(byteOutputStream.size(), greaterThan(0));
            assertNotNull(userDeserialize);

            // Check class model
            assertEquals("Some Name", userDeserialize.getName());
            assertEquals("email@example.com", userDeserialize.getEmail());

            assertThat(userDeserialize.getAge(), greaterThan(40));
            assertEquals(50, userDeserialize.getAge());

            assertEquals(true, userDeserialize.getSpecial());
            assertEquals(true, userDeserialize.isSpecial());

            assertEquals(contacts, userDeserialize.getContacts());
        }
    }

    /**
     * Declex does not serialize the fields that are objects
     * **/
    @Test
    public void testSerializableEnhancedSubClass() throws IOException, ClassNotFoundException {
        ModelUser_ userSerialize = ModelUser_.getInstance_(RuntimeEnvironment.application);

        ModelAddress_ address = new ModelAddress_();
        userSerialize.setAddress(address);

        {
            // Serialize ModelUser class [implement writeObject]
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream (byteOutputStream);
            objectOutputStream.writeObject(userSerialize);
            objectOutputStream.close();

            // Deserialize ModelUser class [implement readObject]
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream);
            ModelUser_ userDeserialize = (ModelUser_) objectInputStream.readObject();
            objectInputStream.close();

            // Check class model
            // Declex does not serialize the fields that are objects
            assertNull(userDeserialize.getAddress());
            assertNotEquals(address, userDeserialize.getAddress());
        }
    }

    @Test
    public void testAfterLoad() {
        ModelUser_ user = ModelUser_.getModel_(RuntimeEnvironment.application, null, null);
        Map<String, Object> args = new HashMap<String, Object>();
        user.modelInit_(args);
        assertEquals("email@after.load", user.getEmail());
    }

    @Test
    public void testAfterPut() {
        ModelUser_ user = ModelUser_.getModel_(RuntimeEnvironment.application, null, null);
        Map<String, Object> args = new HashMap<String, Object>();
        user.putModel_(args);
        assertEquals("email@after.put", user.getEmail());
    }
}
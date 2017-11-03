package com.dspot.declex.test.model.jsonmodel.model;

import com.dspot.declex.test.model.usemodel.model.ModelAddress_;

import static org.hamcrest.Matchers.*;
import org.junit.Before;
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

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*"})
@PrepareForTest({ModelSocialWorker_.class})
public class TestJsonModel {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testConstructorsWithExists() {
        //Models created through direct constructors should "exists"
        //This is used when the object is loaded from DB or inflated from JSON
        ModelSocialWorker_ user = new ModelSocialWorker_();
        assertTrue(user.exists());

        //Construct as a Model
        user = ModelSocialWorker_.getModel_(RuntimeEnvironment.application, null, null);
        assertFalse(user.exists()); //A model only using UseModel will be always created
    }

    @Test
    public void testGettersAndSetters() {
        ModelSocialWorker_ socialWorker = new ModelSocialWorker_();

        socialWorker.setProfessionalFunctions("Supervision");
        assertEquals("Supervision", socialWorker.getProfessionalFunctions());

        socialWorker.setWorkPlace("Schools");
        assertEquals("Schools", socialWorker.getWorkPlace());

        socialWorker.setStudyUniversity(true);
        assertEquals(true, socialWorker.getStudyUniversity());
        assertEquals(true, socialWorker.isStudyUniversity());

        ModelAddress_ address = new ModelAddress_();
        socialWorker.setAddress(address);
        assertEquals(address, socialWorker.getAddress());
    }

    @Test
    public void testSerializableEnhancedClass() throws IOException, ClassNotFoundException {
        ModelSocialWorker_ workerSocialSerialize = ModelSocialWorker_.getInstance_(RuntimeEnvironment.application);

        // Save the attributes in the object
        workerSocialSerialize.setWorkPlace("Schools");
        workerSocialSerialize.setProfessionalFunctions("Supervision");
        workerSocialSerialize.setStudyUniversity(true);

        {
            // Serialize ModelUser class [implement writeObject]
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);
            objectOutputStream.writeObject(workerSocialSerialize);
            objectOutputStream.close();

            // Deserialize ModelUser class [implement readObject]
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream);
            ModelSocialWorker_ workerSocialDeserialize = (ModelSocialWorker_) objectInputStream.readObject();
            objectInputStream.close();

            // Check result test
            assertThat(byteOutputStream.size(), greaterThan(0));
            assertNotNull(workerSocialDeserialize);

            // Check class model
            assertEquals("Supervision", workerSocialDeserialize.getProfessionalFunctions());
            assertEquals("Schools", workerSocialDeserialize.getWorkPlace());
            assertEquals(true, workerSocialDeserialize.getStudyUniversity());
            assertEquals(true, workerSocialDeserialize.isStudyUniversity());
        }
    }
}
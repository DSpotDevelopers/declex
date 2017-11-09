package com.dspot.declex.test.model.jsonmodel.model;

import com.dspot.declex.test.model.usemodel.model.ModelAddress_;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import static org.hamcrest.Matchers.*;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gson.Gson;

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
    public void testConvertToJsonModel() {
        final String modelJson = "{\"workPlace\":\"Schools\",\"professionalFunctions\":\"Supervision\",\"studyUniversity\":true,\"address\":null}";

        ModelSocialWorker_ socialWorker = new ModelSocialWorker_();
        socialWorker.setWorkPlace("Schools");
        socialWorker.setProfessionalFunctions("Supervision");
        socialWorker.setStudyUniversity(true);

        String jsonGenerated = socialWorker.toJson();
        assertEquals(modelJson, jsonGenerated);
    }

    @Test
    public void testConvertToJsonModelSomeAttributes() {
        final String modelJson = "{\"workPlace\":\"Schools\",\"professionalFunctions\":\"Supervision\"}";

        ModelSocialWorker_ socialWorker = new ModelSocialWorker_();
        socialWorker.setWorkPlace("Schools");
        socialWorker.setProfessionalFunctions("Supervision");
        socialWorker.setStudyUniversity(true);

        String jsonGenerated = socialWorker.toJson("workPlace, professionalFunctions");
        assertEquals(modelJson, jsonGenerated);
    }

    @Test
    public void testGetModelFromJsonFormat() {
        final String modelJson = "{\"workPlace\":\"Schools\",\"professionalFunctions\":\"Supervision\",\"studyUniversity\":true,\"address\":null}";
        ModelSocialWorker_ socialWorker = ModelSocialWorker_.fromJson(modelJson);
        assertNotNull(socialWorker);
        assertEquals("Schools", socialWorker.getWorkPlace());
        assertEquals("Supervision", socialWorker.getProfessionalFunctions());
        assertTrue(socialWorker.getStudyUniversity());
        assertTrue(socialWorker.isStudyUniversity());
        assertNull(socialWorker.getAddress());
    }

    @Test
    public void testGetModelFromJsonElement() {
        final JsonObject elementJson = new JsonObject();
        elementJson.addProperty("workPlace", "Schools");
        elementJson.addProperty("professionalFunctions", "Supervision");
        elementJson.addProperty("studyUniversity", true);

        ModelSocialWorker_ socialWorker = ModelSocialWorker_.fromJson(elementJson);
        assertNotNull(socialWorker);
        assertEquals("Schools", socialWorker.getWorkPlace());
        assertEquals("Supervision", socialWorker.getProfessionalFunctions());
        assertTrue(socialWorker.getStudyUniversity());
        assertTrue(socialWorker.isStudyUniversity());
    }

    @Test
    public void testListModelFromJsonFormat() {
        ModelSocialWorker_ socialWorker = new ModelSocialWorker_();
        socialWorker.setWorkPlace("Schools");
        socialWorker.setProfessionalFunctions("Supervision");
        socialWorker.setStudyUniversity(true);

        List<ModelSocialWorker_> listModelSocialWorker = new ArrayList<>();
        listModelSocialWorker.add(socialWorker);

        // [{"workPlace":"Schools","professionalFunctions":"Supervision","studyUniversity":true}]
        String jsonGenerated = new Gson().toJson(listModelSocialWorker);
        listModelSocialWorker = ModelSocialWorker_.listFromJson(jsonGenerated);
        assertNotNull(listModelSocialWorker);
        assertEquals("Schools", listModelSocialWorker.get(0).getWorkPlace());
        assertEquals("Supervision", listModelSocialWorker.get(0).getProfessionalFunctions());
        assertTrue(listModelSocialWorker.get(0).getStudyUniversity());
        assertTrue(listModelSocialWorker.get(0).isStudyUniversity());
    }

    @Test
    public void testListModelFromJsonElement() {
        final JsonObject elementJson = new JsonObject();
        elementJson.addProperty("workPlace", "Schools");
        elementJson.addProperty("professionalFunctions", "Supervision");
        elementJson.addProperty("studyUniversity", true);

        final JsonArray arrayElementJson = new JsonArray();
        arrayElementJson.add(elementJson);

        List<ModelSocialWorker_> listModelSocialWorker = ModelSocialWorker_.listFromJson(arrayElementJson);
        assertNotNull(listModelSocialWorker);
        assertEquals("Schools", listModelSocialWorker.get(0).getWorkPlace());
        assertEquals("Supervision", listModelSocialWorker.get(0).getProfessionalFunctions());
        assertTrue(listModelSocialWorker.get(0).getStudyUniversity());
        assertTrue(listModelSocialWorker.get(0).isStudyUniversity());
    }
}
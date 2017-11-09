package com.dspot.declex.test.model.localdbmodel;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.dspot.declex.test.model.localdbmodel.model.ModelSocialWorker_;

import static org.hamcrest.Matchers.*;
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

import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*" })
@PrepareForTest({ModelSocialWorker_.class})
public class TestLocalDBModel {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {}

    @Test(expected = NullPointerException.class)
    public void testModelDBExists() {
        ModelSocialWorker_.getModel_(RuntimeEnvironment.application, null, null);
    }

    @Test
    public void testSelectModelToDBSyntaxSQL() {
        List<ModelSocialWorker_> listSocialWorker = new Select().from(ModelSocialWorker_.class).execute();
        assertNotNull(listSocialWorker);
        assertThat(listSocialWorker.size(), greaterThan(0));
    }

    @Test
    public void testUpdateModelToDB() {
        List<ModelSocialWorker_> listSocialWorker = new Select().from(ModelSocialWorker_.class).execute();
        assertNotNull(listSocialWorker);
        assertThat(listSocialWorker.size(), greaterThan(0));

        ModelSocialWorker_ socialWorker = listSocialWorker.get(0);
        socialWorker.setSalary(635);
        socialWorker.save();
    }

    @Test
    public void testDeleteModelToDB() {
        List<ModelSocialWorker_> listSocialWorker = new Select().from(ModelSocialWorker_.class).execute();
        assertNotNull(listSocialWorker);
        assertThat(listSocialWorker.size(), greaterThan(0));

        ModelSocialWorker_ socialWorker = listSocialWorker.get(0);
        socialWorker.delete();
    }

    @Test
    public void testDeleteModelToDBSyntaxSQL() {
        List<ModelSocialWorker_> listSocialWorker = new Select().from(ModelSocialWorker_.class).execute();
        assertNotNull(listSocialWorker);
        assertThat(listSocialWorker.size(), greaterThan(0));

        new Delete().from(ModelSocialWorker_.class).where("Id=", listSocialWorker.get(0).getId());
    }
}
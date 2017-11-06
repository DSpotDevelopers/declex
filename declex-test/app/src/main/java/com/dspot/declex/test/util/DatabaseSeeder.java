package com.dspot.declex.test.util;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;

import com.activeandroid.query.Select;
import static com.dspot.declex.actions.Action.*;

import com.dspot.declex.test.model.localdbmodel.model.ModelSocialWorker_;

@EBean
public class DatabaseSeeder {
    @AfterInject
    void seed() {
        $Background();
        createMockDataSocialWorker();
    }

    void createMockDataSocialWorker() {
        //Don't create mock data, if it already exists.
        if (new Select().from(ModelSocialWorker_.class).exists()) return;

        for (int j = 0; j < 5; j++) {
            ModelSocialWorker_ socialWorker = new ModelSocialWorker_();
            socialWorker.setWorkPlace("( " + j + " ) " + "_Schools");
            socialWorker.setProfessionalFunctions("( " + j + " ) " + "_Supervision");
            socialWorker.setStudyUniversity(true);
            socialWorker.save();
        }
    }
}

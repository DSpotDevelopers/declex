package com.dspot.declex.test.util;

import com.dspot.declex.test.model.localdbmodel.model.ModelSocialWorker_;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;

import static com.dspot.declex.actions.Action.*;


@EBean
public class DatabaseSeeder {
    @AfterInject
    void seed() {
        $Background();
        createMockDataSocialWorkers();
    }

    void createMockDataSocialWorkers() {
        for (int i = 0; i < 5; i++) {
            ModelSocialWorker_  modelSocialWorker = new ModelSocialWorker_();
            modelSocialWorker.setWorkPlace("(" + i + ")_Schools");
            modelSocialWorker.setProfessionalFunctions("(" + i + ")_Supervision");
            modelSocialWorker.setStudyUniversity(true);
            modelSocialWorker.setSalary(532);
            modelSocialWorker.save();
        }
    }
}

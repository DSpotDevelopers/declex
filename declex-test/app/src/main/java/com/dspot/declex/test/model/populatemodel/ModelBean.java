package com.dspot.declex.test.model.populatemodel;

import static com.dspot.declex.actions.Action.*;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.Populate;
import com.dspot.declex.annotation.Recollect;
import com.dspot.declex.test.model.service.model.ServerModelEntity_;

import org.androidannotations.annotations.EBean;

@EBean
public class ModelBean {
    @Model(query = "id=1")
    @Populate
    ServerModelEntity_ populateModel;

    @Model(query = "id=1")
    @Populate
    @Recollect
    ServerModelEntity_ bothModel;

    public void click_saveModel() {
        populateModel = new ServerModelEntity_();
        populateModel.setTitle("Testing");
        populateModel.setBody("Testing Request");
        $Populate(populateModel);
    }

    public void click_deleteModel() {
        String title = "Testing both";

        $Recollect(bothModel);
        if(bothModel != null) {
            bothModel.setTitle(title);
            $Populate(bothModel);
        }
    }
}

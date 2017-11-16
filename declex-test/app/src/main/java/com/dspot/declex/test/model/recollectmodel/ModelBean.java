package com.dspot.declex.test.model.recollectmodel;

import static com.dspot.declex.actions.Action.*;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.Recollect;
import com.dspot.declex.test.model.service.model.ServerModelEntity_;


import org.androidannotations.annotations.EBean;

@EBean
public class ModelBean {
    @Model
    @Recollect
    ServerModelEntity_ recollectModel;

    @Model
    @Recollect(validate = true)
    ServerModelEntity_ validateModel;

    public void click_createPosts(Runnable Failed) {
        if(recollectModel == null) {
            recollectModel = new ServerModelEntity_();
            recollectModel.setTitle("Testing");
            recollectModel.setBody("Testing Nothing");
        }

        $Recollect(recollectModel);
        if($Recollect.Failed) {
            if(Failed != null) Failed.run();
        }
    }
}

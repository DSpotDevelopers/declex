package com.dspot.declex.test.thread;

import static com.dspot.declex.actions.Action.*;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.test.thread.service.model.ServerModelEntity_;

import org.androidannotations.annotations.EBean;

@EBean
public class Background {
    @Model
    ServerModelEntity_ model;

    public void createPosts() {
        $Background();
        $PutModel(model).orderBy("create");
    }

    public void detailsPosts(Runnable Done, Runnable Failed) {
        {
            $Background();
            $LoadModel(model);
            if($LoadModel.Failed) {
                if(Failed != null) Failed.run();
            }
            if(Done != null) Done.run();
        }
    }

    public void actionsOnlyBackground(Runnable Done, Runnable Failed) {
        $Background();
        {
            $PutModel(model);
            if($PutModel.Failed) {
                if(Failed != null) Failed.run();
            }

            $LoadModel(model);
            if($LoadModel.Done) {
                if(Done != null) Done.run();
            }
        }
    }
}

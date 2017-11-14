package com.dspot.declex.test.model.putmodel;

import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.ServerModel;
import com.dspot.declex.test.model.putmodel.model.ServerModelEntity_;

import org.androidannotations.annotations.EBean;

import static com.dspot.declex.actions.Action.$PutModel;

@EBean
public class ModelBean {

    @Model(lazy = true, asyncPut = false, orderBy = "create")
    ServerModelEntity_ createModelEntity;

    @Model(lazy = true, asyncPut = false, orderBy = "update")
    ServerModelEntity_ updateModelEntity;

    // Query
    public void createModelQuery(Runnable Done, Runnable Failed) {
        createModelEntity = new ServerModelEntity_();
        createModelEntity.setTitle("Testing");
        createModelEntity.setBody("Testing Put Operations");
        createModelEntity.setUserId(1);

        $PutModel(createModelEntity);
        if ($PutModel.Failed) {
            if (Failed != null) Failed.run();
        }
        if (Done != null) Done.run();
    }

    // Fields
    public void updateModelQuery(Runnable Done, Runnable Failed) {
        updateModelEntity = new ServerModelEntity_();
        updateModelEntity.setId(1);
        updateModelEntity.setTitle("Testing");
        updateModelEntity.setBody("Testing Put Operations");
        updateModelEntity.setUserId(1);

        $PutModel(updateModelEntity).query("1").fields("id, title, body, userId");
        if ($PutModel.Failed) {
            if (Failed != null) Failed.run();
        }
        if (Done != null) Done.run();
    }
}

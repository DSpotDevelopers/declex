package com.dspot.declex.test.model.loadmodel;

import com.dspot.declex.annotation.Model;
import com.dspot.declex.test.model.loadmodel.model.ServerModelEntity_;
import org.androidannotations.annotations.EBean;

import java.util.List;

import static com.dspot.declex.actions.Action.$LoadModel;

@EBean
public class ModelBean {

    @Model
    ServerModelEntity_ modelEntity;

    @Model(lazy = true, async = true, orderBy = "read")
    ServerModelEntity_ readModelEntity;

    @Model(lazy = true, async = true)
    List<ServerModelEntity_> enhancedModelEntityList;

    // Query
    public void loadModelQuery(Runnable Done, Runnable Failed) {
        $LoadModel(modelEntity);
        if($LoadModel.Failed) {
            if(Failed != null) Failed.run();
        }
        if(Done != null) Done.run();
    }

    public void readModelQuery() {
        $LoadModel(readModelEntity).query("1");
        if($LoadModel.Failed) {
            System.out.print("Failed Load Model");
        }
        System.out.print("Load Model Successfully");
    }

    // Fields
    public void loadModelFieldsOrderQuery() {
        $LoadModel(enhancedModelEntityList).query("id=1").fields("userId, title, body").orderBy("userId ASC");
        if($LoadModel.Failed) {
            System.out.print("Failed Load Model");
        }
        System.out.print("Load Model Successfully");
    }
}

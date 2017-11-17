package com.dspot.declex.test.thread;

import static com.dspot.declex.actions.Action.*;

import com.dspot.declex.annotation.Model;
import com.dspot.declex.test.thread.service.model.ServerModelEntity_;

import org.androidannotations.annotations.EBean;

import java.util.List;

@EBean
public class UIThread {
    @Model
    ServerModelEntity_ detailsModel;

    @Model
    List<ServerModelEntity_> ListModel;


    public void sortListModel(Runnable Failed) {
        //This will run the following code in an Asynchronous (Background) Thread
        $Background();
        $LoadModel(ListModel);
        if ($LoadModel.Failed) {
            if (Failed != null) Failed.run();
        }

        //This will run the following code in the Main (UI) Thread
        $UIThread();
        $LoadModel(detailsModel).query("1").orderBy("details");
    }

    public void onlyDetailsModel(Runnable Done, Runnable Failed) {
        $LoadModel(detailsModel).query("1").orderBy("details");
        if ($LoadModel.Failed) {
            if (Failed != null) Failed.run();
        }

        $UIThread();
        $PutModel(detailsModel).orderBy("update").query("1");
        if ($PutModel.Done) {
            if (Done != null) Done.run();
        }
    }
}

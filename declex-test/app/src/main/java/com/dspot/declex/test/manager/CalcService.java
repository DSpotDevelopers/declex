package com.dspot.declex.test.manager;


import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.ServiceAction;
import org.androidannotations.api.support.app.AbstractIntentService;

@EIntentService
public class CalcService extends AbstractIntentService {

    @Bean
    CalcManager calcManager;

    public CalcService() {
        super("CalcService");
    }

    @ServiceAction
    void calculateSumFromService(int first, int second) {
        calcManager.calculateSumVirtual(first, second);
    }

    @ServiceAction
    void calculateSubtFromService(int first, int second) {
        calcManager.calculateSubtVirtual(first, second);
    }
}
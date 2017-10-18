package com.dspot.declex.test.util;

public class Calc {
    private String operation;

    private int numberFirst;

    private int numberSecond;

    private int resultOperation;

    public static  String SUM = "Sum";

    public static  String SUBT = "Subt";

    public Calc() {
        this.operation = null;
        this.numberFirst = 0;
        this.numberSecond = 0;
        this.resultOperation = 0;
    }

    public int Sum() {
        resultOperation = this.numberFirst + this.numberSecond;
        return resultOperation;
    }

    public int Subt() {
        if(this.numberFirst > this.numberSecond)
            resultOperation =  this.numberFirst - this.numberSecond;
        else
            resultOperation =   this.numberSecond - this.numberFirst;
        return resultOperation;
    }

    public int getNumberFirst() {
        return this.numberFirst;
    }

    public int getNumberSecond() {
        return this.numberSecond;
    }

    public int getResultOperation() {
        return this.resultOperation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setNumberFirst(int numberFirst) {
        this.numberFirst = numberFirst;
    }

    public void setNumberSecond(int numberSecond) {
        this.numberSecond = numberSecond;
    }

    public boolean isSum() {
        return this.operation.equals(SUM);
    }

    public boolean isSubt() {
        return this.operation.equals(SUBT);
    }

    public void createOperation() {
        if (isSum()) {
            Sum();
        } else if (isSubt()) {
            Subt();
        }
    }
}

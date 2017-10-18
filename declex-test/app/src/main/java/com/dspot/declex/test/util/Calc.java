package com.dspot.declex.test.util;

public class Calc {
    private int numberFirst;

    private int numberSecond;

    public static  String SUM = "Sum";

    public static  String REST = "Rest";

    public Calc(int numberFirst, int numberSecond) {
        this.numberFirst = numberFirst;
        this.numberSecond = numberSecond;
    }

    public int Sum() {
        return this.numberFirst + this.numberSecond;
    }

    public int Rest() {
        if(this.numberFirst > this.numberSecond)
            return this.numberFirst - this.numberSecond;
        return  this.numberSecond - this.numberFirst;
    }

    public void setNumberFirst(int numberFirst) {
        this.numberFirst = numberFirst;
    }

    public void setNumberSecond(int numberSecond) {
        this.numberSecond = numberSecond;
    }

    public int getNumberFirst() {
        return this.numberFirst;
    }

    public int getNumberSecond() {
        return this.numberSecond;
    }
}

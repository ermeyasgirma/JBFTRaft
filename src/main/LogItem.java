package main;

import java.io.Serializable;

public class LogItem implements Serializable {

    private String msg;
    private int term;

    public LogItem(String m, int t) {
        this.msg = m;
        this.term = t;
    }

    public String getMsg() {
        return msg;
    }

    public int getTerm() {
        return term;
    }

    @Override
    public String toString() {
        return "LogItem - Message: " + msg + ", Term: " + Integer.toString(term) + ". ";
    }
}
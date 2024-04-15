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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogItem logItem = (LogItem) o;
        return term == logItem.term &&
                msg.equals(logItem.msg);
    }
}
package messages;
import java.io.Serializable;

public class ReplicateLog implements Serializable {
    private String msg;

    public ReplicateLog() {
        msg = "";
    }

    public String getMsg() {
        return msg;
    }
}
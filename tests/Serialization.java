package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import main.PreservedData;
import main.LogItem;

public class Serialization {

    private PreservedData pd = new PreservedData();
    
    @Test
    public void testUpdateCurrTerm() {
        pd.updateCurrTerm(34, 0);
        assertEquals(pd.getCurrTerm(), 34);
    }

    @Test
    public void testUpdateCommitLength() {
        pd.updateCommitLength(17, 0);
        assertEquals(pd.getCommitLength(), 17);
    }

    @Test
    public void testUpdateVotedFor() {
        pd.updateVotedFor(54321, 0);
        assertEquals((int) pd.getVotedFor(), 54321);
    }

    @Test
    public void testUpdateLog() {
        List<LogItem> l = new ArrayList<LogItem>();
        l.add(new LogItem("Hello World", 8888));
        pd.updateLog(l, 0);
        assertEquals(pd.getLog(), l);
    }

    @Test
    public void testValuesSavedToFile() {
        pd.updateCommitLength(12, 0);
        pd.updateCurrTerm(39, 0);
        pd.updateVotedFor(43231, 0);
        pd.updateLog(new ArrayList<LogItem>(), 0);
        pd.saveState(0);
        PreservedData pd2 = PreservedData.restoreState(0);
        assertEquals(pd, pd2);
    }

    @Test
    public void testFileCleanup() {
        File f = new File("0.ser");
        f.delete();
        assert(!f.exists());
    }
}

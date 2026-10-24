import java.util.List;

/** Holds the outcome of a risk analysis: score, red flags found and verdict. */
public class AnalysisResult {

    private final int score;
    private final List<String> flags;          // full text, used by the console report
    private final List<String> flagMessages;   // message only, used by the web page
    private final List<Integer> flagPenalties; // penalty of each flag
    private final String verdict;
    private final boolean critical;

    public AnalysisResult(int score, List<String> flags, List<String> flagMessages,
                          List<Integer> flagPenalties, String verdict, boolean critical) {
        this.score = score;
        this.flags = flags;
        this.flagMessages = flagMessages;
        this.flagPenalties = flagPenalties;
        this.verdict = verdict;
        this.critical = critical;
    }

    public int getScore()                   { return score; }
    public List<String> getFlags()          { return flags; }
    public List<String> getFlagMessages()   { return flagMessages; }
    public List<Integer> getFlagPenalties() { return flagPenalties; }
    public String getVerdict()              { return verdict; }
    public boolean isCritical()             { return critical; }

    /** Short level name for the web page: safe, caution, avoid or stolen. */
    public String getLevel() {
        if (critical)     return "stolen";
        if (score >= 80)  return "safe";
        if (score >= 50)  return "caution";
        return "avoid";
    }
}

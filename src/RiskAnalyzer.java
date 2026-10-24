import java.util.ArrayList;
import java.util.List;

/**
 * POLYMORPHISM
 *
 * The analyzer holds a list of RedFlagRule objects. It calls the same
 * methods (isTriggered, getPenalty...) on every rule without knowing which
 * rule it is - each rule behaves in its own way.
 *
 * To add a new red flag, just write a new class and add it to the list.
 * Nothing else in the program changes.
 */
public class RiskAnalyzer {

    private final List<RedFlagRule> rules = new ArrayList<>();

    public RiskAnalyzer() {
        rules.add(new StolenRule());
        rules.add(new FrequentOwnerRule());
        rules.add(new RepeatedRepairRule());
        rules.add(new FakePartRule());
        rules.add(new SellerMismatchRule());
    }

    public AnalysisResult analyze(PhoneHistory history) {
        int score = 100;
        boolean critical = false;
        List<String> flags = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        List<Integer> penalties = new ArrayList<>();

        for (RedFlagRule rule : rules) {
            if (rule.isTriggered(history)) {
                int penalty = rule.getPenalty(history);
                score -= penalty;
                String message = rule.getMessage(history);
                flags.add(message + "  (-" + penalty + ")");
                messages.add(message);
                penalties.add(penalty);
                if (rule.isCritical()) {
                    critical = true;
                }
            }
        }

        score = Math.max(score, 0);
        if (critical) {
            score = 0;
        }
        return new AnalysisResult(score, flags, messages, penalties,
                decideVerdict(score, critical), critical);
    }

    private String decideVerdict(int score, boolean critical) {
        if (critical)      return "DO NOT BUY - REPORTED STOLEN";
        if (score >= 80)   return "SAFE TO BUY";
        if (score >= 50)   return "BUY WITH CAUTION";
        return "AVOID";
    }
}

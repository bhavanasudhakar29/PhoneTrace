/**
 * ABSTRACTION + INHERITANCE
 *
 * Every red flag rule IS-A RedFlagRule. This parent class says WHAT every
 * rule must do (examine a phone's history) but not HOW. Each child class
 * decides how for itself.
 *
 * The rule's name is written once here and inherited by every child.
 */
public abstract class RedFlagRule {

    private final String ruleName;

    protected RedFlagRule(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleName() {
        return ruleName;
    }

    /** Does this rule find a problem in the phone's history? */
    public abstract boolean isTriggered(PhoneHistory history);

    /** How many trust points to deduct when triggered. */
    public abstract int getPenalty(PhoneHistory history);

    /** Human-readable explanation of the problem found. */
    public abstract String getMessage(PhoneHistory history);

    /**
     * Critical rules force the verdict to DO NOT BUY.
     * Most rules are not critical, so the default is false;
     * a child class can OVERRIDE this (see StolenRule).
     */
    public boolean isCritical() {
        return false;
    }
}

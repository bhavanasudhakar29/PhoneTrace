/** RED FLAG: the phone has an ACTIVE theft report. Critical -> DO NOT BUY. */
public class StolenRule extends RedFlagRule {

    public StolenRule() {
        super("Theft check");
    }

    @Override
    public boolean isTriggered(PhoneHistory history) {
        for (TheftReport report : history.getTheftReports()) {
            if (report.isActive()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getPenalty(PhoneHistory history) {
        return 100;
    }

    @Override
    public String getMessage(PhoneHistory history) {
        return "REPORTED STOLEN - an active theft report exists for this IMEI";
    }

    @Override
    public boolean isCritical() {
        return true;      // overrides the parent's default
    }
}

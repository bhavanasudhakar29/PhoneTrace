import java.util.List;

/** Prints the final "detective" report in a clean, readable format. */
public class HistoryReport {

    private static final String LINE = "==============================================================";
    private static final String THIN = "--------------------------------------------------------------";

    public static void print(PhoneHistory h, AnalysisResult result) {
        Phone p = h.getPhone();

        System.out.println();
        System.out.println(LINE);
        System.out.println("                 PHONETRACE HISTORY REPORT");
        System.out.println(LINE);
        System.out.println(" IMEI   : " + p.getImei());
        System.out.println(" Device : " + p.getBrand() + " " + p.getModel());
        System.out.println(THIN);

        List<Ownership> owners = h.getOwnerships();
        System.out.println(" OWNERSHIP HISTORY (" + owners.size() + " owner" + (owners.size() == 1 ? "" : "s") + ")");
        int n = 1;
        for (Ownership o : owners) {
            String to = o.isCurrent() ? "present   [CURRENT]" : o.getToDate().toString();
            System.out.printf("   %d. %-10s %s   %s -> %s%n",
                    n++, o.getOwnerName(), mask(o.getOwnerPhone()), o.getFromDate(), to);
        }
        if (owners.isEmpty()) System.out.println("   No ownership records.");

        System.out.println(THIN);
        System.out.println(" REPAIR HISTORY");
        if (h.getRepairs().isEmpty()) {
            System.out.println("   No repairs recorded.");
        }
        for (Repair r : h.getRepairs()) {
            System.out.println("   " + r);
        }

        System.out.println(THIN);
        System.out.println(" THEFT REPORTS");
        if (h.getTheftReports().isEmpty()) {
            System.out.println("   None.");
        }
        for (TheftReport t : h.getTheftReports()) {
            System.out.println("   " + t);
        }

        System.out.println(THIN);
        System.out.println(" RED FLAGS");
        if (result.getFlags().isEmpty()) {
            System.out.println("   None found. Clean history.");
        }
        for (String flag : result.getFlags()) {
            System.out.println("   [!] " + flag);
        }

        System.out.println(THIN);
        System.out.println(" TRUST SCORE : " + scoreBar(result.getScore()) + " " + result.getScore() + " / 100");
        System.out.println(" VERDICT     : " + result.getVerdict());
        System.out.println(LINE);
    }

    // Shows only the last 4 digits of an owner's number, for privacy
    private static String mask(String phone) {
        return "******" + phone.substring(phone.length() - 4);
    }

    // e.g. score 70 -> [#######---]
    private static String scoreBar(int score) {
        int filled = score / 10;
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? '#' : '-');
        }
        return bar.append(']').toString();
    }
}

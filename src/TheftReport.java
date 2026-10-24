import java.time.LocalDate;

/**
 * MODEL CLASS - one row of the 'theft_reports' table.
 * ENCAPSULATION: status can only be ACTIVE or RECOVERED.
 */
public class TheftReport {

    public static final String ACTIVE    = "ACTIVE";
    public static final String RECOVERED = "RECOVERED";

    private int id;
    private String imei;
    private LocalDate reportDate;
    private String status;

    public TheftReport(int id, String imei, LocalDate reportDate, String status) {
        this.id = id;
        this.imei = imei;
        this.reportDate = reportDate;
        setStatus(status);
    }

    public int getId()               { return id; }
    public String getImei()          { return imei; }
    public LocalDate getReportDate() { return reportDate; }
    public String getStatus()        { return status; }

    public void setStatus(String status) {
        if (!ACTIVE.equals(status) && !RECOVERED.equals(status)) {
            throw new IllegalArgumentException("Status must be ACTIVE or RECOVERED.");
        }
        this.status = status;
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    @Override
    public String toString() {
        return "#" + id + " reported on " + reportDate + " | " + status;
    }
}

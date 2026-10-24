/**
 * Collects a phone's complete history from all tables into one PhoneHistory object.
 */
public class HistoryService {

    private final PhoneDAO phoneDAO = new PhoneDAO();
    private final OwnershipDAO ownershipDAO = new OwnershipDAO();
    private final RepairDAO repairDAO = new RepairDAO();
    private final TheftReportDAO theftDAO = new TheftReportDAO();

    /** Returns null if the IMEI is not registered. */
    public PhoneHistory buildHistory(String imei, String sellerPhone) {
        Phone phone = phoneDAO.getById(imei);
        if (phone == null) {
            return null;
        }
        return new PhoneHistory(
                phone,
                ownershipDAO.getByImei(imei),
                repairDAO.getByImei(imei),
                theftDAO.getByImei(imei),
                sellerPhone);
    }
}

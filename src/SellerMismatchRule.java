/** RED FLAG: the person selling is not the registered current owner. */
public class SellerMismatchRule extends RedFlagRule {

    public SellerMismatchRule() {
        super("Seller verification");
    }

    @Override
    public boolean isTriggered(PhoneHistory history) {
        String seller = history.getSellerPhone();
        Ownership current = history.getCurrentOwner();
        if (seller == null || seller.isEmpty() || current == null) {
            return false;      // buyer skipped this check
        }
        return !current.getOwnerPhone().equals(seller);
    }

    @Override
    public int getPenalty(PhoneHistory history) {
        return 25;
    }

    @Override
    public String getMessage(PhoneHistory history) {
        return "Seller is NOT the registered current owner of this phone";
    }
}

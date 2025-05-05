import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

public class Client {
    private final String name;
    private final Map<LocalDateTime,Bill> bills = new LinkedHashMap<>();

    public Client(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    /** Truncate to seconds so parsing/lookup aligns */
    public Bill addBill(LocalDateTime dt) {
        LocalDateTime truncated = dt.truncatedTo(ChronoUnit.SECONDS);
        Bill bill = new Bill(truncated);
        bills.put(truncated, bill);
        return bill;
    }

    public Map<LocalDateTime,Bill> getBills() {
        return bills;
    }

    @Override
    public String toString() {
        return name;
    }
}

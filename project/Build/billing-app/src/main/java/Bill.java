import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class Bill {
    private final LocalDateTime dateTime;
    private final Map<String,Product> products = new LinkedHashMap<>();
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Bill(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public LocalDateTime getDateTime() { return dateTime; }

    public void addProduct(String name, double price) {
        products.put(name, new Product(name, price));
    }

    public Map<String,Product> getProducts() {
        return products;
    }

    @Override
    public String toString() {
        return dateTime.format(FMT);
    }
}

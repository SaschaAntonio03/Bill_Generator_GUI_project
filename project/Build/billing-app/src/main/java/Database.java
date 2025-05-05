import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class Database {
    private final Map<String,Client> clients = new LinkedHashMap<>();
    private static final DateTimeFormatter CSV_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Client getOrCreateClient(String name) {
        return clients.computeIfAbsent(name, Client::new);
    }

    public Map<String,Client> getClients() {
        return clients;
    }

    /** Load entire DB from database.csv if present */
    public void loadFromCsv(File csv) throws IOException {
        if (!csv.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(csv))) {
            String line;
            r.readLine(); // skip header
            while ((line = r.readLine()) != null) {
                String[] parts = line.split(",", 4);
                if (parts.length < 4) continue;
                String clientName = parts[0];
                LocalDateTime dt  = LocalDateTime.parse(parts[1], CSV_FMT);
                String pname      = parts[2];
                double price      = Double.parseDouble(parts[3]);

                Client c = getOrCreateClient(clientName);
                Bill b = c.getBills().get(dt);
                if (b == null) {
                    b = c.addBill(dt);
                }
                b.addProduct(pname, price);
            }
        }
    }

    /** Save the entire DB to database.csv (overwrites) */
    public void saveToCsv(File csv) throws IOException {
        try (PrintWriter w = new PrintWriter(new FileWriter(csv))) {
            w.println("Client,BillDateTime,ProductName,Price");
            for (var e : clients.entrySet()) {
                String clientName = e.getKey();
                Client c = e.getValue();
                for (var be : c.getBills().entrySet()) {
                    LocalDateTime dt = be.getKey();
                    Bill b = be.getValue();
                    for (Product p : b.getProducts().values()) {
                        w.printf("%s,%s,%s,%.2f%n",
                                clientName,
                                dt.format(CSV_FMT),
                                p.getName(),
                                p.getPrice()
                        );
                    }
                }
            }
        }
    }

    /** Export a single bill into an XLSX template */
    public void exportBillToXlsx(File template, File out,
                                 String clientName,
                                 LocalDateTime billDate) throws Exception {
        ExcelExporter.export(template, out, clientName, billDate, this);
    }
}

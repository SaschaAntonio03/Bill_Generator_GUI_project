import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;              // <-- only the NIO Path
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class BillingApp extends JFrame {
    private final Database db = new Database();
    private final JTree tree;
    private final DefaultTreeModel model;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final File dbFile = new File("database.csv");

    public BillingApp() throws Exception {
        super("Billing System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600,400);

        // 1) pick a user‐writable spot (Application Support on macOS)
        String usr = System.getProperty("user.home");
        Path appSupport = Paths.get(usr,
        "Library","Application Support","BillingApp");
        if (!Files.exists(appSupport)) {
            Files.createDirectories(appSupport);
        }
        File dbFile = appSupport.resolve("database.csv").toFile();

        // 2) if first run, copy the _embedded_ blank CSV out
        if (!dbFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/database.csv")) {
                if (in == null) throw new FileNotFoundException(
                    "database.csv not bundled in JAR!");
                Files.copy(in, dbFile.toPath());
            }
        }

        // 3) now load
        db.loadFromCsv(dbFile);


        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) {
                try { db.saveToCsv(dbFile); }
                catch(Exception ex) { ex.printStackTrace(); }
            }
        });

        // Menus
        JMenuBar mb = new JMenuBar();

        JMenu mC = new JMenu("Client");
        mC.add(new AbstractAction("Add Client") {
            public void actionPerformed(ActionEvent e) {
                String name = JOptionPane.showInputDialog(
                        BillingApp.this, "Client name:");
                if (name!=null && !name.isBlank()) {
                    db.getOrCreateClient(name);
                    refresh();
                }
            }
        });
        mb.add(mC);

        JMenu mB = new JMenu("Bill");
        mB.add(new AbstractAction("Add Bill") {
            public void actionPerformed(ActionEvent e) {
                String client = JOptionPane.showInputDialog(
                        BillingApp.this, "Client name:");
                if (client==null) return;
                Client cObj = db.getClients().get(client);
                if (cObj==null) {
                    showErr("No such client");
                    return;
                }
                cObj.addBill(LocalDateTime.now());
                refresh();
            }
        });
        mb.add(mB);

        JMenu mP = new JMenu("Product");
        mP.add(new AbstractAction("Add Product") {
            public void actionPerformed(ActionEvent e) {
                TreePath p = tree.getSelectionPath();
                if (p==null || p.getPathCount()!=3) {
                    showErr("Select a Bill node");
                    return;
                }
                String client = p.getPathComponent(1).toString();
                String dateS  = p.getPathComponent(2).toString();
                String name   = JOptionPane.showInputDialog(
                        BillingApp.this, "Product name:");
                String priceS = JOptionPane.showInputDialog(
                        BillingApp.this, "Price:");
                try {
                    double price = Double.parseDouble(priceS);
                    LocalDateTime dt = LocalDateTime.parse(dateS, FMT);
                    db.getClients()
                            .get(client)
                            .getBills()
                            .get(dt)
                            .addProduct(name, price);
                    refresh();
                } catch(Exception ex) {
                    showErr(ex.getMessage());
                }
            }
        });
        mb.add(mP);

        JMenu mE = new JMenu("Export");
        mE.add(new AbstractAction("Export XLSX") {
            public void actionPerformed(ActionEvent e) {
                TreePath p = tree.getSelectionPath();
                if (p == null || p.getPathCount() != 3) {
                    showErr("Select a Bill node first");
                    return;
                }
                String client = p.getPathComponent(1).toString();
                String dateS  = p.getPathComponent(2).toString();
                LocalDateTime dt;
                try {
                    dt = LocalDateTime.parse(dateS, FMT);
                } catch(Exception ex) {
                    showErr("Invalid bill date");
                    return;
                }

                // 1) template file (adjust if yours lives elsewhere)
                // old File template = new File("lib/Soumission-Canal1-2.xlsx");

                File template;
                try (InputStream tplIn =
                        getClass().getResourceAsStream("/Soumission-Canal1-2.xlsx")) {
                    if (tplIn == null) {
                        showErr("Template not bundled in JAR!");
                        return;
                    }
                    Path tmp = Files.createTempFile("tpl", ".xlsx");
                    Files.copy(tplIn, tmp, StandardCopyOption.REPLACE_EXISTING);
                    template = tmp.toFile();
                } catch (IOException ioe) {
                    showErr("Cannot load template: " + ioe.getMessage());
                    return;
                }
                if (template==null || !template.exists()) {
                    showErr("Template not available");
                    return;
                }

                // 2) output filename based on timestamp
                String outName = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
                        + ".xlsx";
                File outFile = new File(outName);

                // 3) export
                try {
                    db.exportBillToXlsx(template, outFile, client, dt);
                    JOptionPane.showMessageDialog(
                            BillingApp.this,
                            "Wrote " + outFile.getName() + " in " + System.getProperty("user.dir")
                    );
                } catch(Exception ex) {
                    showErr(ex.getMessage());
                }
            }
        });
        mb.add(mE);

        setJMenuBar(mb);

        // Tree
        DefaultMutableTreeNode root =
                new DefaultMutableTreeNode("Clients");
        model = new DefaultTreeModel(root);
        tree  = new JTree(model);
        add(new JScrollPane(tree), BorderLayout.CENTER);

        refresh();
        setVisible(true);
    }

    private void refresh() {
        DefaultMutableTreeNode root =
                new DefaultMutableTreeNode("Clients");
        db.getClients().forEach((name,cObj)->{
            DefaultMutableTreeNode nc =
                    new DefaultMutableTreeNode(name);
            cObj.getBills().forEach((dt,b)->{
                DefaultMutableTreeNode nb =
                        new DefaultMutableTreeNode(
                                dt.format(FMT));
                b.getProducts().values()
                        .forEach(prod->nb.add(
                                new DefaultMutableTreeNode(
                                        prod.toString())));
                nc.add(nb);
            });
            root.add(nc);
        });
        model.setRoot(root);
        tree.expandRow(0);
    }

    private void showErr(String msg) {
        JOptionPane.showMessageDialog(
                this, msg, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            try { new BillingApp(); }
            catch(Exception ex){ ex.printStackTrace(); }
        });
    }
}

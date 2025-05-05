import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;

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

        // load on start
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
                File template = new File("lib/Soumission-Canal1-2.xlsx");
                if (!template.exists()) {
                    showErr("Template not found at " + template.getPath());
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

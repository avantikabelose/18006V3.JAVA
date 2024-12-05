import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

class IceCream
{
    private ArrayList<Integer> flavorIds;
    private ArrayList<Double> flavorPrices;
    private ArrayList<Integer> toppingIds;
    private ArrayList<Double> toppingPrices;
    private int numScoops;
    private int numCups;
    private int numCones;
    private int numWaffles;
    private static Connection conn;

    public IceCream(Connection connection) 
    {
        this.flavorIds = new ArrayList<>();
        this.flavorPrices = new ArrayList<>();
        this.toppingIds = new ArrayList<>();
        this.toppingPrices = new ArrayList<>();
        this.numScoops = 0;
        this.numCups = 0;
        this.numCones = 0;
        this.numWaffles = 0;
        IceCream.conn = connection;
    }

    public void addFlavor(int flavorId, double price) 
    {
        flavorIds.add(flavorId);
        flavorPrices.add(price);
    }

    public void addTopping(int toppingId, double price) 
    {
        toppingIds.add(toppingId);
        toppingPrices.add(price);
    }

    public void setNumScoops(int num) 
    {
        this.numScoops = num;
    }

    public void setNumCups(int num) 
    {
        this.numCups = num;
    }

    public void setNumCones(int num) 
    {
        this.numCones = num;
    }

    public void setNumWaffles(int num) 
    {
        this.numWaffles = num;
    }

    public double calculateTotal() 
    {
        double total = 0;
        for (double price : flavorPrices) 
        {
            total += price;
        }
        total += (numScoops * 20);
        total += (numCups * 30);
        total += (numCones * 40);
        total += (numWaffles * 50);
        for (double price : toppingPrices) 
        {
            total += price;
        }
        return total;
    }

    public void printReceipt() 
    {
        System.out.println("----- Receipt -----");
        System.out.println("Flavors:");
        for (int i = 0; i < flavorIds.size(); i++) {
            System.out.printf("%d. Flavor ID: %d (%.2f Rs.)\n", i + 1, flavorIds.get(i), flavorPrices.get(i));
        }

        if (numScoops > 0) 
        {
            double scoopTotal = numScoops * 20;
            System.out.printf("Scoops: %d (%.2f Rs.)\n", numScoops, scoopTotal);
        }
        if (numCups > 0) 
        {
            double cupTotal = numCups * 30;
            System.out.printf("Cups: %d (%.2f Rs.)\n", numCups, cupTotal);
        }
        if (numCones > 0) 
        {
            double coneTotal = numCones * 40;
            System.out.printf("Cones: %d (%.2f Rs.)\n", numCones, coneTotal);
        }
        if (numWaffles > 0) 
        {
            double waffleTotal = numWaffles * 50;
            System.out.printf("Waffles: %d (%.2f Rs.)\n", numWaffles, waffleTotal);
        }

        System.out.println("Toppings:");
        for (int i = 0; i < toppingIds.size(); i++) 
        {
            System.out.printf("%d. Topping ID: %d (%.2f Rs.)\n", i + 1, toppingIds.get(i), toppingPrices.get(i));
        }

        double total = calculateTotal();
        System.out.printf("Total Cost: %.2f Rs.\n", total);
        System.out.println("-------------------");
    }

    public void saveOrder(int customerId) throws SQLException 
    {
        
        String insertOrderQuery = "INSERT INTO Orders (customer_id, total_cost) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertOrderQuery, Statement.RETURN_GENERATED_KEYS)) 
        {
            double total = calculateTotal();
            stmt.setInt(1, customerId);
            stmt.setDouble(2, total);
            stmt.executeUpdate();

            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) 
            {
                int orderId = rs.getInt(1);

               
                String insertFlavorQuery = "INSERT INTO OrderFlavors (order_id, flavor_id) VALUES (?, ?)";
                try (PreparedStatement flavorStmt = conn.prepareStatement(insertFlavorQuery)) 
                {
                    for (int flavorId : flavorIds) 
                    {
                        flavorStmt.setInt(1, orderId);
                        flavorStmt.setInt(2, flavorId);
                        flavorStmt.addBatch();
                    }
                    flavorStmt.executeBatch();
                }

               
                String insertToppingQuery = "INSERT INTO OrderToppings (order_id, topping_id) VALUES (?, ?)";
                try (PreparedStatement toppingStmt = conn.prepareStatement(insertToppingQuery)) 
                {
                    for (int toppingId : toppingIds) 
                    {
                        toppingStmt.setInt(1, orderId);
                        toppingStmt.setInt(2, toppingId);
                        toppingStmt.addBatch();
                    }
                    toppingStmt.executeBatch();
                }

                System.out.println("Order saved successfully!");
            }
        }
    }
}

class IceCreamClientJDBC1 
{
    private static int customerCount = 0;
    private static final int MAX_CUSTOMERS = 100;
    private static final String JDBC_URL = "jdbc:mysql://localhost:3307/icecreamparlour";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "";

    public static void main(String[] args) 
    {
        Scanner scanner = new Scanner(System.in);
        Connection connection = null;

        try 
        {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
        } 
            catch (SQLException e) 
            {
            System.out.println("Database connection failed!");
            e.printStackTrace();
            return;
        }

        while (customerCount < MAX_CUSTOMERS) 
        {
            System.out.println("\nCustomer " + (customerCount + 1) + " - Welcome to the Ice Cream Parlour!");
            IceCream iceCream = new IceCream(connection);

           
            System.out.println("Choose flavors (0 to stop):");
            String flavorQuery = "SELECT id, name, price FROM Flavors";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(flavorQuery)) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    double price = rs.getDouble("price");
                    System.out.printf("%d. %s (%.2f Rs.)\n", id, name, price);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            while (true) {
                int flavorChoice = scanner.nextInt();
                if (flavorChoice == 0) break;
                String flavorQueryById = "SELECT price FROM Flavors WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(flavorQueryById)) {
                    stmt.setInt(1, flavorChoice);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        double price = rs.getDouble("price");
                        iceCream.addFlavor(flavorChoice, price);
                    } else {
                        System.out.println("Invalid flavor ID. Please select again.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            // Set scoops, cups, cones, waffles
            System.out.println("How many scoops? (20 Rs. per scoop):");
            iceCream.setNumScoops(scanner.nextInt());

            System.out.println("How many cups? (30 Rs. per cup):");
            iceCream.setNumCups(scanner.nextInt());

            System.out.println("How many cones? (40 Rs. per cone):");
            iceCream.setNumCones(scanner.nextInt());

            System.out.println("How many waffles? (50 Rs. per waffle):");
            iceCream.setNumWaffles(scanner.nextInt());

            // Choose toppings
            System.out.println("Choose toppings (0 to stop):");
            String toppingQuery = "SELECT id, name, price FROM Toppings";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(toppingQuery)) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    double price = rs.getDouble("price");
                    System.out.printf("%d. %s (%.2f Rs.)\n", id, name, price);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            while (true) {
                int toppingChoice = scanner.nextInt();
                if (toppingChoice == 0) break;
                String toppingQueryById = "SELECT price FROM Toppings WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(toppingQueryById)) {
                    stmt.setInt(1, toppingChoice);
                    ResultSet rs = stmt.executeQuery();
		                    if (rs.next()) {
                        double price = rs.getDouble("price");
                        iceCream.addTopping(toppingChoice, price);
                    } else {
                        System.out.println("Invalid topping ID. Please select again.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            // Save the order and print the receipt
            try {
                iceCream.saveOrder(customerCount + 1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            iceCream.printReceipt();
            System.out.println("Thank you for your purchase!");
            customerCount++;
        }

        System.out.println("Maximum customer limit reached. Thank you for visiting!");
        scanner.close();
    }
}


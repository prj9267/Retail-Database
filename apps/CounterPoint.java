package apps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import javafx.util.Pair; 
import java.util.Map;
import java.util.Date;
import java.util.Calendar;
import java.sql.*;

/**
 * Main driver program for the counterpoint application.
 * 
 * @author Dylan R. Wagner
 */
public class CounterPoint{

    /**
     * Tbl enum represents item tables
     */
    enum Tbl{
        Items("ITEMS"),
        Foods("FOODS"),
        Beverage("BEVERAGE"),
        Pharma("PHARMA");

        private final String name;

        Tbl(String s){
            name = s;
        }
    }

    /**
     * Inner class checkout used to run logic behind transactions
     * Customer facing
     * 
     * @author Dylan R. Wagner
     * 
     */
    class Checkout{

        private int storeID;
        private int customerID;
        private HashMap<String, Integer> trans;
        private HashMap<String, Pair<CounterPoint.Tbl, Double>> tblMap;

        public Checkout(String storeID, String customerID){
            this.storeID = Integer.parseInt(storeID);
            this.customerID = Integer.parseInt(customerID);

            this.trans = new HashMap<String, Integer>();
            this.tblMap = new HashMap<String, Pair<CounterPoint.Tbl, Double>>();
        }

        /**
         * Main input loop for the transactions segment.
         * 
         * Commands:
         *      exit:
         *          Exit the program
         *      cancel:
         *          Cancel the transaction and go back to the main loop.
         *      add <UPC14> <Quantity>:
         *          Add a item to the transaction, if item is already in the transaction
         *          add the value to it.
         *      remove <UPC14> [Quantity]:
         *          Remove an item if no quantity is provided, decrement by quantity if
         *          provided. If quantity is greater than the value already in the transaction, 
         *          remove the entire item.
         *      enter:
         *          This submits the transaction for processing, resulting in a receipt. 
         *      help:
         *          Print help message.
         */
        public void mainInptLoopCheckout(){
            System.out.println("Transaction Mode Commands:");
            System.out.println("    exit - Quits program");
            System.out.println("    help - Shows help message");
            System.out.println("    enter - Submits transaction");
            System.out.println("    cancel - Cancels current transaction");
            System.out.println("    show - Displays current transaction");
            System.out.println("    add <upc14> <QTY> - Adds this item with quantity to the transaction");
            System.out.println("    remove <upc14> [QTY] - Removes the item with quantity from transaction");
            for(;;){
                System.out.print("[Checkout] >> ");
                String userInpt = in.nextLine();
                
                String[] userInptVals = userInpt.split(" ");

                if(userInptVals.length == 0) break;

                switch(userInptVals[0]){
                    case "exit": {
                        closeConnection();
                        System.exit(0);
                    }
                    case "help": {
                        showHelpCheckout();
                        break;
                    }
                    case "add": {

                        if(userInptVals.length > 2){
                            String upc14 = userInptVals[1];
                            int qty;

                            try{
                                qty = Integer.parseInt(userInptVals[2]);
                            } catch(NumberFormatException e){
                                System.out.println("Quantity is non-numeric");
                                break;
                            }

                            this.addItem(upc14, qty);
                        }

                        break;
                    }
                    case "remove": {
                        if(userInptVals.length == 1) break;

                        String upc14 = userInptVals[1];
                        int qty = -1;

                        if(userInptVals.length > 2){
                            try{
                                qty = Integer.parseInt(userInptVals[2]);
                            } catch(NumberFormatException e){
                                System.out.println("Quantity is non-numeric");
                                break;
                            }
                        } 

                        this.removeItem(upc14, qty);
                        break;                    
                    }
                    case "enter": {
                        // On successful transaction quit transaction mode
                        correctItems();
                        if(enterTransaction()){
                            printReceipt();
                            return;
                        } else {
                            System.out.println("Transaction Failed!");
                        }
                        break;
                    }
                    case "show": {
                        this.showTransaction();
                        break;
                    }
                    case "cancel": {
                        return;
                    }
                    default: {
                        System.out.println("Invalid command, enter 'help' for commands.");
                    }
                }
            }
        }

        /**
         * Adds item to the transaction. Invalid items are allowed to pass through
         * this method.
         * 
         * @param upc14 Unique item id
         * @param qty # of items to add to the transaction
         */
        private void addItem(String upc14, int qty){

            if(this.trans.containsKey(upc14)){
                int currQty = this.trans.get(upc14);
                this.trans.put(upc14, currQty + qty);
            } else {
                this.trans.put(upc14, qty);
            }
        }

        /**
         * Grabs all information needed to display a receipt item.
         * FORMAT: '\tPrice($)\tQuantity\tItem\n'  
         * 
         * @param upc14 The item unique code
         * @param qty The actual quantity of the item. 
         * @return
         *      The price (price * qty)
         */
        private Double printItem(String upc14, Integer qty){
            Double totalItemsPrice = null;

            try {

                String tblName = this.tblMap.get(upc14).getKey().name();
                Double price = this.tblMap.get(upc14).getValue();

                PreparedStatement pstmt = conn.prepareStatement(
                "SELECT NAME FROM " + tblName + " WHERE UPC14 = ?;");

                pstmt.setString(1, upc14);
                ResultSet res = pstmt.executeQuery();

                if (res.next()){
                    System.out.printf("\t %-10s%-10s%s\n", price.toString(), qty.toString(), res.getString(1));
                }

                totalItemsPrice = qty * price;

            } catch(SQLException e){
                e.printStackTrace();
            }

            return totalItemsPrice;
        }

        /**
         * Prints the whole receipt including total. 
         */
        private void printReceipt(){

            System.out.println("\nCustomer: " + customerName(customerID));
            // Print receipt header 
            System.out.printf("\n\t %-10s%-10s%s\n", "Price", "Quantity", "Item");

            Double totalPrice = 0.0;

            for (Map.Entry<String, Integer> entry : this.trans.entrySet()) {
                String key = entry.getKey();
                Integer val = entry.getValue();

                if (val != null && val > 0) totalPrice += printItem(key, val);
            }

            System.out.printf("\n\tTotal: %.2f\n", totalPrice);
        }

        /**
         * Removes or subtracts qty from the transaction.
         * 
         * @param upc14 Unique item id
         * @param qty # of items to add to the transaction
         */
        private void removeItem(String upc14, int qty){

            if(this.trans.containsKey(upc14)){
                int currQty = this.trans.get(upc14) - qty;

                if(currQty < 0 || qty == -1){
                     this.trans.remove(upc14);
                } else {
                    this.trans.put(upc14, currQty);
                }

            } else {
                System.out.println("Item: " + upc14 + " not found in transaction");
            }            
        }

        /**
         * Builds transaction prepared statement.
         * 
         * @return The constructed SQL insert command in format:
         * INSERT INTO TRANSACTIONS VALUES(null, TIMESTAMP_STR, STORE_ID, CUSTOMER_ID)
         */
        private PreparedStatement makeTransInsertPSTMT() throws SQLException{

            PreparedStatement pstmt = conn.prepareStatement(
            "INSERT INTO TRANSACTIONS VALUES(null, ?, ?, ?);", 
            Statement.RETURN_GENERATED_KEYS);

            String timestamp = new Timestamp(System.currentTimeMillis()).toString();

            pstmt.setString(1, timestamp);
            pstmt.setInt(2, this.storeID);
            pstmt.setInt(3, this.customerID);

            return pstmt;
        }

        /**
         * Queries the database for the maximum available for purchase. Providing the 
         * amount to be included into the purchase. 
         * 
         * @param upc14 The item to be purchased
         * @param tryAmt The amount requested
         * @return
         *      The amount requested if the maximum found is greater than the requested amount.
         *      null if the item to be purchased cannot be found. 
         */
        private Integer getBuyAmount(String upc14, Integer tryAmt){
            try{
                PreparedStatement stmt = conn.prepareStatement(
                "SELECT INVENTORY, TBL_ENUM, PRICE FROM PROD_STORE WHERE upc14 = ? AND STORE_ID = ?;");

                stmt.setString(1, upc14);
                stmt.setInt(2, this.storeID);

                ResultSet res = stmt.executeQuery();

                if (res.next()){
                    int amtFound = res.getInt(1);
                    // Grab corresponding tbl enum
                    CounterPoint.Tbl resTbl = CounterPoint.Tbl.values()[res.getInt(2)];
                    Double price = res.getDouble(3);
                    this.tblMap.put(upc14, new Pair<CounterPoint.Tbl, Double>(resTbl, price));

                    return amtFound > tryAmt ? tryAmt : amtFound;
                } else {
                    return null; // Cannot find the product in the store
                }
            } catch(SQLException e) {
                return null;
            }
        }

        /**
         * Updates the PROD_STORE table to reflect the purchase while tracking the
         * transactions of individual items within the TRANS_PROD table.
         * 
         * While calling this function, autocommit is turned off.
         * This is to force atom-icy onto this section. 
         *  
         * @param upc14 The item to be bought.
         * @param qty The item quantity. 
         */
        private void updateProdAddTransItem(Long transID, String upc14, Integer qty)
        throws SQLException
        {
            PreparedStatement upStmt = conn.prepareStatement(
            "UPDATE PROD_STORE SET INVENTORY = INVENTORY - ? WHERE UPC14 = ? AND STORE_ID = ?;");

            upStmt.setInt(1, qty); // Inventory amount
            upStmt.setString(2, upc14); // upc14
            upStmt.setInt(3, storeID); // store_id 

            PreparedStatement insStmt = conn.prepareStatement(
            "INSERT INTO PROD_TRANSACTIONS VALUES(?, ?, ?)");
            
            insStmt.setLong(1, transID); // Key from main transaction entry
            insStmt.setString(2, upc14); // upc14
            insStmt.setInt(3, qty); // quantity

            upStmt.executeUpdate();
            insStmt.executeUpdate();
        }

        /**
         * Enters and updates all valid items.
         * 
         * @param transactionKey The key that was inserted into the database
         */
        private void enterItems(Long transactionKey) throws SQLException {

            for (Map.Entry<String, Integer> entry : this.trans.entrySet()) {
                String key = entry.getKey();
                Integer val = entry.getValue();

                if (val != null) {
                        updateProdAddTransItem(transactionKey, key, val);
                }
            }
        }

        /**
         * Corrects the items within the user transaction. This function changes the
         * transaction map such that invalid items are assigned a null and 
         * no valid item has a quantity that is greater than the amount currently in the store.
         */
        private void correctItems(){
            // Find the correct buy amount
            this.trans.forEach((key, val) -> {
                this.trans.put(key, getBuyAmount(key, val));
            });
        }

        /**
         * Submits and checks the transaction to the database. This is the last step
         * in creating and submitting a transaction. 
         * 
         * @return True: on full or partial success, False: on full failure. 
         */
        private Boolean enterTransaction(){
            boolean success = false;

            try{
                // Turn off auto commit, make transaction
                conn.setAutoCommit(false);

                PreparedStatement stmt = makeTransInsertPSTMT();
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()){
                    Long genKey = stmt.getGeneratedKeys().getLong(1); // Get generated key
                    enterItems(genKey);

                    conn.commit();
                    success = true;
                }

            } catch(SQLException e) {
                try{
                    conn.rollback();
                } catch(SQLException e_nested) {}
            }

            try {
                conn.setAutoCommit(true);
            } catch (Exception e) {}

            return success;
        }

        /**
         * Displays the transaction to the user.
         */
        private void showTransaction(){
            this.trans.forEach((key, val) ->
            {if(val > 0) System.out.println(key + ": " + val.toString());});
        }

        /**
         * Displays help info to the user.
         */
        private void showHelpCheckout(){
            System.out.println("    exit - Quits program");
            System.out.println("    help - Shows help message");
            System.out.println("    enter - Submits transaction");
            System.out.println("    cancel - Cancels current transaction");
            System.out.println("    show - Displays current transaction");
            System.out.println("    add <upc14> <QTY> - Adds this item with quantity to the transaction");
            System.out.println("    remove <upc14> [QTY] - Removes the item with quantity from transaction, QTY can be omitted");
        }
    }

    /**
     * Inner class Resupply used to restock inventory at locations
     * Vendor facing
     */
    class Restock{

        Integer storeID;

        public Restock(String storeID){
            this.storeID = Integer.parseInt(storeID);
        }

        public void mainInptLoopRestock(){
            System.out.println("Restock Mode Commands:");
            System.out.println("    exit - Quits program");
            System.out.println("    help - Shows help message");
            System.out.println("    cancel - Cancels current transaction");
            System.out.println("    restock - Restocks all inventory");
            for(;;){
                System.out.print("[Restock] >> ");
                String userInpt = in.nextLine();
                
                String[] userInptVals = userInpt.split(" ");

                if(userInptVals.length == 0) break;

                switch(userInptVals[0]){
                    case "cancel": {
                        return;
                    }
                    case "exit": {
                        closeConnection();
                        System.exit(0);
                    }
                    case "help": {
                        showHelpRestock();
                        break;
                    }
                    case "restock": {
                        restockAll();
                        return;
                    }
                }
            }
        }

        /**
         * Shows help message for restock
         */
        private void showHelpRestock(){
            System.out.println("    exit - Quits program");
            System.out.println("    help - Shows help message");
            System.out.println("    cancel - Cancels current transaction");
            System.out.println("    restock - Restocks all inventory");
        }

        /**
         * Updates the prod_store table with the items found within the restock table.
         * 
         * @param upc14 The item key
         * @param inventory The amount to update.
         * @return
         *      True on success, false on failure.
         */
        private boolean updateProdStore(String upc14, Integer inventory){
            try {
                PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE PROD_STORE SET INVENTORY = INVENTORY + ? WHERE UPC14 = ? AND STORE_ID = ?;");

                pstmt.setInt(1, inventory);
                pstmt.setString(2, upc14);
                pstmt.setInt(3, storeID);

                pstmt.executeUpdate();

                return true;

            } catch(SQLException e){
                e.printStackTrace();
            }

            return false;
        }

        /**
         * Deletes a reorder request from the reorder_request table.
         *
         * @param request_id The reorder request id
         */
        private void deleteRestockReq(int request_id){
            try {
                PreparedStatement pstmt = conn.prepareStatement(
                "DELETE FROM REORDER_REQUEST WHERE REQUEST_ID = ?;");

                pstmt.setInt(1, request_id);

                pstmt.executeUpdate();
            } catch(SQLException e) {
                e.printStackTrace();;
            }
        }

        /**
         * Restocks all of the reorder requests for the current store.
         */
        private void restockAll(){
            try {
                PreparedStatement pstmt = conn.prepareStatement(
                "SELECT * FROM REORDER_REQUEST WHERE STORE_ID = ? AND SHIPMENT_DATE IS NOT NULL;");

                pstmt.setInt(1, storeID);

                ResultSet res = pstmt.executeQuery();

                while(res.next()){
                    // Get today's date without time
                    Calendar c1 = Calendar.getInstance();
                    c1.set(Calendar.HOUR_OF_DAY, 0);
                    c1.set(Calendar.MINUTE, 0);
                    c1.set(Calendar.SECOND, 0);
                    c1.set(Calendar.MILLISECOND, 0);
                    Date today = c1.getTime();

                    // Get shipment date without time
                    Timestamp shipment_date = res.getTimestamp("SHIPMENT_DATE");
                    Calendar c2 = Calendar.getInstance();
                    c2.setTime(shipment_date);
                    c2.set(Calendar.HOUR_OF_DAY, 0);
                    c2.set(Calendar.MINUTE, 0);
                    c2.set(Calendar.SECOND, 0);
                    c2.set(Calendar.MILLISECOND, 0);
                    Date ship_date = c2.getTime();

                    // Update inventory and close request if shipment date has passed
                    if(today.after(ship_date)||today.equals(ship_date)) {
                        String upc14Val = res.getString("UPC_14");
                        Integer qty = res.getInt("QUANTITY");

                        updateProdStore(upc14Val, qty);

                        System.out.printf("Updated: %s, QTY: %s, Store: %d\n", upc14Val, qty, storeID);

                        // Remove completed reorder request
                        int request_id = res.getInt("REQUEST_ID");
                        deleteRestockReq(request_id);
                    }
                }

            } catch(SQLException e) {
                e.printStackTrace();;
            }
        }
    }

    // User input
    Scanner in;
    // Database connection
    Connection conn;


    /**
     * Constructor for the counter point class
     * 
     * @param location The location of the .db file.
     * @param user The username for the database.
     * @param password The password to the database.
     */
    public CounterPoint(String location, 
                        String user, 
                        String password){
        
        String url = "jdbc:h2:" + location;

        try{
            this.conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected: " + url + ";IFEXISTS=TRUE");
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        this.in = new Scanner(System.in);
    }

    /**
    * Used on program exit, closes the database. 
    */
    public void closeConnection(){
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }

    /**
     * Main mode selector code, loops until the user inputs a valid command.
     * 
     * @return The user input
     */
    public String[] mainInptLoop(){
        for(;;){
            System.out.print(">> ");
            String userInpt = this.in.nextLine();

            String[] userInptVals = userInpt.split(" ");
            if(userInptVals.length == 0) break;

            switch(userInptVals[0]){
                case "trans": {

                    // Check if length is correct
                    if(userInptVals.length != 3){
                        System.out.println("Expected <StoreID> <CustomerID>");
                        break;
                    }

                    if(!this.storeExists(userInptVals[1])){
                        System.out.println("Cannot find store with id: " + userInptVals[1]);
                        break;
                    }


                    if(!this.customerExists(Integer.parseInt(userInptVals[2]))){
                        System.out.println("Cannot find customer with id: " + userInptVals[2]);
                        break;
                    }

                    return userInptVals;
                }
                case "restock": {
                    // Check if length is correct
                    if(userInptVals.length != 2){
                        System.out.println("Expected <StoreID>");
                        break;
                    }

                    if(!this.storeExists(userInptVals[1])){
                        System.out.println("Cannot find store with id: " + userInptVals[1]);
                        break;
                    }

                    return userInptVals;
                }
                case "exit": {
                    this.closeConnection();
                    System.exit(0);
                }
                case "help": {
                    this.showCommands();
                    break;
                }
                default: {
                    System.out.println("Invalid command");
                    break;
                }
            }
        }

        return null;
    }

    /**
     * Checks if store exists within the database
     * 
     * @param storeID The id of the store to be checked
     * @return True if store is found, False otherwise
     */
    private Boolean storeExists(String storeID){
        
        String query = "SELECT 1 FROM STORES WHERE Store_ID = " + storeID + ";";
        int nrows = 0;

        try{
            Statement stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery(query);
            res.last();
            nrows = res.getRow();

        } catch(SQLException e){
            e.printStackTrace();
        }

        return nrows == 1;
    }

    /**
     * Grabs the customer name from the the database.
     * 
     * @param customerID The customer id.
     * @return
     *      The firstname lastname for the customer
     */
    String customerName(Integer customerID){
        String name = "UNKNOWN";

        try {
            PreparedStatement pstmt = conn.prepareStatement(
            "SELECT FIRSTNAME, LASTNAME FROM CUSTOMER WHERE CUSTOMER_ID = ?;");

            pstmt.setInt(1, customerID);
            ResultSet res = pstmt.executeQuery();

            if(res.next()){
                name = res.getString(1) + " " + res.getString(2);
            }

        } catch(SQLException e) {
            e.printStackTrace();
        }

        return name;
    }

    /**
     * Checks if customer exists within the database
     * 
     * @param customerID The id of the customer to be checked
     * @return True if customer is found, False otherwise
     */
    private Boolean customerExists(Integer customerID){
        
        String query = "SELECT 1 FROM CUSTOMER WHERE CUSTOMER_ID = " + customerID.toString() + ";";
        int nrows = 0;

        try{
            Statement stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery(query);
            res.last();
            nrows = res.getRow();

        } catch(SQLException e){
            e.printStackTrace();
        }

        return nrows == 1;
    }

    /**
     * Displays listing and description of commands to the user.
     */
    private void showCommands(){
        System.out.println("    exit - Quits the program");
        System.out.println("    help - Shows help message");
        System.out.println("    trans <StoreID> <CustomerID> - Enters transaction mode");
        System.out.println("    restock <StoreID> - Enters restock mode");
    }

    public static void main(String[] args) {
        
        CounterPoint mainObj = new CounterPoint("./database/db", "user", "password");

        System.out.println("Commands:");
        System.out.println("    exit - Quits the program");
        System.out.println("    help - Shows help message");
        System.out.println("    trans <StoreID> <CustomerID> - Enters transaction mode");
        System.out.println("    restock <StoreID> - Enters restock mode");

        for(;;){
            String[] res = mainObj.mainInptLoop();

            if(res[0].equals("trans")){
                CounterPoint.Checkout checkout;
                checkout = mainObj.new Checkout(res[1], res[2]);

                checkout.mainInptLoopCheckout();
            } else if(res[0].equals("restock")) {
                CounterPoint.Restock restock;
                restock = mainObj.new Restock(res[1]);

                restock.mainInptLoopRestock();
            }
        }
    }
}

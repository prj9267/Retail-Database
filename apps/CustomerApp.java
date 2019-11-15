package apps;

import org.h2.util.StringUtils;
import java.sql.*;
import java.util.Scanner;

/**
 * Program for the Customer Application
 */
public class CustomerApp {

    private Scanner in;
    private Connection conn;

    public CustomerApp(String location, String user, String password){
        String url = "jdbc:h2:" + location;

        try {
            this.conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected: " + url + ";IFEXISTS=TRUE");
        }
        catch (SQLException e) {
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
     * Checks if customer exists within the database
     *
     * @param customerID The id of the customer to be checked
     * @return True if customer is found, False otherwise
     */
    private Boolean customerExists(String customerID){

        String query = "SELECT 1 FROM CUSTOMER WHERE CUSTOMER_ID = " + customerID + ";";
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
     * Creates a new customer and inserts them into the database.
     */
    private void createCustomer(){
        System.out.print("<First Name> <Last Name> <Phone Number> (XXX-XXX-XXXX or 0 for Phone #)\n");
        while(true) {
            System.out.print(">> ");

            String input = in.nextLine();
            String[] inputValues = input.split(" ");

            if(input.equals("EXIT")) exitProgram();

            //IF MORE THAN 3 INPUTS ARE TYPED
            if(inputValues.length != 3) {
                System.out.print("Invalid Input: FORMAT\n");
            }
            //IF PHONE NUM HAS INCORRECT DIGITS
            else if(inputValues[2].length() != 12 && !inputValues[2].equals("0")){
                System.out.print("Invalid Phone Number\n");
            }
            else{
                String fname = inputValues[0];
                String lname = inputValues[1];
                String phone = inputValues[2];
                int customer_ID = getNewID();
                
                String query = String.format("INSERT INTO CUSTOMER VALUES (%d,'%s','%s','%s');",customer_ID,fname,lname,phone);

                try {
                    Statement statement = conn.createStatement();
                    statement.execute(query);

                    System.out.printf("NEW CUSTOMER:\n    ID: %d\n    Name: %s %s\n    Phone Number: %s\nENTER to continue\n>> ",customer_ID,fname,lname,phone);
                    in.nextLine();

                    //GOES BACK TO INITIAL SCREEN
                    System.out.print("\n<Customer_ID> (NEW to create ID):\nEXIT to quit program.\n");
                }
                catch(SQLException e){
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    /**
     * Auto-Increments the ID of a newly created customer based on the highest numeric value ID of an already existing customer and adding one.
     *
     * @return  - the int value of the new customer ID
     */
    private int getNewID(){
        String query = "SELECT MAX(CUSTOMER_ID) FROM CUSTOMER";
        try{
            Statement statement = conn.createStatement();
            ResultSet res = statement.executeQuery(query);
            if(res.next()){
                return res.getInt(1) + 1;
            }

        }
        catch(SQLException e){
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * Retrieves and prints a list of the Customer_ID's transactions from the database.
     *
     * @param customer_ID   - the specific customer_ID
     */
    private void retrieveCustomerTransactions(String customer_ID){
        String query = "SELECT TRANSACTIONS.TRANSACTION_ID,TIMESTAMP,STORE_ID,UPC14,QUANTITY FROM (TRANSACTIONS JOIN PROD_TRANSACTIONS) WHERE CUSTOMER_ID = '" + customer_ID + "' AND TRANSACTIONS.TRANSACTION_ID = PROD_TRANSACTIONS.TRANSACTION_ID;";

        try{
            Statement statement = conn.createStatement();
            ResultSet res = statement.executeQuery(query);
            if(res.next()){
                do{
                    System.out.println("\nTransaction_ID: " + res.getInt("TRANSACTION_ID")+
                                     "\n    Time/Date: " + res.getString("TIMESTAMP")+
                                     "\n    Store ID: " + res.getInt("STORE_ID")+
                                     "\n    Product UPC14: " + res.getString("UPC14")+
                                     "\n    Quantity: " + res.getInt("QUANTITY")+"\n");
                } while (res.next());
            }
            else{
                System.out.println("NO PREVIOUS TRANSACTIONS");
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * Retrieves and prints a list of stores that sell a specific product from the database.
     *
     * @param upc14 - the specific product_upc_14
     */
    private void retrieveStoreList(String upc14){
        String query = "SELECT STORE_ID,PRICE FROM PROD_STORE WHERE UPC14 = '" + upc14 + "';";
        try {
            Statement statement = conn.createStatement();
            ResultSet res = statement.executeQuery(query);
            if(res.next()){
                System.out.println("\nList of Availability for: " + upc14);
                do{
                    System.out.println("Store ID: " + res.getInt("STORE_ID") + "   Price: $" + res.getInt("PRICE"));
                }while(res.next());
            }
            else{
                System.out.println("NO AVAILABLE STORES WITH UPC14: " + upc14);
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * Retrieves and prints the information of specific store from the database.
     *
     * @param store_ID  - the specific store_ID
     */
    private void retrieveStoreInfo(String store_ID){
        String query = "SELECT * FROM STORES WHERE STORE_ID = '" + store_ID +"';";
        try {
            Statement statement = conn.createStatement();
            ResultSet res = statement.executeQuery(query);
            if (res.next()) {
                System.out.println("\nStore_ID: " + store_ID +
                                   "\n  Address: " + res.getString("ADDRESS") + ", " + res.getString("CITY") + "," + res.getString("STATE") + "," + res.getString("ZIP"));
            }
            else{
                System.out.println("Invalid Store_ID");
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * Retrieves and prints the information of the most sold product at a specific store from the database.
     *
     * INCOMPLETE
     *
     * @param store_ID  - the specific store_ID
     */
    private void retrieveMostSoldProduct(String store_ID){
        String subQuery = "SELECT UPC14,SUM(QUANTITY) AS Q FROM (TRANSACTIONS INNER JOIN PROD_TRANSACTIONS ON TRANSACTIONS.TRANSACTION_ID = PROD_TRANSACTIONS.TRANSACTION_ID) WHERE STORE_ID = " + store_ID + " GROUP BY UPC14";
        String query = "SELECT UPC14,Q FROM (" + subQuery + ") WHERE Q=(SELECT DISTINCT MAX(Q) FROM (" + subQuery + "));";
        try{
            Statement statement = conn.createStatement();
            ResultSet res = statement.executeQuery(query);
            if(res.next()){
                System.out.println("\nBased on recent transaction data..." +
                                   "\nMost Sold Product at Store: " + store_ID +
                                   "\n   UPC14: " + res.getString(1) +
                                   "\n   QUANTITY: " + res.getInt(2));
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * Retrieves and prints the inventory of a specific product at a specific store from the database.
     * @param upc14     - the specific product_upc_14
     * @param store_ID  - the specific store_ID
     */
    private void retrieveInventory(String upc14, String store_ID){
        String query = "SELECT UPC14, STORE_ID, INVENTORY FROM PROD_STORE WHERE STORE_ID = '" + store_ID + "' AND UPC14 = '" + upc14 + "';";
        try {
            Statement statement = conn.createStatement();
            ResultSet res = statement.executeQuery(query);
            if (res.next()) {
                System.out.println("\nProduct UPC_14: " + upc14 + "   Store_ID: " + store_ID +
                                 "\n    Inventory: " + res.getString("INVENTORY"));
            }
            else{
                System.out.println("Invalid Store_ID/UPC14");
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * Retrieves and prints the information of a specific product from the database.
     *
     * INCOMPLETE
     *
     * @param upc14 - the specific product_upc_14
     */
    private void retrieveProductInfo(String upc14){
        String query = "SELECT DISTINCT TBL_ENUM FROM PROD_STORE WHERE UPC14 = '" + upc14 + "';";
        try {
            Statement statement = conn.createStatement();
            ResultSet res = statement.executeQuery(query);
            if(res.next()){
                int prodType = res.getInt("TBL_ENUM");

                //IF PRODUCT IS NON-CONSUMABLE
                if(prodType == 0){
                    query = "Select * from ITEMS WHERE UPC14 = '" + upc14 + "';";
                    res = statement.executeQuery(query);
                    if(res.next()){
                        System.out.println("\nUPC_14: " + upc14 +
                                         "\n    Name: " + res.getString("NAME") +
                                         "\n    Brand_ID: " + res.getString("BRAND_ID"));
                    }
                }
                //IF PRODUCT IS PHARMACEUTICAL
                else if(prodType == 3){
                    query = "Select * from PHARMA WHERE UPC14 = '" + upc14 + "';";
                    res = statement.executeQuery(query);
                    if(res.next()){
                        System.out.println("\nUPC_14: " + upc14 +
                                "\n    Name: " + res.getString("NAME") +
                                "\n    Brand_ID: " + res.getString("BRAND_ID") +
                                "\n    Over The Counter: " + res.getString("OTC"));
                    }
                }
                //IF PRODUCT IS A FOOD
                else if(prodType == 1){
                    query = "Select * from FOODS WHERE UPC14 = '" + upc14 + "';";
                    res = statement.executeQuery(query);
                    if(res.next()){
                        String allergens = res.getString("ALLERGENS");

                        if(allergens == null) allergens = "None";

                        System.out.println("\nUPC_14: " + upc14 +
                                "\n    Name: " + res.getString("NAME") +
                                "\n    Brand_ID: " + res.getString("BRAND_ID") +
                                "\n    Calories: " + res.getInt("CALORIES") +
                                "\n    Storage: " + res.getString("STORAGE") +
                                "\n    Allergens: " + allergens +
                                "\n    Weight: " + res.getString("WEIGHT"));
                    }
                }
                //IF PRODUCT IS A BEVERAGE
                else{
                    query = "Select * from BEVERAGE WHERE UPC14 = '" + upc14 + "';";
                    res = statement.executeQuery(query);
                    if(res.next()){
                        String allergens = res.getString("ALLERGENS");

                        if(allergens == null) allergens = "None";

                        System.out.println("\nUPC_14: " + upc14 +
                                "\n    Name: " + res.getString("NAME") +
                                "\n    Brand_ID: " + res.getString("BRAND_ID") +
                                "\n    Calories: " + res.getInt("CALORIES") +
                                "\n    Storage: " + res.getString("STORAGE") +
                                "\n    Allergens: " + allergens +
                                "\n    Weight: " + res.getString("WEIGHT") +
                                "\n    Alcoholic: " + res.getString("ALCOHOLIC"));
                    }
                }
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }


    /**
     * Runs the Customer Application that allows any Customer to interact with the database using a valid Customer_ID, or create their own Customer_ID.
     */
    private void runApplication(){

        System.out.print("\n<Customer_ID> (NEW to create ID):\nEXIT to quit program.\n");

        while(true){
            System.out.print(">> ");
            String input = in.nextLine();

            if(input.isEmpty()) exitProgram();

            // CREATES NEW CUSTOMER
            if(input.equals("NEW")){
                createCustomer();
            }
            // IF USER INPUTS A NUMERIC ID
            else if(StringUtils.isNumber(input)){

                boolean exist = this.customerExists(input);

                //IF THE CUSTOMER_ID DOESNT EXIST
                if(!exist){
                    System.out.println("Invalid Customer_ID");
                }
                else{
                    String customer_ID = input;

                    System.out.print("1) List of Transactions" +
                                   "\n2) List of Stores that sell <Product_UPC_14>" +
                                   "\n3) Information of Store <Store_ID>" +
                                   "\n4) Most sold product at <Store_ID>" +
                                   "\n5) Inventory of <Product_UPC_14> at <Store_ID>" +
                                   "\n6) Information of <Product_UPC_14>."+
                                   "\n   Enter numeric choice with specific input (e.g. '5 <Product_UPC_14> <Store_ID>')" +
                                   "\n   EXIT to quit program" +
                                   "\n   HELP to re-display commands." +
                                   "\n>> ");

                    while(true) {

                        input = in.nextLine();
                        String[] inputValues = input.split(" ");

                        if(input.isEmpty()) exitProgram();

                        //WHICHEVER OPTION USER CHOSE...
                        switch (inputValues[0]) {
                            case "1": {
                                if(inputValues.length != 1){
                                    System.out.print("Invalid Format");
                                }
                                else{
                                    retrieveCustomerTransactions(customer_ID);
                                }
                                break;
                            }
                            case "2": {
                                if(inputValues.length != 2){
                                    System.out.print("Invalid Format");
                                }
                                else{
                                    retrieveStoreList(inputValues[1]);
                                }
                                break;
                            }
                            case "3": {
                                if(inputValues.length != 2){
                                    System.out.print("Invalid Format");
                                }
                                else{
                                    retrieveStoreInfo(inputValues[1]);
                                }
                                break;
                            }
                            case "4": {
                                if(inputValues.length != 2){
                                    System.out.print("Invalid Format");
                                }
                                else{
                                    retrieveMostSoldProduct(inputValues[1]);
                                }
                                break;
                            }
                            case "5": {
                                if(inputValues.length != 3){
                                    System.out.print("Invalid Format");
                                }
                                else{
                                    retrieveInventory(inputValues[1], inputValues[2]);
                                }
                                break;
                            }
                            case "6": {
                                if(inputValues.length != 2){
                                    System.out.print("Invalid Format");
                                }
                                else{
                                    retrieveProductInfo(inputValues[1]);
                                }
                                break;
                            }
                            case "EXIT":{
                                exitProgram();
                            }
                            case "HELP":{
                                System.out.print("1) List of Transactions" +
                                        "\n2) List of Stores that sell <Product_UPD_14>" +
                                        "\n3) Information of Store <Store_ID>" +
                                        "\n4) Most sold product at <Store_ID>" +
                                        "\n5) Inventory of <Product_UPC_14> at <Store_ID>" +
                                        "\n6) Information of <Product_UPC_14>."+
                                        "\n   Enter numeric choice with specific input (e.g. '5 <Product_UPC_14> <Store_ID>')" +
                                        "\n   EXIT to quit program" +
                                        "\n   HELP to re-display commands.");
                                break;
                            }
                            default: {
                                System.out.print("Invalid Input");
                                break;
                            }
                        }
                        System.out.print("\n>> ");
                    }
                }
            }
            // EXITS PROGRAM
            else if(input.equals("EXIT")){
                exitProgram();
            }
            // IF INPUT CONTAINS NON-NUMERIC CHARACTERS
            else{
                System.out.println("Invalid Input: NON-NUMERIC");
            }
        }
    }

    /**
     * Exits the entire program.
     */
    private void exitProgram(){
        this.closeConnection();
        System.exit(0);
    }

    public static void main(String[] args){
        CustomerApp app = new CustomerApp("./database/db", "user", "password");
        app.runApplication();
    }
}

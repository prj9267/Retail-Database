package apps;

import java.sql.Connection;
import java.util.Scanner;
import java.sql.*;
import java.util.Date;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

// SQL Statements
// 01: SELECT * FROM REORDER_REQUEST WHERE VENDOR_ID = SUPPLIED_ID AND SHIPMENT_DATE IS NULL;
//      Get all open reorder requests for a given vendor
// 02: UPDATE REORDER_REQUEST SET SHIPMENT_DATE = SUPPLIED_DATE WHERE REQUEST_ID = SUPPLIED_ID;
//      Close an open reorder request by setting its shipment date
// 03: SELECT * FROM REORDER_REQUEST WHERE UPC_14 = SUPPLIED_UPC14 AND SHIPMENT_DATE IS NULL AND VENDOR_ID = SUPPLIED_ID;
//      Gets all open reorder requests for signed-in vendor for a given UPC_14 code
// 04: SELECT count(REQUEST_ID) FROM REORDER_REQUEST WHERE UPC_14 = SUPPLIED_UPC14 AND SHIPMENT_DATE IS NULL AND VENDOR_ID = SUPPLIED_ID;
//      Gets count of all open reorder requests for signed-in vendor for a given UPC_14 code
// 05: SELECT 1 FROM REORDER_REQUEST WHERE UPC_14 = SUPPLIED_UPC14 AND SHIPMENT_DATE IS NULL AND VENDOR_ID = SUPPLIED_ID;
//      Checks to see if a reorder requests for signed-in vendor exists for a product with a given id
// 06: SELECT 1 FROM REORDER_REQUEST WHERE REQUEST_ID = SUPPLIED_ID AND SHIPMENT_DATE IS NULL AND VENDOR_ID = SUPPLIED_ID;
//      Checks to see if a reorder requests exists for signed-in vendor with a given id
// 07: SELECT 1 FROM VENDORS WHERE VENDOR_ID = SUPPLIED_ID AND SHIPMENT_DATE IS NULL;
//      Checks to see if a vendor exists with a given id

/**
 * Main driver program for the vendor application.
 *
 * @author Imris S. Curry
 */
public class VendorAccess {

    class Access{

        private String vendorID;

        public Access(String VendorID) {
            this.vendorID = VendorID;
        }

        /**
         * Show a vendor all open reorder requests and handle their interactions
         *
         */
        public void mainVendorAccess() {
            System.out.println("Open Reorder Requests:");

            String query = "SELECT * FROM REORDER_REQUEST WHERE VENDOR_ID = " + vendorID +
                    " AND SHIPMENT_DATE IS NULL;";

            try {
                Statement stmt = conn.createStatement();
                ResultSet res = stmt.executeQuery(query);
                while (res.next()) {
                    int requestID = res.getInt("REQUEST_ID");
                    String upc_14 = res.getString("UPC_14");
                    int store = res.getInt("STORE_ID");
                    int quantity = res.getInt("QUANTITY");
                    System.out.println("\tRequest ID: " + requestID +
                            "\n\tProduct ID: " + upc_14 + "\n\tStore: " + store +
                            "\n\tQuantity: " + quantity + "\n");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            System.out.println("Commands:");
            showVendorCommands();

            for(;;){
                System.out.print(">> ");
                String userInpt = in.nextLine();
                String[] userInptVals = userInpt.split(" ");

                switch (userInptVals[0]) {
                    case "1": {
                        // Search for reorder requests for a product

                        // Check if length is correct
                        if (userInptVals.length != 2) {
                            System.out.println("Expected <Product_ID>");
                            break;
                        }
                        // Check that input is a number
                        if (userInptVals[1].chars().allMatch(Character::isDigit) == false
                                || (userInptVals[1]).length() != 14) {
                            System.out.println("Please enter only 14-digit numeric product ID values");
                            break;
                        }
                        // Check if reorder request exists
                        if (!this.productReorderExists(userInptVals[1])) {
                            System.out.println("No open reorder requests for product ID: "
                                    + userInptVals[1]);
                            break;
                        }

                        requestsForProduct(userInptVals[1]);
                        break;
                    }
                    case "shipment": {
                        // Enter a shipment
                        System.out.println("Please enter shipment delivery date:\n<MM/DD/YYYY>");
                        System.out.print(">> ");
                        String inpt = in.nextLine();
                        while(true) {
                            // Confirm date format is legal
                            String regex = "(1[0-2]|0[1-9])/(3[01]|[12][0-9]|0[1-9])/[0-9]{4}$";
                            Pattern pattern = Pattern.compile(regex);
                            Matcher matcher = pattern.matcher((CharSequence) inpt);
                            if(matcher.matches() == false){
                                System.out.println("Please enter a valid date:\n<MM/DD/YYYY>");
                                System.out.print(">> ");
                                inpt = in.nextLine();
                                continue;
                            }

                            // Confirm date is after or on today's date
                            Date today = new Date();
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(today);
                            int currentDay = cal.get(Calendar.DAY_OF_MONTH);
                            int currentMonth = cal.get(Calendar.MONTH)+1;
                            int currentYear = cal.get(Calendar.YEAR);
                            String[] inputDate = inpt.split("/");

                            if(Integer.parseInt(inputDate[2]) < currentYear) {
                                System.out.println("Please enter a future date:\n<MM/DD/YYYY>");
                            }else if(Integer.parseInt(inputDate[2]) == currentYear){
                                if(Integer.parseInt(inputDate[0]) < currentMonth) {
                                    System.out.println("Please enter a future date:\n<MM/DD/YYYY>");
                                }else if(Integer.parseInt(inputDate[2]) == currentMonth){
                                    if(Integer.parseInt(inputDate[1]) < currentDay) {
                                        System.out.println("Please enter a future date:\n<MM/DD/YYYY>");
                                    }else{
                                        break;
                                    }
                                }else{
                                    break;
                                }
                            }else{
                                break;
                            }
                            System.out.print(">> ");
                            inpt = in.nextLine();
                        }
                        String[] inputDate = inpt.split("/");
                        enterShipment(Integer.parseInt(inputDate[2]),Integer.parseInt(inputDate[0]),
                                Integer.parseInt(inputDate[1]));
                        break;
                    }
                    case "all": {
                        // See all open reorder requests for signed-in vendor
                        System.out.println("Open Reorder Requests:");

                        String inner_query = "SELECT * FROM REORDER_REQUEST WHERE VENDOR_ID = " + vendorID +
                                " AND SHIPMENT_DATE IS NULL;";

                        try {
                            Statement stmt = conn.createStatement();
                            ResultSet res = stmt.executeQuery(inner_query);
                            while (res.next()) {
                                int requestID = res.getInt("REQUEST_ID");
                                String upc_14 = res.getString("UPC_14");
                                int store = res.getInt("STORE_ID");
                                int quantity = res.getInt("QUANTITY");
                                System.out.println("\tRequest ID: " + requestID +
                                        "\n\tProduct ID: " + upc_14 + "\n\tStore: " + store +
                                        "\n\tQuantity: " + quantity + "\n");
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case "help": {
                        // Get a list of instructions
                        showVendorCommands();
                        break;
                    }
                    case "exit": {
                        // Exit
                        closeConnection();
                        System.exit(0);
                    }
                    default: {
                        // Command not recognized
                        System.out.println("Invalid command");
                        break;
                    }
                }
            }

        }

        /**
         * Displays listing and description of commands to the user.
         */
        private void showVendorCommands() {
            System.out.println("    '1 <Product_ID>' - Search a product");
            System.out.println("    'shipment' - Enter a shipment");
            System.out.println("    'all' - See all open reorder requests");
            System.out.println("    'help' - Instructions");
            System.out.println("    'exit' - Exit");
        }

        /**
         * Process a vendor's new shipment by updating given reorder requests
         *
         * @param year  The shipment year
         * @param month The shipment month
         * @param day   The shipment day
         */
        public void enterShipment(int year, int month, int day) {

            System.out.println("Please enter reorder request IDs that are filled by this shipment,\n'enter' to submit:\n"
                    + "<Reorder_ID>");
            ArrayList<String> requests = new ArrayList<String>();

            for(;;) {
                System.out.print("[Shipment]>> ");
                String userInpt = in.nextLine();

                if(userInpt.equals("enter")) {
                    System.out.println("Requests closed with shipment date " + month + "/" + day + "/" + year +":");
                    requests.forEach((n) -> System.out.println(n));
                    break;
                } else {
                    if (userInpt.chars().allMatch(Character::isDigit) == false) {
                        System.out.println("Please enter only numeric reorder request ID values:");
                    }else if(reorderExists(userInpt)) {
                        // Update reorder request to add shipment date (closing request)
                        Calendar cal = Calendar.getInstance();
                        cal.set(year, month-1, day);
                        Timestamp timestamp = new java.sql.Timestamp(cal.getTimeInMillis());
                        try {
                            PreparedStatement pstmt = conn.prepareStatement(
                                    "UPDATE REORDER_REQUEST SET SHIPMENT_DATE = ? WHERE REQUEST_ID = ?;");

                            pstmt.setTimestamp(1, timestamp);
                            pstmt.setInt(2, Integer.parseInt(userInpt));

                            pstmt.executeUpdate();
                            requests.add(userInpt);
                        } catch(SQLException e){
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Reorder request " + userInpt + " does not exist or is closed.");
                    }
                }
            }
        }

        /**
         * Prints all open reorder requests for a given product id
         *
         * @param upc14 The id of the product to print requests for
         */
        public void requestsForProduct(String upc14) {
            System.out.println("Product ID: " + upc14);

            String query1 = "SELECT * FROM REORDER_REQUEST WHERE UPC_14 = " + upc14 + " AND SHIPMENT_DATE IS NULL" +
                    " AND VENDOR_ID = " + vendorID + ";";
            String query2 = "SELECT COUNT(REQUEST_ID) FROM REORDER_REQUEST WHERE UPC_14 = " + upc14 +
                    " AND SHIPMENT_DATE IS NULL AND VENDOR_ID = " + vendorID + ";";

            try {
                Statement stmt1 = conn.createStatement();
                ResultSet res1 = stmt1.executeQuery(query1);
                Statement stmt2 = conn.createStatement();
                ResultSet res2 = stmt2.executeQuery(query2);
                if (res2.next()) {
                    int count = res2.getInt("COUNT(REQUEST_ID)");
                    System.out.println("Open Requests: " + count);
                }
                while (res1.next()) {
                    int requestID = res1.getInt("REQUEST_ID");
                    int store = res1.getInt("STORE_ID");
                    int quantity = res1.getInt("QUANTITY");
                    System.out.println("\tRequest ID: " + requestID + "\n\tStore: " + store +
                            "\n\tQuantity: " + quantity + "\n");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        /**
         * Check if open reorder requests for a product by the signed-in vendor exist in the database
         *
         * @param productID The id of the product to be checked
         * @return True if a request is found, False otherwise
         */
        private Boolean productReorderExists(String productID) {
            String query = "SELECT 1 FROM REORDER_REQUEST WHERE UPC_14 = " + productID + " AND SHIPMENT_DATE IS NULL" +
                    " AND VENDOR_ID = " + vendorID + ";";
            int nrows = 0;

            try {
                Statement stmt = conn.createStatement();
                ResultSet res = stmt.executeQuery(query);
                res.last();
                nrows = res.getRow();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return nrows == 1;
        }

        /**
         * Checks if a reorder request exists within the database
         *
         * @param requestID The id of the reorder request to be checked
         * @return True if request is found, False otherwise
         */
        private Boolean reorderExists(String requestID) {
            String query = "SELECT 1 FROM REORDER_REQUEST WHERE REQUEST_ID = " + requestID +
                    " AND SHIPMENT_DATE IS NULL AND VENDOR_ID = " + vendorID + ";";
            int nrows = 0;

            try {
                Statement stmt = conn.createStatement();
                ResultSet res = stmt.executeQuery(query);
                res.last();
                nrows = res.getRow();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return nrows == 1;
        }
    }

    Scanner in;
    Connection conn;

    public VendorAccess(String location,
                        String user,
                        String password) {

        String url = "jdbc:h2:" + location + ";IFEXISTS=TRUE";

        try {
            this.conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected: " + url);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        this.in = new Scanner(System.in);
    }

    /**
     * Used on program exit, closes the database.
     */
    public void closeConnection() {
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
    public String[] mainInptLoop() {
        for (; ; ) {
            System.out.print(">> ");
            String userInpt = this.in.nextLine();

            String[] userInptVals = userInpt.split(" ");
            if (userInptVals.length == 0) break;

            switch (userInptVals[0]) {
                case "1": {
                    // Sign in as a Vendor

                    // Check if length is correct
                    if (userInptVals.length != 2) {
                        System.out.println("Expected '1 <VendorID>'");
                        break;
                    }

                    if (!this.vendorExists(userInptVals[1])) {
                        System.out.println("Cannot find Vendor with ID: " + userInptVals[1]);
                        break;
                    }

                    return userInptVals;
                }
                case "help": {
                    // Get a list of instructions
                    showCommands();
                    break;
                }
                case "exit": {
                    // Exit
                    this.closeConnection();
                    System.exit(0);
                }
                default: {
                    // Command not recognized
                    System.out.println("Invalid command.");
                    break;
                }
            }
        }

        return null;
    }

    /**
     * Checks if vendor exists within the database
     *
     * @param vendorID The id of the vendor to be checked
     * @return True if vendor is found, False otherwise
     */
    private Boolean vendorExists(String vendorID) {

        String query = "SELECT 1 FROM VENDORS WHERE VENDOR_ID = " + vendorID + ";";
        int nrows = 0;

        try {
            Statement stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery(query);
            res.last();
            nrows = res.getRow();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return nrows == 1;
    }

    /**
     * Displays listing and description of commands to the user.
     */
    private void showCommands() {
        System.out.println("    '1 <VendorID>' - Sign in");
        System.out.println("    'exit' - Quits the program");
        System.out.println("    'help' - Shows help message");
    }

    public static void main(String[] args) {

        VendorAccess mainObj = new VendorAccess("./database/db", "user", "password");

        System.out.println("Commands:");
        System.out.println("    '1 <VendorID>' - Sign in");
        System.out.println("    'exit' - Quits the program");
        System.out.println("    'help' - Shows help message");

        for (; ; ) {
            String[] res = mainObj.mainInptLoop();

            if (res[0].equals("1")) {
                VendorAccess.Access access;
                access = mainObj.new Access(res[1]);

                access.mainVendorAccess();
            }
        }
    }
}

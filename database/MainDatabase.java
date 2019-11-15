package database;

import java.sql.*;
import org.h2.api.Trigger;

/**
 * Main database program which creates and fills the initial tables
 * 
 * @author Dylan R. Wagner
 * 
 */
public class MainDatabase{

    private Connection conn;

    /**
     * MainDatabase constructor, creates the actual database.
     * @param location The location of the database supplied by the user
     * @param user username
     * @param password password for user
     */
    public MainDatabase(String location, 
                        String user, 
                        String password){
        
        String url = "jdbc:h2:" + location;

        try{
            this.conn = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
			e.printStackTrace();
		}
                                               
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
     * Creates, defines and executes SQL command to create and fill a table from a csv file.
     * @param tableName Name of the table to be created
     * @param csvPath Path to csv source file
     * @param SQLTableDef Definitions on fields within the table
     */
    public void createFillExecute(String tableName, String csvPath, String SQLTableDef){

        StringBuilder createStrngBuilder = new StringBuilder("CREATE TABLE ");
        createStrngBuilder.append(tableName);
        createStrngBuilder.append("(" + SQLTableDef + ") ");
        createStrngBuilder.append("AS SELECT * FROM CSVREAD('" + csvPath + "');");

        System.out.println(createStrngBuilder.toString() + "\n");

        try{

            Statement stmt = conn.createStatement();
            Boolean status = stmt.execute(createStrngBuilder.toString());

        } catch(SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * Adds a foreign key to a table
     * @param table table to be modified
     * @param fieldName Field to be made a foreign key
     * @param foreignTable Name of the ref table
     * @param foreignField Name of the ref field in the foreign table
     */
    public void addForeignKey(String table,
                         String fieldName,
                         String foreignTable,
                         String foreignField)
    {
        StringBuilder createStrngBuilder = new StringBuilder("ALTER TABLE ");
        createStrngBuilder.append(table);
        createStrngBuilder.append(" ADD FOREIGN KEY (");
        createStrngBuilder.append(fieldName);
        createStrngBuilder.append(") REFERENCES ");
        createStrngBuilder.append(foreignTable + "(");
        createStrngBuilder.append(foreignField + ");");

        System.out.println(createStrngBuilder.toString() + "\n");

        try{
            Statement stmt = conn.createStatement();
            Boolean status = stmt.execute(createStrngBuilder.toString());

        } catch(SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * Just executes any trigger string provided.
     * 
     * @param cmnd The trigger sql
     */
    public void addTrigger(String cmnd){
        try {
            Statement stmt = conn.createStatement();
            stmt.execute(cmnd);

            System.out.println(cmnd);
        } catch(SQLException e){
            e.printStackTrace();
        }
    }

    public static class RestockTrigger implements Trigger {

        /**
         * Initializes the trigger.
         *
         * @param conn a connection to the database
         * @param schemaName the name of the schema
         * @param triggerName the name of the trigger used in the CREATE TRIGGER
         *            statement
         * @param tableName the name of the table
         * @param before whether the fire method is called before or after the
         *            operation is performed
         * @param type the operation type: INSERT, UPDATE, or DELETE
         */
        @Override
        public void init(Connection conn, String schemaName,
                String triggerName, String tableName, boolean before, int type) {
            // initialize the trigger object is necessary
        }

        /**
         * Method to be executed on update of a row within the PROD_STORE table.
         * 
         * @param conn The connection to the db
         * @param oldRow Before update
         * @param newRow After update
         * @throws SQLException
         */
        @Override
        public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
            if ((Integer) newRow[3] <= 0) {
                PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO REORDER_REQUEST VALUES(?, ?, ?, ?, ?, ?);");

                stmt.setObject(1, null); // New key
                stmt.setObject(2, null); // Shipment Date
                stmt.setObject(3, newRow[2]); // Store ID
                stmt.setObject(4, null); // Vendor ID TODO
                stmt.setObject(5, newRow[0]); // UPC14
                stmt.setInt(6, (int) (Math.random() * 1000)); // New Inv
                
                stmt.execute();
            }
        }

        @Override
        public void close() {
            // ignore
        }

        @Override
        public void remove() {
            // ignore
        }
    }

    public static void main(String[] args){

        // Uses dummy user and password
        // USERNAME: user PASSWORD: password
        MainDatabase mainDB = new MainDatabase("./database/db", "user", "password");

        // CUSTOMER table - Tracks customers
        mainDB.createFillExecute("CUSTOMER", "./data/customer.csv",
        "Customer_ID INT PRIMARY KEY auto_increment, FirstName VARCHAR(255), LastName VARCHAR(255), Phone CHAR(15)");

        // STORES table - Tracks stores
        mainDB.createFillExecute("STORES", "./data/stores.csv",
        "Store_ID INT PRIMARY KEY auto_increment, Address VARCHAR(255), City VARCHAR(255), State VARCHAR(255), Zip CHAR(5)");

        // ITEMS table - Basic items
        mainDB.createFillExecute("ITEMS", "./data/items.csv",
        "upc14 CHAR(14) PRIMARY KEY, brand_id INT, name VARCHAR(255)");

        // BRANDS table - Tracks brands
        mainDB.createFillExecute("BRANDS", "./data/brands.csv",
        "brand_id INT PRIMARY KEY auto_increment, Name VARCHAR(255), No_of_items INT, Manufacturer VARCHAR(255)");

        // VENDORS table - Tracks vendors
        mainDB.createFillExecute("VENDORS", "./data/vendors.csv",
        "vendor_id INT PRIMARY KEY auto_increment, name VARCHAR(255)");

        // BRAND_DIS table - Connects brands to distributors 
        mainDB.createFillExecute("BRAND_DIS", "./data/brand_distribution.csv",
        "vendor_id INT, brand_id INT, PRIMARY KEY(vendor_id, brand_id)");

        // BEVERAGE table - Beverage items
        mainDB.createFillExecute("BEVERAGE", "./data/beverage.csv",
        "upc14 CHAR(14) PRIMARY KEY, brand_id INT, name VARCHAR(255), calories INT, storage VARCHAR(255), " + 
        "allergens VARCHAR(255), weight INT, alcoholic BOOLEAN");

        // FOODS table - Food items
        mainDB.createFillExecute("FOODS", "./data/foods.csv",
        "upc14 CHAR(14) PRIMARY KEY, brand_id INT, name VARCHAR(255), calories INT, storage VARCHAR(255)," + 
        " allergens VARCHAR(255), weight INT");

        // PHARMA table - Pharma items
        mainDB.createFillExecute("PHARMA", "./data/pharma.csv",
        "upc14 CHAR(14) PRIMARY KEY, brand_id INT, name VARCHAR(255), otc BOOLEAN");

        // TRANSACTIONS table
        mainDB.createFillExecute("TRANSACTIONS", "./data/transaction.csv",
        "Transaction_ID LONG auto_increment, TimeStamp TIMESTAMP, Store_ID INT, Customer_ID INT, PRIMARY KEY(Transaction_ID, TimeStamp, Customer_ID)");

        mainDB.createFillExecute("PROD_TRANSACTIONS", "./data/prod_transaction.csv",
        "Transaction_ID LONG, upc14 CHAR(14), quantity INT, PRIMARY KEY(Transaction_ID, upc14)");        

        // PRODSTORE table - Relates products to individual stores 
        mainDB.createFillExecute("PROD_STORE", "./data/prod_store.csv",
        "upc14 CHAR(14), tbl_enum INT, store_id INT, inventory INT, price DECIMAL(7,2), PRIMARY KEY(upc14, store_id)");

        mainDB.createFillExecute("REORDER_REQUEST", "./data/reorder_req.csv",
        "request_ID INT PRIMARY KEY auto_increment, shipment_date TIMESTAMP, store_ID INT, vendor_ID INT, upc_14 CHAR(14), quantity INT");

        // Add foreign keys

        mainDB.addForeignKey("ITEMS", "brand_id", "BRANDS", "brand_id");

        mainDB.addForeignKey("BRAND_DIS", "brand_id", "BRANDS", "brand_id");
        mainDB.addForeignKey("BRAND_DIS", "vendor_id", "VENDORS", "vendor_id");

        mainDB.addForeignKey("BEVERAGE", "brand_id", "BRANDS", "brand_id");

        mainDB.addForeignKey("FOODS", "brand_id", "BRANDS", "brand_id");

        mainDB.addForeignKey("PHARMA", "brand_id", "BRANDS", "brand_id");

        mainDB.addForeignKey("TRANSACTIONS", "Store_ID", "STORES", "Store_ID");
        mainDB.addForeignKey("TRANSACTIONS", "Customer_ID", "CUSTOMER", "Customer_ID");

        mainDB.addForeignKey("PROD_TRANSACTIONS", "Transaction_ID", "TRANSACTIONS", "Transaction_ID");

        mainDB.addForeignKey("PROD_STORE", "store_id", "STORES", "Store_ID");

        mainDB.addForeignKey("REORDER_REQUEST", "store_ID", "STORES", "Store_ID");
        mainDB.addForeignKey("REORDER_REQUEST", "vendor_ID", "VENDORS", "vendor_id");

        mainDB.addTrigger("CREATE TRIGGER INV_RESTOCK AFTER UPDATE ON PROD_STORE " +
                          "FOR EACH ROW CALL \"" + MainDatabase.RestockTrigger.class.getName() + "\"");

        // Done
        mainDB.closeConnection();
    }
}

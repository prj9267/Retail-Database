package apps;

import java.sql.*;
import java.util.Scanner;

public class AdminApp {

    private Scanner in;
    private Connection conn;

    private AdminApp(String location, String user, String password){
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
    private void closeConnection(){
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void runApplication(){

        System.out.println("|Access the Retail Database through this application. |");
        System.out.println("|Accepted commands are EXIT and SQL queries.          |");
        while(true) {
            System.out.print(">> ");
            String input = in.nextLine();

            if(input.isEmpty()) System.out.print("");

            else if(input.equalsIgnoreCase("EXIT")){
                exitProgram();
            }
            else {
                try{
                    PreparedStatement stmt = conn.prepareStatement(input);
                    ResultSet res = stmt.executeQuery();
                    ResultSetMetaData metaData = res.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    if (res.next()){
                        System.out.println("|Query Success                                        |");
                        if(input.substring(0,6).equalsIgnoreCase("SELECT")){

                            for (int i = 1; i <= columnCount ; i++){
                                String col_name = metaData.getColumnName(i);
                                System.out.print(col_name + "|");
                            }
                            while(res.next()){
                                System.out.printf("%n");
                                for(int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                                    System.out.print(res.getString(columnIndex) + "|");
                                }
                            }
                            System.out.print("\n");

                        }
                    }
                } catch(SQLException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private void exitProgram(){
        this.closeConnection();
        System.exit(0);
    }

    public static void main(String[] args){
        AdminApp app = new AdminApp("./database/db", "user", "password");
        app.runApplication();
    }
}

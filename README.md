# CSCI320

Retail database project for CSCI320 - Principles of Data Management

#### Repository Contents

* ***/aux*** - Project tools
* ***/data*** - Contains all of the project sample data
* ***/database*** - Contains the database creation and initial insertion code
* ***/apps*** - Applications which interact with the database

#### Database

Database file is stored as database/db.mv.db

Compile: `javac -cp .:PATH_TO/h2-1.4.199.jar database/MainDatabase.java` 

Usage: `java -cp .:PATH_TO/h2-1.4.199.jar database.MainDatabase`

Ex: `java -cp .:/Users/dylanwagner/java/h2/bin/h2-1.4.199.jar database.MainDatabase`

#### Apps

##### CounterPoint

Interface for transaction and restock functionality.

Compile: `javac -cp .:PATH_TO/h2-1.4.199.jar apps/CounterPoint.java`

Usage: `java -cp .:PATH_TO/h2-1.4.199.jar apps.CounterPoint`

##### VendorAccess

Interface for vendors to view reorder requests for their products and close requests be entering shipments.

Compile: `javac -cp .:PATH_TO/h2-1.4.199.jar apps/VendorAccess.java`

Usage: `java -cp .:PATH_TO/h2-1.4.199.jar apps.VendorAccess`

##### CustomerApp

Interface for customers to create customer accounts, view personal transaction history, and gather information about products.

Compile: `javac -cp .:PATH_TO/h2-1.4.199.jar apps/CustomerApp.java`

Usage: `java -cp .:PATH_TO/h2-1.4.199.jar apps.CustomerApp`

##### AdminApp

Interface for the database administrator to use SQL via the command line to interact with the database.

Compile: `javac -cp .:PATH_TO/h2-1.4.199.jar apps/CustomerApp.java`

Usage: `java -cp .:PATH_TO/h2-1.4.199.jar apps.CustomerApp`

# CSCI320

Retail database applications for CSCI320 - Principles of Data Management

##### CounterPoint

Interface for transaction and restock functionality.

# Commands:

CounterPoint
    exit - Quits the program
    help - Shows help message
    trans <StoreID> <CustomerID> - Enters transaction mode
    restock <StoreID> - Enters restock mode
    
Transaction Module
    exit - Quits program
    help - Shows help message
    enter - Submits transaction
    cancel - Cancels current transaction
    show - Displays current transaction
    add <upc14> <QTY> - Adds this item with quantity to the transaction
    remove <upc14> [QTY] - Removes the item with quantity from transaction, QTY can be omitted
    
Restock Module
    exit - Quits program
    help - Shows help message
    cancel - Cancels current transaction
    restock - Restocks all inventory

##### VendorAccess

Interface for vendors to view reorder requests for their products and close requests be entering shipments.

# Commands:

VendorAccess
    1 <VendorID> - Signs in as a vendor
    help - Shows help message
    exit - Quits the program
    
Access Module
   1 <ProductID> - Searches requests for a given product
    shipment - Enters a shipment
    all - Shows all open reorder requests
    help - Shows help message
    exit - Quits the program

##### CustomerApp

Interface for customers to create customer accounts, view personal transaction history, and gather information about products.

# Commands: 

Customer Application
    <Customer_ID> - If valid, options for information retrieval become available
    NEW - Customer creation begins.

Customer Creation
    <first name> <last name> <phone number (XXX-XXX-XXXX)> - creates customer with inputted information and unique ID.

Information Retrieval
    1 - Displays transaction history for that customer.
    2 <UPC_14> - Displays list of stores that sell the product.
    3 <Store_ID> - Displays information specific to the store.
    4 <Store_ID> - Displays the most sold product at the store.
    5 <UPC_14> <Store_ID> - Displays the inventory of the product at the store.
    6 <UPC_14> - Displays the information specific to that product.
    EXIT - Terminates the program
    HELP - Re-displays the options.

##### AdminApp

Interface for the database administrator to use SQL via the command line to interact with the database.

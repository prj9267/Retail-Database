# file: product_store_gen.r
# author: Dylan R. Wagner
# Desc:
#       Generates product store relation data for use in 
#       the database.
#

library(data.table)

MAX_PRICE <- 50
MAX_INV <- 1000

# Read in the source store data
stores_data <- fread("../data/stores.csv")[, "Store_ID"]
items_source <- c("../data/items.csv", "../data/foods.csv", "../data/beverage.csv", "../data/pharma.csv")

item_enum_cnt <- 0
read_items <- function(path){
    dta <- fread(path, select=c("upc14"), colClasses=c(upc14="character"))
    dta <- dta[, tbl_enum := item_enum_cnt]
    item_enum_cnt <<- item_enum_cnt + 1

    dta
}

# Read in all item files then combine them
l <- lapply(items_source, read_items)
items_data <- unique(rbindlist(l))
setkey(items_data, upc14)

# gen_tuples: Generates random inventory sample per store
# Args:
# - store_id: Used to link the inventory to a store
#
# Return: the newly created sample with structure:
#   upc14,store_ID,inventory,price
#
gen_tuples <- function(store_id) {
    # Generates random sequence of random length over all items
    rand_seq <- sample(seq(from = 0, to = nrow(items_data)), size=sample(1:nrow(items_data), 1))
    # Subset the items space
    items_rand <- items_data[rand_seq, ] 
    # Generate additional attributes and add in the store id
    items_rand <- items_rand[ ,c("store_id", "inventory", "price") := list(store_id, sample(1:MAX_INV, nrow(items_rand)), sample(seq(from = 1, to = MAX_PRICE, by=0.01), nrow(items_rand)))]
    
    # Return the new sample
    items_rand 
}

# Create sample for each store
rand_expand_data_lst <- lapply(stores_data[[1]], gen_tuples)
rand_expand_data <- rbindlist(rand_expand_data_lst)

fwrite(rand_expand_data, file="../data/prod_store.csv")
// database.h
#ifndef DATABASE_H
#define DATABASE_H

#include <time.h>

#define NAME_LEN    100
#define DATE_LEN    11  /* YYYY-MM-DD plus null */

typedef struct Product {
    int   product_id;
    char  name[NAME_LEN];
    float price;
} Product;

typedef struct Bill {
    int      bill_id;
    char     bill_date[DATE_LEN];    /* "YYYY-MM-DD" */
    Product *products;
    int      product_count;
} Bill;

typedef struct Client {
    char     client_name[NAME_LEN];  /* key */
    Bill    *bills;
    int      bill_count;
    time_t   date_last_mod;
} Client;

typedef struct Database {
    Client *clients;
    int     size;
    int     capacity;
} Database;

/* Database lifecycle */
Database* db_create(void);
void      db_free(Database* db);

/* Client operations by name */
int       client_index_by_name(const Database* db, const char* client_name);
void      create_client(Database* db, const char* client_name);
Client*   db_lookup_by_name(Database* db, const char* client_name);

/* Bill operations */
void      add_bill(Database* db, int client_index, int bill_id, const char* bill_date);
int       bill_index_by_date(const Client* c, const char* bill_date);
Bill*     client_bill_by_date(Client* c, const char* bill_date);

/* Product operations */
void      add_product(Database* db,
                      int client_index,
                      int bill_index,
                      int product_id,
                      const char* product_name,
                      float price);

/* CSV I/O */
void      write_bill_csv(const Client* c, int bill_index, const char* path);

/* Printing */
void      print_product(const Product* p);
void      print_bill(const Bill* b);
void      print_client_bills(const Client* c);
void      print_database(const Database* db);

#endif /* DATABASE_H */

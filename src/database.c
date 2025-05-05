// database.c
#include "database.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

static void ensure_clients_capacity(Database* db) {
    if (db->size >= db->capacity) {
        int new_cap = db->capacity ? db->capacity * 2 : 4;
        Client* tmp = realloc(db->clients, new_cap * sizeof(Client));
        if (!tmp) {
            fprintf(stderr, "Failed to expand clients array\n");
            exit(EXIT_FAILURE);
        }
        db->clients = tmp;
        db->capacity = new_cap;
    }
}

Database* db_create(void) {
    Database* db = malloc(sizeof(Database));
    if (!db) {
        fprintf(stderr, "Failed to allocate Database\n");
        exit(EXIT_FAILURE);
    }
    db->clients = NULL;
    db->size     = 0;
    db->capacity = 0;
    return db;
}

void db_free(Database* db) {
    if (!db) return;
    for (int i = 0; i < db->size; i++) {
        Client* c = &db->clients[i];
        for (int j = 0; j < c->bill_count; j++) {
            free(c->bills[j].products);
        }
        free(c->bills);
    }
    free(db->clients);
    free(db);
}

int client_index_by_name(const Database* db, const char* client_name) {
    for (int i = 0; i < db->size; i++) {
        if (strcmp(db->clients[i].client_name, client_name) == 0)
            return i;
    }
    return -1;
}

void create_client(Database* db, const char* client_name) {
    if (client_index_by_name(db, client_name) >= 0) return;
    ensure_clients_capacity(db);
    Client* c = &db->clients[db->size++];
    strncpy(c->client_name, client_name, NAME_LEN-1);
    c->client_name[NAME_LEN-1] = '\0';
    c->bills         = NULL;
    c->bill_count    = 0;
    c->date_last_mod = time(NULL);
}

Client* db_lookup_by_name(Database* db, const char* client_name) {
    int idx = client_index_by_name(db, client_name);
    return idx >= 0 ? &db->clients[idx] : NULL;
}

void add_bill(Database* db, int client_index, int bill_id, const char* bill_date) {
    Client* c = &db->clients[client_index];
    int new_count = c->bill_count + 1;
    Bill* tmp = realloc(c->bills, new_count * sizeof(Bill));
    if (!tmp) {
        fprintf(stderr, "Failed to expand bills\n");
        exit(EXIT_FAILURE);
    }
    c->bills = tmp;
    Bill* b = &c->bills[c->bill_count++];
    b->bill_id       = bill_id;
    strncpy(b->bill_date, bill_date, DATE_LEN-1);
    b->bill_date[DATE_LEN-1] = '\0';
    b->products      = NULL;
    b->product_count = 0;
    c->date_last_mod = time(NULL);
}

int bill_index_by_date(const Client* c, const char* bill_date) {
    for (int i = 0; i < c->bill_count; i++) {
        if (strcmp(c->bills[i].bill_date, bill_date) == 0)
            return i;
    }
    return -1;
}

Bill* client_bill_by_date(Client* c, const char* bill_date) {
    int idx = bill_index_by_date(c, bill_date);
    return idx >= 0 ? &c->bills[idx] : NULL;
}

void add_product(Database* db, int client_index, int bill_index, int product_id, const char* product_name, float price) {
    Bill* b = &db->clients[client_index].bills[bill_index];
    int new_count = b->product_count + 1;
    Product* tmp = realloc(b->products, new_count * sizeof(Product));
    if (!tmp) {
        fprintf(stderr, "Failed to expand products\n");
        exit(EXIT_FAILURE);
    }
    b->products = tmp;
    Product* p = &b->products[b->product_count++];
    p->product_id = product_id;
    strncpy(p->name, product_name, NAME_LEN-1);
    p->name[NAME_LEN-1] = '\0';
    p->price      = price;
    db->clients[client_index].date_last_mod = time(NULL);
}

void write_bill_csv(const Client* c, int bill_index, const char* path) {
    FILE* fp = fopen(path, "w");
    if (!fp) {
        fprintf(stderr, "Error opening %s\n", path);
        return;
    }
    Bill* b = &c->bills[bill_index];
    for (int i = 0; i < b->product_count; i++) {
        Product* p = &b->products[i];
        fprintf(fp, "%s,%d,%s,%d,%s,%.2f\n",
                c->client_name,
                b->bill_id,
                b->bill_date,
                p->product_id,
                p->name,
                p->price);
    }
    fclose(fp);
}

void print_product(const Product* p) {
    printf("    Product %d: %-20s $%.2f\n",
           p->product_id, p->name, p->price);
}

void print_bill(const Bill* b) {
    printf("  Bill %d (%s):\n", b->bill_id, b->bill_date);
    for (int i = 0; i < b->product_count; i++)
        print_product(&b->products[i]);
}

void print_client_bills(const Client* c) {
    printf("Client %s has %d bills:\n", c->client_name, c->bill_count);
    for (int i = 0; i < c->bill_count; i++)
        printf("  [%d] %s\n",
               c->bills[i].bill_id,
               c->bills[i].bill_date);
}

void print_database(const Database* db) {
    printf("=== Database: %d clients ===\n", db->size);
    for (int i = 0; i < db->size; i++)
        printf("- %s (Last mod: %ld)\n",
               db->clients[i].client_name,
               (long)db->clients[i].date_last_mod);
}

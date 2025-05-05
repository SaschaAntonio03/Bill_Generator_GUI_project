// main.c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// Absolute path to Homebrew’s Tcl/Tk headers—adjust if your prefix is different
#include <tcl.h>
#include <tk.h>

#include "database.h"

/* Forward declarations for the Tcl‐command callbacks */
static int Cmd_LogClient(Client *dbPtr, Tcl_Interp *interp, int objc, Tcl_Obj *const objv[]);
static int Cmd_AddBill(Client *dbPtr, Tcl_Interp *interp, int objc, Tcl_Obj *const objv[]);
static int Cmd_AddProduct(Client *dbPtr, Tcl_Interp *interp, int objc, Tcl_Obj *const objv[]);
static int Cmd_ShowDB(Client *dbPtr, Tcl_Interp *interp, int objc, Tcl_Obj *const objv[]);

int main(int argc, char **argv) {
    Tcl_Interp *interp = Tcl_CreateInterp();
    if (Tcl_Init(interp) == TCL_ERROR) {
        fprintf(stderr, "Tcl_Init error: %s\n", Tcl_GetStringResult(interp));
        return EXIT_FAILURE;
    }
    if (Tk_Init(interp) == TCL_ERROR) {
        fprintf(stderr, "Tk_Init error: %s\n", Tcl_GetStringResult(interp));
        return EXIT_FAILURE;
    }

    Database *db = db_create();

    /* Register Tcl commands */
    Tcl_CreateObjCommand(interp, "logClient",   (Tcl_ObjCmdProc*)Cmd_LogClient,   (Client*)db, NULL);
    Tcl_CreateObjCommand(interp, "addBill",      (Tcl_ObjCmdProc*)Cmd_AddBill,      (Client*)db, NULL);
    Tcl_CreateObjCommand(interp, "addProduct",   (Tcl_ObjCmdProc*)Cmd_AddProduct,   (Client*)db, NULL);
    Tcl_CreateObjCommand(interp, "showDB",       (Tcl_ObjCmdProc*)Cmd_ShowDB,       (Client*)db, NULL);

    /* Inline Tcl/Tk UI script */
    const char *script =
        "package require Tk\n"
        "wm title . \"Billing System\"\n"
        "button .log  -text \"Log Client\"   -command {set n [tk_dialog .d \"Client\" \"Name:\" OK]; if {$n ne {}} { logClient $n }}\n"
        "button .bill -text \"Add Bill\"     -command {set n [tk_dialog .d1 \"Client\" \"Name:\" OK]; set d [tk_dialog .d2 \"Date\" \"YYYY-MM-DD:\" OK]; if {$n ne {} && $d ne {}} { addBill $n $d }}\n"
        "button .prod -text \"Add Product\"  -command {set n [tk_dialog .d3 \"Client\" \"Name:\" OK]; set d [tk_dialog .d4 \"Bill Date\" \"YYYY-MM-DD:\" OK]; set p [tk_dialog .d5 \"Prod ID\" \"ID:\" OK]; set pn [tk_dialog .d6 \"Name\" \"Prod Name:\" OK]; set pr [tk_dialog .d7 \"Price\" \"Price:\" OK]; addProduct $n $d $p $pn $pr}\n"
        "button .show -text \"Show DB\"      -command { showDB }\n"
        "pack .log .bill .prod .show -side left -padx 5 -pady 5\n";

    if (Tcl_Eval(interp, script) == TCL_ERROR) {
        fprintf(stderr, "Tcl script error: %s\n", Tcl_GetStringResult(interp));
        return EXIT_FAILURE;
    }

    Tk_MainLoop();

    db_free(db);
    Tcl_DeleteInterp(interp);
    return EXIT_SUCCESS;
}

/* Tcl command callbacks */

static int Cmd_LogClient(Client *dbPtr, Tcl_Interp *interp, int objc, Tcl_Obj *const objv[]) {
    if (objc != 2) {
        Tcl_WrongNumArgs(interp, 1, objv, "clientName");
        return TCL_ERROR;
    }
    const char *name = Tcl_GetString(objv[1]);
    create_client((Database*)dbPtr, name);
    Tcl_SetResult(interp, "Client logged", TCL_STATIC);
    return TCL_OK;
}

static int Cmd_AddBill(Client *dbPtr, Tcl_Interp *interp, int objc, Tcl_Obj *const objv[]) {
    if (objc != 3) {
        Tcl_WrongNumArgs(interp, 1, objv, "clientName date");
        return TCL_ERROR;
    }
    const char *name = Tcl_GetString(objv[1]);
    const char *date = Tcl_GetString(objv[2]);
    int idx = client_index_by_name((Database*)dbPtr, name);
    if (idx < 0) { Tcl_SetResult(interp, "No such client", TCL_STATIC); return TCL_ERROR; }
    static int nextBill = 300;
    add_bill((Database*)dbPtr, idx, nextBill++, date);
    Tcl_SetResult(interp, "Bill added", TCL_STATIC);
    return TCL_OK;
}

static int Cmd_AddProduct(Client *dbPtr, Tcl_Interp *interp, int objc, Tcl_Obj *const objv[]) {
    if (objc != 6) {
        Tcl_WrongNumArgs(interp, 1, objv, "clientName date pid pname price");
        return TCL_ERROR;
    }
    const char *name   = Tcl_GetString(objv[1]);
    const char *date   = Tcl_GetString(objv[2]);
    int         pid    = atoi(Tcl_GetString(objv[3]));
    const char *pname  = Tcl_GetString(objv[4]);
    float       price  = atof(Tcl_GetString(objv[5]));

    int cidx = client_index_by_name((Database*)dbPtr, name);
    if (cidx < 0) { Tcl_SetResult(interp, "No such client", TCL_STATIC); return TCL_ERROR; }
    int bidx = bill_index_by_date(&((Database*)dbPtr)->clients[cidx], date);
    if (bidx < 0) { Tcl_SetResult(interp, "No such bill", TCL_STATIC); return TCL_ERROR; }

    add_product((Database*)dbPtr, cidx, bidx, pid, pname, price);
    Tcl_SetResult(interp, "Product added", TCL_STATIC);
    return TCL_OK;
}

static int Cmd_ShowDB(Client *dbPtr, Tcl_Interp *interp, int objc, Tcl_Obj *const objv[]) {
    char buf[4096] = {0};
    Database *db = (Database*)dbPtr;
    for (int i = 0; i < db->size; i++)
        sprintf(buf+strlen(buf), "%s\n", db->clients[i].client_name);
    Tcl_SetResult(interp, buf, TCL_VOLATILE);
    return TCL_OK;
}

package com.carddemo.service;

import com.carddemo.dto.MenuOption;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu service - migrated from COMEN01C (CM00 Main Menu) and COADM01C (Admin Menu).
 * Replaces the COMEN02Y copybook menu option data structure.
 */
@Service
public class MenuService {

    /**
     * Get menu options for regular users - migrated from COMEN02Y copybook.
     * Maps each COBOL program name to its REST endpoint URL.
     */
    public List<MenuOption> getUserMenuOptions() {
        List<MenuOption> options = new ArrayList<>();
        options.add(new MenuOption(1, "Account View", "COACTVWC", "U", "/accounts/view"));
        options.add(new MenuOption(2, "Account Update", "COACTUPC", "U", "/accounts/update"));
        options.add(new MenuOption(3, "Credit Card List", "COCRDLIC", "U", "/cards/list"));
        options.add(new MenuOption(4, "Credit Card View", "COCRDSLC", "U", "/cards/view"));
        options.add(new MenuOption(5, "Credit Card Update", "COCRDUPC", "U", "/cards/update"));
        options.add(new MenuOption(6, "Transaction List", "COTRN00C", "U", "/transactions/list"));
        options.add(new MenuOption(7, "Transaction View", "COTRN01C", "U", "/transactions/view"));
        options.add(new MenuOption(8, "Transaction Add", "COTRN02C", "U", "/transactions/add"));
        options.add(new MenuOption(9, "Transaction Reports", "CORPT00C", "U", "/reports"));
        options.add(new MenuOption(10, "Bill Payment", "COBIL00C", "U", "/billing/payment"));
        options.add(new MenuOption(11, "Pending Authorization View", "COPAUS0C", "U", "/authorizations/summary"));
        return options;
    }

    /**
     * Get admin menu options - includes user management and transaction type admin.
     */
    public List<MenuOption> getAdminMenuOptions() {
        List<MenuOption> options = getUserMenuOptions();
        options.add(new MenuOption(12, "List Users", "COUSR00C", "A", "/admin/users"));
        options.add(new MenuOption(13, "Add User", "COUSR01C", "A", "/admin/users/add"));
        options.add(new MenuOption(14, "Update User", "COUSR02C", "A", "/admin/users/update"));
        options.add(new MenuOption(15, "Delete User", "COUSR03C", "A", "/admin/users/delete"));
        options.add(new MenuOption(16, "Transaction Type List", "COTRTLIC", "A", "/admin/transaction-types"));
        options.add(new MenuOption(17, "Transaction Type Add/Edit", "COTRTUPC", "A", "/admin/transaction-types/edit"));
        return options;
    }
}

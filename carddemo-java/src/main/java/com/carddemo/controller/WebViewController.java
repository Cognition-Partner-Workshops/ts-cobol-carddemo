package com.carddemo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web view controller for Thymeleaf template routing.
 * Maps URL paths to Thymeleaf template names.
 * Replaces BMS map SEND MAP/RECEIVE MAP CICS commands with Spring MVC view resolution.
 */
@Controller
public class WebViewController {

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/menu")
    public String menuPage() {
        return "menu";
    }

    @GetMapping("/accounts/view")
    public String accountViewPage() {
        return "account-view";
    }

    @GetMapping("/accounts/update")
    public String accountUpdatePage() {
        return "account-update";
    }

    @GetMapping("/cards/list")
    public String cardListPage() {
        return "card-list";
    }

    @GetMapping("/cards/view")
    public String cardViewPage() {
        return "card-view";
    }

    @GetMapping("/cards/update")
    public String cardUpdatePage() {
        return "card-update";
    }

    @GetMapping("/transactions/list")
    public String transactionListPage() {
        return "transaction-list";
    }

    @GetMapping("/transactions/view")
    public String transactionViewPage() {
        return "transaction-view";
    }

    @GetMapping("/transactions/add")
    public String transactionAddPage() {
        return "transaction-add";
    }

    @GetMapping("/reports")
    public String reportsPage() {
        return "reports";
    }

    @GetMapping("/billing/payment")
    public String billPaymentPage() {
        return "bill-payment";
    }

    @GetMapping("/authorizations/summary")
    public String authSummaryPage() {
        return "authorization-summary";
    }

    @GetMapping("/authorizations/details")
    public String authDetailsPage() {
        return "authorization-details";
    }

    @GetMapping("/admin/users")
    public String adminUsersPage() {
        return "admin-users";
    }

    @GetMapping("/admin/users/add")
    public String adminUserAddPage() {
        return "admin-user-add";
    }

    @GetMapping("/admin/users/update")
    public String adminUserUpdatePage() {
        return "admin-user-update";
    }

    @GetMapping("/admin/transaction-types")
    public String adminTransactionTypesPage() {
        return "admin-transaction-types";
    }

    @GetMapping("/admin/transaction-types/edit")
    public String adminTransactionTypeEditPage() {
        return "admin-transaction-type-edit";
    }
}

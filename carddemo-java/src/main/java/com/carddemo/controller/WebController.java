package com.carddemo.controller;

import com.carddemo.dto.AccountViewDto;
import com.carddemo.dto.CardDto;
import com.carddemo.dto.TransactionDto;
import com.carddemo.dto.UserDto;
import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.TransactionType;
import com.carddemo.entity.User;
import com.carddemo.service.AccountService;
import com.carddemo.service.CardService;
import com.carddemo.service.TransactionService;
import com.carddemo.service.TransactionTypeService;
import com.carddemo.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class WebController {

    private final AccountService accountService;
    private final CardService cardService;
    private final TransactionService transactionService;
    private final UserService userService;
    private final TransactionTypeService transactionTypeService;

    public WebController(AccountService accountService,
                         CardService cardService,
                         TransactionService transactionService,
                         UserService userService,
                         TransactionTypeService transactionTypeService) {
        this.accountService = accountService;
        this.cardService = cardService;
        this.transactionService = transactionService;
        this.userService = userService;
        this.transactionTypeService = transactionTypeService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/menu")
    public String mainMenu(Authentication auth, Model model) {
        model.addAttribute("userId", auth.getName());
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        return "menu";
    }

    @GetMapping("/accounts/view")
    public String accountViewForm() {
        return "account-view";
    }

    @GetMapping("/accounts/view/{id}")
    public String viewAccount(@PathVariable("id") Long acctId, Model model) {
        AccountViewDto dto = accountService.getAccountView(acctId);
        model.addAttribute("account", dto);
        return "account-view";
    }

    @GetMapping("/accounts/update/{id}")
    public String accountUpdateForm(@PathVariable("id") Long acctId, Model model) {
        Account account = accountService.getAccount(acctId);
        model.addAttribute("account", account);
        return "account-update";
    }

    @PostMapping("/accounts/update/{id}")
    public String updateAccount(@PathVariable("id") Long acctId,
                                @ModelAttribute Account account,
                                RedirectAttributes redirectAttributes) {
        accountService.updateAccount(acctId, account);
        redirectAttributes.addFlashAttribute("message", "Account updated successfully");
        return "redirect:/accounts/view/" + acctId;
    }

    @GetMapping("/cards/list")
    public String listCards(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Card> cards = cardService.listCards(PageRequest.of(page, 10));
        model.addAttribute("cards", cards);
        return "card-list";
    }

    @GetMapping("/cards/view/{id}")
    public String viewCard(@PathVariable("id") String cardNum, Model model) {
        CardDto card = cardService.toDto(cardService.getCard(cardNum));
        model.addAttribute("card", card);
        return "card-view";
    }

    @GetMapping("/cards/update/{id}")
    public String cardUpdateForm(@PathVariable("id") String cardNum, Model model) {
        CardDto card = cardService.toDto(cardService.getCard(cardNum));
        model.addAttribute("card", card);
        return "card-update";
    }

    @PostMapping("/cards/update/{id}")
    public String updateCard(@PathVariable("id") String cardNum,
                             @ModelAttribute CardDto cardDto,
                             RedirectAttributes redirectAttributes) {
        cardService.updateCard(cardNum, cardDto);
        redirectAttributes.addFlashAttribute("message", "Card updated successfully");
        return "redirect:/cards/view/" + cardNum;
    }

    @GetMapping("/transactions/list")
    public String listTransactions(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(required = false) Long acctId,
                                   Model model) {
        Page<Transaction> transactions;
        if (acctId != null) {
            transactions = transactionService.listTransactionsByAccount(acctId, PageRequest.of(page, 10));
            model.addAttribute("acctId", acctId);
        } else {
            transactions = transactionService.listTransactions(PageRequest.of(page, 10));
        }
        model.addAttribute("transactions", transactions);
        return "transaction-list";
    }

    @GetMapping("/transactions/view/{id}")
    public String viewTransaction(@PathVariable("id") String tranId, Model model) {
        Transaction transaction = transactionService.getTransaction(tranId);
        model.addAttribute("transaction", transaction);
        return "transaction-view";
    }

    @GetMapping("/transactions/add")
    public String addTransactionForm(Model model) {
        model.addAttribute("transaction", new TransactionDto());
        List<TransactionType> types = transactionTypeService.listTransactionTypes();
        model.addAttribute("transactionTypes", types);
        return "transaction-add";
    }

    @PostMapping("/transactions/add")
    public String addTransaction(@ModelAttribute TransactionDto dto,
                                 RedirectAttributes redirectAttributes) {
        try {
            Transaction tran = transactionService.addTransaction(dto);
            redirectAttributes.addFlashAttribute("message",
                    "Transaction added successfully: " + tran.getTranId());
            return "redirect:/transactions/view/" + tran.getTranId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/transactions/add";
        }
    }

    @GetMapping("/reports")
    public String reportForm() {
        return "reports";
    }

    @GetMapping("/payments")
    public String paymentForm() {
        return "payments";
    }

    @GetMapping("/admin")
    public String adminMenu() {
        return "admin-menu";
    }

    @GetMapping("/admin/users")
    public String listUsers(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<User> users = userService.listUsers(PageRequest.of(page, 10));
        model.addAttribute("users", users);
        return "user-list";
    }

    @GetMapping("/admin/users/add")
    public String addUserForm(Model model) {
        model.addAttribute("user", new UserDto());
        return "user-add";
    }

    @PostMapping("/admin/users/add")
    public String addUser(@ModelAttribute UserDto dto, RedirectAttributes redirectAttributes) {
        try {
            userService.addUser(dto);
            redirectAttributes.addFlashAttribute("message", "User added successfully");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/add";
        }
    }

    @GetMapping("/admin/users/update/{id}")
    public String updateUserForm(@PathVariable("id") String userId, Model model) {
        User user = userService.getUser(userId);
        UserDto dto = new UserDto();
        dto.setUserId(user.getUserId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUserType(user.getUserType());
        model.addAttribute("user", dto);
        return "user-update";
    }

    @PostMapping("/admin/users/update/{id}")
    public String updateUser(@PathVariable("id") String userId,
                             @ModelAttribute UserDto dto,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(userId, dto);
            redirectAttributes.addFlashAttribute("message", "User updated successfully");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/update/" + userId;
        }
    }

    @PostMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable("id") String userId,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("message", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}

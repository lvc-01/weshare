package weshare.controller;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import weshare.model.Expense;
import weshare.model.Person;
import weshare.persistence.ExpenseDAO;
import weshare.server.ServiceRegistry;
import weshare.server.WeShareServer;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ExpensesController {
    private static void viewer(String filePath, Context context) {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);
        Collection<Expense> expenses = (expensesDAO.findExpensesForPerson(personLoggedIn)).
                stream().filter(expense -> !expense.isFullyPaidByOthers()).toList();

        MonetaryAmount totalAmount = Monetary.getDefaultAmountFactory()
                .setCurrency("ZAR").setNumber(0).create();

        for (Expense expense : expenses) {
            MonetaryAmount amount = expense.amountLessPaymentsReceived();
            if (amount != null) {
                totalAmount = totalAmount.add(amount);
            }
        }
        Map<String, Object> viewModel = Map.of("expenses", expenses, "totalAmount", totalAmount);
        context.render(filePath, viewModel);
    }

    public static final Handler view = context -> {
        viewer("expenses.html", context);
    };

    public static final Handler viewNewExpenses = context -> {
        viewer("newexpense.html", context);
    };

    public static final Handler addExpense = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);

        String description = context.formParam("description");
        String date = context.formParam("date");
        MonetaryAmount amount = Monetary.getDefaultAmountFactory().setCurrency("ZAR").
                setNumber(Double.parseDouble(context.formParam("amount"))).create();
        Expense e = new Expense(personLoggedIn, description, amount, LocalDate.parse(date));
        expensesDAO.save(e);
        context.redirect("/expenses");
    };
}

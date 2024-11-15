package weshare.server;

import io.javalin.http.Handler;
import weshare.controller.*;
import weshare.model.Expense;
import weshare.model.Person;
import weshare.persistence.ExpenseDAO;

import java.util.Collection;
import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public class Routes {
    public static final String LOGIN_PAGE = "/";
    public static final String LOGIN_ACTION = "/login.action";
    public static final String LOGOUT = "/logout";
    public static final String EXPENSES = "/expenses";
    public static final String NEW_EXPENSE = "/newexpense";
    public static final String RECEIVED_REQ = "/paymentrequests_received";
    public static final String SENT_REQ = "/paymentrequests_sent";
    public static final String ADD_EXPENSE = "/expense.action";
    public static final String NEW_REQ = "/paymentrequest";
    public static final String REQ = "/paymentrequest";
    public static final String PAYMENT = "/payment.action";

    public static void configure(WeShareServer server) {
        server.routes(() -> {
            post(LOGIN_ACTION,  PersonController.login);
            get(LOGOUT,         PersonController.logout);
            get(EXPENSES,           ExpensesController.view);
            get(NEW_EXPENSE,           ExpensesController.viewNewExpenses);
            get(RECEIVED_REQ,           RequestController.viewReceivedRequests);
            get(SENT_REQ,           RequestController.viewSentRequests);
            post(ADD_EXPENSE,  ExpensesController.addExpense);
            get(REQ,  RequestController.requestForm);
            post(NEW_REQ,  RequestController.addRequest);
            post(PAYMENT,  RequestController.payRequest);
        });
    }
}

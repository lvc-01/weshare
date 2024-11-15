package weshare.controller;

import io.javalin.http.Handler;
import weshare.model.Expense;
import weshare.model.PaymentRequest;
import weshare.model.Person;
import weshare.persistence.ExpenseDAO;
import weshare.persistence.PersonDAO;
import weshare.server.ServiceRegistry;
import weshare.server.WeShareServer;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.time.LocalDate;
import java.util.*;


public class RequestController {
    public static final Handler viewReceivedRequests = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);
        Collection<PaymentRequest> requestsReceived = expensesDAO.findPaymentRequestsReceived(personLoggedIn);
        MonetaryAmount totalAmount = Monetary.getDefaultAmountFactory()
                .setCurrency("ZAR").setNumber(0).create();
        for (PaymentRequest paymentRequest : requestsReceived) {
            MonetaryAmount amount = paymentRequest.getAmountToPay();
            if (amount != null) totalAmount = totalAmount.add(amount);
        }
    
        Map<String, Object> viewModel = Map.of(
                "paymentRequestsReceived", requestsReceived,
                "totalAmountOwed", totalAmount
        );
    
        context.render("paymentrequests_received.html", viewModel);
    };
    

    public static final Handler viewSentRequests = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);
        Collection<PaymentRequest> requestsSent = expensesDAO.findPaymentRequestsSent(personLoggedIn);
        
        MonetaryAmount totalAmount = Monetary.getDefaultAmountFactory()
                .setCurrency("ZAR").setNumber(0).create();

        for (PaymentRequest paymentRequest : requestsSent) {
            MonetaryAmount amount = paymentRequest.getAmountToPay();
            if (amount != null) totalAmount = totalAmount.add(amount);
        }

        Map<String, Object> viewModel = Map.of(
                "paymentRequestsSent", requestsSent,
                "totalAmount", totalAmount
        );

        context.render("paymentrequests_sent.html", viewModel);
    };

    public static final Handler requestForm = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        String idParam = context.queryParam("expenseId");
        Expense expense = null;
        Collection<PaymentRequest> paymentRequests = Collections.emptyList();
        UUID id = null;
        MonetaryAmount totalOwed = Monetary.getDefaultAmountFactory()
                .setCurrency("ZAR").setNumber(0).create();
    
        if (idParam != null) {
            id = UUID.fromString(idParam);
            Optional<Expense> optional = expensesDAO.get(id);
            
            if (optional.isPresent()) {
                expense = optional.get();
                paymentRequests = expense.listOfPaymentRequests();
    
                for (PaymentRequest paymentRequest : paymentRequests) {
                    MonetaryAmount amount = paymentRequest.getAmountToPay();
                    if (amount != null) totalOwed = totalOwed.add(amount);
                }
            }
        }
        
        Map<String, Object> viewModel = Map.of(
            "expense", expense,
            "previousRequests", paymentRequests,
            "expenseId", id, "totalOwed", totalOwed
        );
    
        context.render("paymentrequest.html", viewModel);
    };
    
    public static final Handler addRequest = context -> {
        PersonDAO personDAO = ServiceRegistry.lookup(PersonDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        UUID id;

        String idParam = context.formParam("expense_id");
        Expense expense;

        MonetaryAmount money = Monetary.getDefaultAmountFactory()
        .setCurrency("ZAR").setNumber(Double.parseDouble(context.formParam("amount"))).create();

        id = UUID.fromString(idParam);
        String location = "/paymentrequest?expenseId="+id;
        String email  = context.formParam("email");
        Optional<Person> optPerson = personDAO.findPersonByEmail(email);
        Person person;
        if (optPerson.isPresent()){
            person = optPerson.get();
            Optional<Expense> optional = expensesDAO.get(id);
            if (optional.isPresent()) {
                expense = optional.get();
                expense.requestPayment(person, money, LocalDate.parse(context.formParam("date")));
            }
        }

        context.redirect(location);
    };

    public static final Handler payRequest = context -> {
        ExpenseDAO expenseDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);
        String reqIdParam = context.formParam("requestId");
    
        if (reqIdParam != null) {
            UUID reqId = UUID.fromString(reqIdParam);
            Optional<PaymentRequest> optRequest = expenseDAO.findPaymentRequestsReceived(personLoggedIn)
                .stream().filter(request -> request.getId().equals(reqId)).findFirst();

            if (optRequest.isPresent()) {
                PaymentRequest request = optRequest.get();
                Expense expense = request.getExpense();
    
                if (!request.isPaid()) {
                    expense.payPaymentRequest(reqId, personLoggedIn, LocalDate.now());
    
                    Expense newExpense = new Expense(personLoggedIn,
                        expense.getDescription(),
                        request.getAmountToPay(),
                        LocalDate.now()
                    );
    
                    expenseDAO.save(newExpense);
                }
            }
        }
        context.redirect("/paymentrequests_received");
    };
}
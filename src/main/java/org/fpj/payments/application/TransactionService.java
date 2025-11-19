package org.fpj.payments.application;

import org.fpj.Exceptions.TransactionException;
import org.fpj.payments.domain.*;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.fpj.Data.UiHelpers.parseAmountTolerant;
import static org.fpj.Data.UiHelpers.safe;
import static org.fpj.payments.domain.TransactionType.*;

//TODO Bei Überweisungen sichstellen dass die Tabelle kurz gesperrt wird dass zwischendurch keine Transaktionen für den Benutzer ausgeführt werden könne => Kontostand sicherstellen

@Service
public class TransactionService {
    @Autowired
    private TransactionRepository txRepo;

    @Autowired
    private UserService userService;

    @Transactional(readOnly = true)
    public Page<TransactionRow> findLiteItemsForUser(long userId, int page, int size) {
        Pageable pr = PageRequest.of(page, size);
        return txRepo.findRowsForUser(userId, pr);
    }

    @Transactional(readOnly = true)
    public BigDecimal computeBalance(long userId) {
        BigDecimal b = txRepo.computeBalance(userId);
        return b != null ? b : BigDecimal.ZERO;
    }

    public List<TransactionRow> transactionsForUserAsList(long userId) {
        return this.txRepo.findRowsForUserList(userId);
    }

    /* =================== Commands (neu) =================== */

    @Transactional
    public Transaction deposit(User user, BigDecimal amount, String subject) {
        Transaction tx = new Transaction();
        tx.setTransactionType(EINZAHLUNG);
        tx.setRecipient(user);
        tx.setAmount(amount);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);
        return tx;
    }

    @Transactional
    public Transaction withdraw(User user, BigDecimal amount, String subject) {
        Transaction tx = new Transaction();
        tx.setTransactionType(AUSZAHLUNG);
        tx.setSender(user);
        tx.setAmount(amount);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);
        return tx;
    }

    @Transactional
    public Transaction transfer(User sender, String recipientUsername, BigDecimal amount, String subject) {
        if (recipientUsername == null || recipientUsername.isBlank()) {
            throw new TransactionException("Empfänger ist erforderlich.");
        }
        User recipient = userService.findByUsername(recipientUsername);

        if (recipient.getId().equals(sender.getId())) {
            throw new TransactionException("Überweisung an sich selbst ist nicht erlaubt.");
        }

        requirePositive(amount, "Der Betrag muss größer als 0 sein.");

        Transaction tx = new Transaction();
        tx.setTransactionType(UEBERWEISUNG);
        tx.setSender(sender);
        tx.setRecipient(recipient);
        tx.setAmount(amount);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);
        return tx;
    }

    public Page<TransactionRow> searchTransactions(TransactionViewSearchParameter parameter, int page, int size) {
        String senderRecipientPattern = parameter.getSenderRecipientUsername() == null ? null : "%" + parameter.getSenderRecipientUsername().toLowerCase() + "%";
        String descriptionPattern = parameter.getDescription() == null ? null : "%" + parameter.getDescription().toLowerCase() + "%";
        Pageable pr = PageRequest.of(page, size);
        return txRepo.searchTransactions(
                parameter.getCurrentUserID(),
                parameter.getCreatedFrom(),
                parameter.getCreatedTo(),
                senderRecipientPattern,
                parameter.getAmountFrom(),
                parameter.getAmountTo(),
                descriptionPattern,
                pr
        );
    }

    public TransactionLite transactionInfosToTransactionLite(String amountIn, String senderUsername, String recipientUsername, String description, TransactionType type) {
        BigDecimal amount = parseAmountTolerant(amountIn);
        String recipient = safe(recipientUsername);
        if (type == TransactionType.EINZAHLUNG) {

        } else if (type == TransactionType.AUSZAHLUNG) {
        } else if (type == TransactionType.UEBERWEISUNG) {
            if (recipient.equals(senderUsername))
                throw new TransactionException("Der angegebene Empfänger existiert nicht.");
                userService.findByUsername(recipient);
        } else {
            throw new IllegalStateException("Kein Transaktionstyp ausgewählt.");
        }
        return new TransactionLite(amount, type, senderUsername, recipientUsername, description);
    }

    public TransactionResult sendTransfers(TransactionLite transactionLite, User currentUser) {
        BigDecimal currentBalance = this.computeBalance(currentUser.getId());
        if (transactionLite.type() == TransactionType.EINZAHLUNG) {
            this.deposit(currentUser, transactionLite.amount(), transactionLite.description());
            currentBalance = currentBalance.add(transactionLite.amount());
        } else if (transactionLite.type() == TransactionType.AUSZAHLUNG) {
            if (currentBalance.compareTo(transactionLite.amount()) < 0) throw new TransactionException("Nicht genügend Guthaben für die Auszahlung.");
            currentBalance = currentBalance.subtract(transactionLite.amount());
            this.withdraw(currentUser, transactionLite.amount(), transactionLite.description());
            currentBalance = currentBalance.add(transactionLite.amount());
        } else {
            if (currentBalance.compareTo(transactionLite.amount()) < 0) throw new TransactionException("Nicht genügend Guthaben für die Auszahlung.");
            currentBalance = currentBalance.subtract(transactionLite.amount());

            this.transfer(currentUser, transactionLite.recipientUsername(), transactionLite.amount(), transactionLite.description());
        }
        return new TransactionResult(new Transaction(), currentBalance);
    }


    public ArrayList<TransactionResult> sendBulkTransfers(List<TransactionLite> transactionsLite, User currentUser) {
        if(transactionsLite.isEmpty()) throw new IllegalArgumentException("Die Transaktionsliste ist leer, es können keine Transaktionen ausgeführt werden");
        ArrayList<TransactionResult> results =  new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (TransactionLite lite : transactionsLite) {
            if (lite.type() == TransactionType.EINZAHLUNG) {
                sum = sum.subtract(lite.amount());
            } else {
                sum = sum.add(lite.amount());
            }
        }
        BigDecimal balance = this.computeBalance(currentUser.getId());
        if(balance.compareTo(sum) < 0) throw  new TransactionException( "Dein Kontostand ist zu gering um die Transaktionen auszuführen");

        balance = balance.subtract(sum);
        for (int i = 0; i < transactionsLite.size(); i++) {
            TransactionLite lite = transactionsLite.get(i);
            Transaction transaction = sendTransfersWithoutBalanceCheck(lite, currentUser);
            results.add(new TransactionResult(transaction, balance));
        }
        if(results.isEmpty())throw new IllegalArgumentException("Es gab ein Problem beim ausführen deiner Transaktionen");
        return results;
    }

    private Transaction sendTransfersWithoutBalanceCheck(TransactionLite transactionLite,User currentUser) {
        Transaction transaction;
        if (transactionLite.type() == TransactionType.EINZAHLUNG) {
            transaction = this.deposit(currentUser, transactionLite.amount(), transactionLite.description());
        } else if (transactionLite.type()== TransactionType.AUSZAHLUNG) {
            transaction = this.withdraw(currentUser, transactionLite.amount(), transactionLite.description());
        } else {
            transaction = this.transfer(currentUser, transactionLite.recipientUsername(), transactionLite.amount(), transactionLite.description());
        }
        return transaction;
    }

    public BigDecimal findUserBalanceAfterTransaction(long userId, long transactionId){
       return txRepo.findUserBalanceAfterTransaction(userId, transactionId);
    }

    private void requirePositive(BigDecimal amt, String msg) {
        if (amt.signum() <= 0) throw new TransactionException(msg);
    }

}

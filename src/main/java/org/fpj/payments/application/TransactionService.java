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
    public void deposit(User user, BigDecimal amount, String subject) {
        Transaction tx = new Transaction();
        tx.setTransactionType(EINZAHLUNG);
        tx.setRecipient(user);
        tx.setAmount(amount);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);
    }

    @Transactional
    public void withdraw(User user, BigDecimal amount, String subject) {
        Transaction tx = new Transaction();
        tx.setTransactionType(AUSZAHLUNG);
        tx.setSender(user);
        tx.setAmount(amount);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);
    }

    @Transactional
    public void transfer(User sender, String recipientUsername, BigDecimal amount, String subject) {
        if (recipientUsername == null || recipientUsername.isBlank()) {
            throw new TransactionException("Empfänger ist erforderlich.");
        }
       Optional<User> optRecipient = userService.findByUsername(recipientUsername);
        if (!optRecipient.isPresent()) {
            throw new TransactionException("Der angegebene Empfänger existiert nicht.");
        }
        User recipient = optRecipient.get();

        if (recipient.getId().equals(sender.getId())) {
            throw new TransactionException("Überweisung an sich selbst ist nicht erlaubt.");
        }

        requirePositive(amount, "Der Betrag muss größer als 0 sein.");
        ensureSufficientFunds(sender.getId(), amount, "Nicht genügend Guthaben für Überweisung.");

        Transaction tx = new Transaction();
        tx.setTransactionType(UEBERWEISUNG);
        tx.setSender(sender);
        tx.setRecipient(recipient);
        tx.setAmount(amount);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);
    }

    public  Page<TransactionRow>  searchTransactions( TransactionViewSearchParameter parameter,  int page, int size) {
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

    public TransactionLite transactionInfosToTransactionLite(String amountIn, String senderUsername, String recipientUsername, String description , TransactionType type) {
            BigDecimal amount = parseAmountTolerant(amountIn);
            String recipient = safe(recipientUsername);
            if (type == TransactionType.EINZAHLUNG) {

            } else if (type == TransactionType.AUSZAHLUNG) {
            } else if (type == TransactionType.UEBERWEISUNG) {
                if(recipient.equals(senderUsername))throw  new TransactionException("Der angegebene Empfänger existiert nicht.");
                if(userService.findByUsername(recipient).isEmpty()) throw  new TransactionException(String.format( "Der Empfänger mit dem Benutzernamen %s  ist kein Benutzer unserer Plattform", recipient));
            } else {
                throw new IllegalStateException("Kein Transaktionstyp ausgewählt.");
            }
            return new  TransactionLite(amount, type, senderUsername, recipientUsername, description);
    }

    public TransactionResult sendTransfers(TransactionLite transactionLite,User currentUser) {
        TransactionResult result;
            if (transactionLite.type() == TransactionType.EINZAHLUNG) {
                this.deposit(currentUser, transactionLite.amount(), transactionLite.description());
            } else if (transactionLite.type()== TransactionType.AUSZAHLUNG) {
                ensureSufficientFunds(currentUser.getId(), transactionLite.amount(), "Nicht genügend Guthaben für die Auszahlung.");
                this.withdraw(currentUser, transactionLite.amount(), transactionLite.description());
            } else if (transactionLite.type()== TransactionType.UEBERWEISUNG) {
                ensureSufficientFunds(currentUser.getId(), transactionLite.amount(), "Nicht genügend Guthaben für die Überweisung.");
                this.transfer(currentUser, transactionLite.recipientUsername(), transactionLite.amount(), transactionLite.description());
            } else {
                throw new IllegalStateException("Kein Transaktionstyp ausgewählt.");
            }
            return result;
    }

    public List<TransactionResult> sendBulkTransfers(List<TransactionLite> transactionsLite, User currentUser) {
        List<TransactionResult> results = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (TransactionLite lite : transactionsLite) {
            if (lite.type() == TransactionType.EINZAHLUNG) {
                sum = sum.subtract(lite.amount());
            } else {
                sum = sum.add(lite.amount());
            }
        }
        ensureSufficientFunds(currentUser.getId(), sum, "Dein Kontostand ist zu gering um die Transaktionen auszuführen");
        for (TransactionLite lite : transactionsLite) {
           results.add(sendTransfersWithoutBalanceCheck(lite, currentUser));
        }

        return results;
    }

    private TransactionResult sendTransfersWithoutBalanceCheck(TransactionLite transactionLite,User currentUser) {
        requirePositive(transactionLite.amount(), "Der Betrag muss größer als 0 sein.");
        TransactionResult result;
        if (transactionLite.type() == TransactionType.EINZAHLUNG) {
            result = this.deposit(currentUser, transactionLite.amount(), transactionLite.description());
        } else if (transactionLite.type()== TransactionType.AUSZAHLUNG) {
            result = this.withdraw(currentUser, transactionLite.amount(), transactionLite.description());
        } else {
            result = this.transfer(currentUser, transactionLite.recipientUsername(), transactionLite.amount(), transactionLite.description());
        }
        return result;
    }

    public BigDecimal findUserBalanceAfterTransaction(long userId, long transactionId){
       return txRepo.findUserBalanceAfterTransaction(userId, transactionId);
    }

    private void requirePositive(BigDecimal amt, String msg) {
        if (amt.signum() <= 0) throw new TransactionException(msg);
    }

    private void ensureSufficientFunds(long userId, BigDecimal amt, String msg) {
        if (computeBalance(userId).compareTo(amt) < 0) {
            throw new TransactionException(msg);
        }
    }
}

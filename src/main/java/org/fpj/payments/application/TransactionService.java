package org.fpj.payments.application;

import org.fpj.Exceptions.DataNotPresentException;
import org.fpj.Exceptions.TransactionException;
import org.fpj.payments.domain.Transaction;
import org.fpj.payments.domain.TransactionRepository;
import org.fpj.payments.domain.TransactionResult;
import org.fpj.payments.domain.TransactionRow;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import static org.fpj.payments.domain.TransactionType.*;

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

    /* =================== Commands (neu) =================== */

    @Transactional
    public TransactionResult deposit(User user, BigDecimal amount, String subject) {
        BigDecimal a = normalizeAmount(amount);
        requirePositive(a, "Der Betrag muss größer als 0 sein.");

        Transaction tx = new Transaction();
        tx.setTransactionType(EINZAHLUNG);
        tx.setRecipient(user);
        tx.setAmount(a);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);

        BigDecimal newBalance = computeBalance(user.getId());
        return new TransactionResult(tx, newBalance);
    }

    @Transactional
    public TransactionResult withdraw(User user, BigDecimal amount, String subject) {
        BigDecimal a = normalizeAmount(amount);
        requirePositive(a, "Der Betrag muss größer als 0 sein.");
        ensureSufficientFunds(user.getId(), a, "Nicht genügend Guthaben für Auszahlung.");

        Transaction tx = new Transaction();
        tx.setTransactionType(AUSZAHLUNG);
        tx.setSender(user);
        tx.setAmount(a);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);

        BigDecimal newBalance = computeBalance(user.getId());
        return new TransactionResult(tx, newBalance);
    }

    @Transactional
    public TransactionResult transfer(User sender, String recipientUsername, BigDecimal amount, String subject) {
        if (recipientUsername == null || recipientUsername.isBlank()) {
            throw new TransactionException("Empfänger ist erforderlich.");
        }

        final User recipient;
        try {
            recipient = userService.findByUsername(recipientUsername);
        }
        catch (DataNotPresentException e){
            throw new TransactionException("Der angegebene Empfänger existiert nicht.");
        }

        if (Objects.equals(recipient.getId(), sender.getId())) {
            throw new TransactionException("Überweisung an sich selbst ist nicht erlaubt.");
        }

        BigDecimal a = normalizeAmount(amount);
        requirePositive(a, "Der Betrag muss größer als 0 sein.");
        ensureSufficientFunds(sender.getId(), a, "Nicht genügend Guthaben für Überweisung.");

        Transaction tx = new Transaction();
        tx.setTransactionType(UEBERWEISUNG);
        tx.setSender(sender);
        tx.setRecipient(recipient);
        tx.setAmount(a);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);

        BigDecimal newBalance = computeBalance(sender.getId());
        return new TransactionResult(tx, newBalance);
    }

    /* =================== Helpers =================== */

    private static BigDecimal normalizeAmount(BigDecimal amt) {
        if (amt == null) return BigDecimal.ZERO;
        return amt.setScale(2, RoundingMode.HALF_UP);
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

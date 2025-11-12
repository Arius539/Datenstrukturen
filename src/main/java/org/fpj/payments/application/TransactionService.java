package org.fpj.payments.application;

import org.fpj.Exceptions.TransactionException;
import org.fpj.payments.domain.Transaction;
import org.fpj.payments.domain.TransactionRepository;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import static org.fpj.Data.TransactionType.*;

@Service
public class TransactionService {
    @Autowired
    private TransactionRepository txRepo;

    @Autowired
    private UserService userService;

    /* =================== Queries (unverändert) =================== */

    @Transactional(readOnly = true)
    public Page<TransactionItemLite> findLiteItemsForUser(long userId, int page, int size) {
        Pageable pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return txRepo.findRowsForUser(userId, pr).map(r -> {
            boolean outgoing = r.senderId() != null && r.senderId().longValue() == userId;

            BigDecimal signed = r.amount() == null ? BigDecimal.ZERO
                    : (outgoing ? r.amount().negate() : r.amount());

            String name = outgoing
                    ? (r.recipientUsername() != null ? r.recipientUsername() : "Empfänger unbekannt")
                    : (r.senderUsername() != null ? r.senderUsername() : "Sender unbekannt");
            String counterparty = switch (r.type()) {
                case EINZAHLUNG   -> "Einzahlung";
                case AUSZAHLUNG   -> "Auszahlung";
                case UEBERWEISUNG -> (outgoing ? "Überweisung an " : "Überweisung von ") + name;
            };
            LocalDateTime ts = LocalDateTime.ofInstant(r.createdAt(), ZoneId.systemDefault());
            return new TransactionItemLite(counterparty, signed, ts, r.description());
        });
    }

    public Page<TransactionItem> findItemsForUser(long userId, int page, int size) {
        Pageable pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return txRepo.findRowsForUser(userId, pr).map(r -> {
            BigDecimal signed = r.amount() == null ? BigDecimal.ZERO: r.amount();

            String sender= r.senderUsername() == null ? "Unbekannt" : r.senderUsername();
            String recipient= r.recipientUsername() == null ? "Unbekannt" : r.senderUsername();

            String type = switch (r.type()) {
                case EINZAHLUNG   -> "Einzahlung";
                case AUSZAHLUNG   -> "Auszahlung";
                case UEBERWEISUNG -> "Überweisung";
            };
            LocalDateTime ts = LocalDateTime.ofInstant(r.createdAt(), ZoneId.systemDefault());
            return new TransactionItem(sender, recipient, signed, ts, r.description(),type);
        });
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
        requirePositive(a, "Betrag muss > 0 sein.");

        Transaction tx = new Transaction();
        tx.setTransactionType(EINZAHLUNG);
        tx.setRecipient(user);
        tx.setAmount(a);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);

        BigDecimal newBalance = computeBalance(user.getId());
        return new TransactionResult(tx, newBalance,
                new TransactionItemLite("Einzahlung", a,
                        LocalDateTime.ofInstant(tx.getCreatedAt(), ZoneId.systemDefault()), subject));
    }

    @Transactional
    public TransactionResult withdraw(User user, BigDecimal amount, String subject) {
        BigDecimal a = normalizeAmount(amount);
        requirePositive(a, "Betrag muss > 0 sein.");
        ensureSufficientFunds(user.getId(), a, "Nicht genügend Guthaben für Auszahlung.");

        Transaction tx = new Transaction();
        tx.setTransactionType(AUSZAHLUNG);
        tx.setSender(user);
        tx.setAmount(a);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);

        BigDecimal newBalance = computeBalance(user.getId());
        return new TransactionResult(tx, newBalance,
                new TransactionItemLite("Auszahlung", a.negate(),
                        LocalDateTime.ofInstant(tx.getCreatedAt(), ZoneId.systemDefault()), subject));
    }

    @Transactional
    public TransactionResult transfer(User sender, String recipientUsername, BigDecimal amount, String subject) {
        if (recipientUsername == null || recipientUsername.isBlank()) {
            throw new TransactionException("Empfänger ist erforderlich.");
        }
        User recipient = userService.findByUsername(recipientUsername);

        if (recipient.getId() == sender.getId()) {
            throw new TransactionException("Überweisung an sich selbst ist nicht erlaubt.");
        }

        BigDecimal a = normalizeAmount(amount);
        requirePositive(a, "Betrag muss > 0 sein.");
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
        String cp = "Überweisung an " + recipient.getUsername();
        return new TransactionResult(tx, newBalance,
                new TransactionItemLite(cp, a.negate(),
                        LocalDateTime.ofInstant(tx.getCreatedAt(), ZoneId.systemDefault()), subject));
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

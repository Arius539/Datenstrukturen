package org.fpj.payments.application;

import org.fpj.exceptions.DataNotPresentException;
import org.fpj.exceptions.TransactionException;
import org.fpj.payments.domain.*;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository txRepo;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionService underTest;

    @Test
    public void testFindLiteItemsForUser() {
        long userId = 1L;
        int page = 0;
        int size = 10;
        PageRequest pageRequest = PageRequest.of(page, size);

        TransactionRow row = new TransactionRow(1L, new BigDecimal("100"), Instant.now(),
                TransactionType.EINZAHLUNG, null, null, userId, "user@test.de", "Test");
        Page<TransactionRow> expectedPage = new PageImpl<>(Arrays.asList(row));

        when(txRepo.findRowsForUser(userId, pageRequest)).thenReturn(expectedPage);

        Page<TransactionRow> result = underTest.findLiteItemsForUser(userId, page, size);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(txRepo, times(1)).findRowsForUser(userId, pageRequest);
    }

    @Test
    public void testComputeBalance() {
        long userId = 1L;
        BigDecimal expectedBalance = new BigDecimal("500.00");

        when(txRepo.computeBalance(userId)).thenReturn(expectedBalance);

        BigDecimal result = underTest.computeBalance(userId);

        assertEquals(expectedBalance, result);
        verify(txRepo, times(1)).computeBalance(userId);
    }

    @Test
    public void testComputeBalanceReturnsZeroWhenNull() {
        long userId = 2L;

        when(txRepo.computeBalance(userId)).thenReturn(null);

        BigDecimal result = underTest.computeBalance(userId);

        assertEquals(BigDecimal.ZERO, result);
        verify(txRepo, times(1)).computeBalance(userId);
    }

    @Test
    public void testTransactionsForUserAsList() {
        long userId = 1L;
        TransactionRow row = new TransactionRow(1L, new BigDecimal("100"), Instant.now(),
                TransactionType.EINZAHLUNG, null, null, userId, "user@test.de", "Test");
        List<TransactionRow> expectedList = Arrays.asList(row);

        when(txRepo.findRowsForUserList(userId)).thenReturn(expectedList);

        List<TransactionRow> result = underTest.transactionsForUserAsList(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(txRepo, times(1)).findRowsForUserList(userId);
    }

    @Test
    public void testDeposit() {
        User user = new User("user@test.de", "hash");
        user.setId(1L);
        BigDecimal amount = new BigDecimal("100.00");
        String subject = "Test deposit";

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(1L);

        when(txRepo.save(any(Transaction.class))).thenReturn(savedTransaction);

        Transaction result = underTest.deposit(user, amount, subject);

        assertNotNull(result);
        verify(txRepo, times(1)).save(any(Transaction.class));
    }

    @Test
    public void testWithdraw() {
        User user = new User("user@test.de", "hash");
        user.setId(1L);
        BigDecimal amount = new BigDecimal("50.00");
        String subject = "Test withdrawal";

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(2L);

        when(txRepo.save(any(Transaction.class))).thenReturn(savedTransaction);

        Transaction result = underTest.withdraw(user, amount, subject);

        assertNotNull(result);
        verify(txRepo, times(1)).save(any(Transaction.class));
    }

    @Test
    public void testTransferSuccess() {
        User sender = new User("sender@test.de", "hash");
        sender.setId(1L);
        User recipient = new User("recipient@test.de", "hash");
        recipient.setId(2L);

        BigDecimal amount = new BigDecimal("75.00");
        String subject = "Test transfer";

        when(userService.findByUsername("recipient@test.de")).thenReturn(recipient);
        when(txRepo.save(any(Transaction.class))).thenReturn(new Transaction());

        Transaction result = underTest.transfer(sender, "recipient@test.de", amount, subject);

        assertNotNull(result);
        verify(userService, times(1)).findByUsername("recipient@test.de");
        verify(txRepo, times(1)).save(any(Transaction.class));
    }

    @Test
    public void testTransferThrowsExceptionWhenRecipientIsNull() {
        User sender = new User("sender@test.de", "hash");
        sender.setId(1L);

        TransactionException exception = assertThrows(TransactionException.class, () -> {
            underTest.transfer(sender, null, new BigDecimal("50"), "Test");
        });

        assertEquals("Empfänger ist erforderlich.", exception.getMessage());
        verify(txRepo, never()).save(any());
    }

    @Test
    public void testTransferThrowsExceptionWhenRecipientIsBlank() {
        User sender = new User("sender@test.de", "hash");
        sender.setId(1L);

        TransactionException exception = assertThrows(TransactionException.class, () -> {
            underTest.transfer(sender, "", new BigDecimal("50"), "Test");
        });

        assertEquals("Empfänger ist erforderlich.", exception.getMessage());
        verify(txRepo, never()).save(any());
    }

    @Test
    public void testTransferThrowsExceptionWhenTransferringToSelf() {
        User user = new User("user@test.de", "hash");
        user.setId(1L);

        when(userService.findByUsername("user@test.de")).thenReturn(user);

        TransactionException exception = assertThrows(TransactionException.class, () -> {
            underTest.transfer(user, "user@test.de", new BigDecimal("50"), "Test");
        });

        assertEquals("Überweisung an sich selbst ist nicht erlaubt.", exception.getMessage());
        verify(txRepo, never()).save(any());
    }

    @Test
    public void testTransferThrowsExceptionWhenAmountIsZero() {
        User sender = new User("sender@test.de", "hash");
        sender.setId(1L);
        User recipient = new User("recipient@test.de", "hash");
        recipient.setId(2L);

        when(userService.findByUsername("recipient@test.de")).thenReturn(recipient);

        TransactionException exception = assertThrows(TransactionException.class, () -> {
            underTest.transfer(sender, "recipient@test.de", BigDecimal.ZERO, "Test");
        });

        assertEquals("Der Betrag muss größer als 0 sein.", exception.getMessage());
        verify(txRepo, never()).save(any());
    }

    @Test
    public void testTransferThrowsExceptionWhenAmountIsNegative() {
        User sender = new User("sender@test.de", "hash");
        sender.setId(1L);
        User recipient = new User("recipient@test.de", "hash");
        recipient.setId(2L);

        when(userService.findByUsername("recipient@test.de")).thenReturn(recipient);

        TransactionException exception = assertThrows(TransactionException.class, () -> {
            underTest.transfer(sender, "recipient@test.de", new BigDecimal("-50"), "Test");
        });

        assertEquals("Der Betrag muss größer als 0 sein.", exception.getMessage());
        verify(txRepo, never()).save(any());
    }

    @Test
    public void testSearchTransactions() {
        TransactionViewSearchParameter parameter = new TransactionViewSearchParameter();
        parameter.setCurrentUserID(1L);
        parameter.setSenderRecipientUsername("test");
        parameter.setDescription("payment");

        TransactionRow row = new TransactionRow(1L, new BigDecimal("100"), Instant.now(),
                TransactionType.UEBERWEISUNG, 1L, "sender@test.de", 2L, "test@test.de", "payment");
        Page<TransactionRow> expectedPage = new PageImpl<>(Arrays.asList(row));

        when(txRepo.searchTransactions(
                eq(1L),
                any(),
                any(),
                eq("%test%"),
                any(),
                any(),
                eq("%payment%"),
                any()
        )).thenReturn(expectedPage);

        Page<TransactionRow> result = underTest.searchTransactions(parameter, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(txRepo, times(1)).searchTransactions(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testSendTransfersEinzahlung() {
        User user = new User("user@test.de", "hash");
        user.setId(1L);

        TransactionLite lite = new TransactionLite(
                new BigDecimal("100"),
                TransactionType.EINZAHLUNG,
                "user@test.de",
                null,
                "Deposit"
        );

        when(txRepo.computeBalance(1L)).thenReturn(new BigDecimal("200"));
        when(txRepo.save(any(Transaction.class))).thenReturn(new Transaction());

        TransactionResult result = underTest.sendTransfers(lite, user);

        assertNotNull(result);
        assertEquals(new BigDecimal("300"), result.newBalance());
        verify(txRepo, times(1)).save(any(Transaction.class));
    }

    @Test
    public void testSendTransfersAuszahlung() {
        User user = new User("user@test.de", "hash");
        user.setId(1L);

        TransactionLite lite = new TransactionLite(
                new BigDecimal("50"),
                TransactionType.AUSZAHLUNG,
                "user@test.de",
                null,
                "Withdrawal"
        );

        when(txRepo.computeBalance(1L)).thenReturn(new BigDecimal("200"));
        when(txRepo.save(any(Transaction.class))).thenReturn(new Transaction());

        TransactionResult result = underTest.sendTransfers(lite, user);

        assertNotNull(result);
        assertEquals(new BigDecimal("150"), result.newBalance());
        verify(txRepo, times(1)).save(any(Transaction.class));
    }

    @Test
    public void testSendTransfersAuszahlungInsufficientBalance() {
        User user = new User("user@test.de", "hash");
        user.setId(1L);

        TransactionLite lite = new TransactionLite(
                new BigDecimal("300"),
                TransactionType.AUSZAHLUNG,
                "user@test.de",
                null,
                "Withdrawal"
        );

        when(txRepo.computeBalance(1L)).thenReturn(new BigDecimal("200"));

        TransactionException exception = assertThrows(TransactionException.class, () -> {
            underTest.sendTransfers(lite, user);
        });

        assertEquals("Nicht genügend Guthaben für die Auszahlung.", exception.getMessage());
        verify(txRepo, never()).save(any());
    }

    @Test
    public void testSendTransfersUeberweisung() {
        User sender = new User("sender@test.de", "hash");
        sender.setId(1L);
        User recipient = new User("recipient@test.de", "hash");
        recipient.setId(2L);

        TransactionLite lite = new TransactionLite(
                new BigDecimal("100"),
                TransactionType.UEBERWEISUNG,
                "sender@test.de",
                "recipient@test.de",
                "Transfer"
        );

        when(txRepo.computeBalance(1L)).thenReturn(new BigDecimal("200"));
        when(userService.findByUsername("recipient@test.de")).thenReturn(recipient);
        when(txRepo.save(any(Transaction.class))).thenReturn(new Transaction());

        TransactionResult result = underTest.sendTransfers(lite, sender);

        assertNotNull(result);
        assertEquals(new BigDecimal("100"), result.newBalance());
        verify(txRepo, times(1)).save(any(Transaction.class));
    }

    @Test
    public void testSendBulkTransfersSuccess() {
        User user = new User("user@test.de", "hash");
        user.setId(1L);
        User recipient = new User("recipient@test.de", "hash");
        recipient.setId(2L);

        TransactionLite lite1 = new TransactionLite(
                new BigDecimal("50"),
                TransactionType.UEBERWEISUNG,
                "user@test.de",
                "recipient@test.de",
                "Transfer 1"
        );

        TransactionLite lite2 = new TransactionLite(
                new BigDecimal("30"),
                TransactionType.AUSZAHLUNG,
                "user@test.de",
                null,
                "Withdrawal"
        );

        List<TransactionLite> transactionsLite = Arrays.asList(lite1, lite2);

        when(txRepo.computeBalance(1L)).thenReturn(new BigDecimal("200"));
        when(userService.findByUsername("recipient@test.de")).thenReturn(recipient);
        when(txRepo.save(any(Transaction.class))).thenReturn(new Transaction());

        ArrayList<TransactionResult> results = underTest.sendBulkTransfers(transactionsLite, user);

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(txRepo, times(2)).save(any(Transaction.class));
    }

    @Test
    public void testSendBulkTransfersEmptyList() {
        User user = new User("user@test.de", "hash");
        user.setId(1L);

        List<TransactionLite> emptyList = new ArrayList<>();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            underTest.sendBulkTransfers(emptyList, user);
        });

        assertEquals("Die Transaktionsliste ist leer, es können keine Transaktionen ausgeführt werden", exception.getMessage());
        verify(txRepo, never()).save(any());
    }

    @Test
    public void testSendBulkTransfersInsufficientBalance() {
        User user = new User("user@test.de", "hash");
        user.setId(1L);

        TransactionLite lite1 = new TransactionLite(
                new BigDecimal("100"),
                TransactionType.AUSZAHLUNG,
                "user@test.de",
                null,
                "Withdrawal 1"
        );

        TransactionLite lite2 = new TransactionLite(
                new BigDecimal("150"),
                TransactionType.AUSZAHLUNG,
                "user@test.de",
                null,
                "Withdrawal 2"
        );

        List<TransactionLite> transactionsLite = Arrays.asList(lite1, lite2);

        when(txRepo.computeBalance(1L)).thenReturn(new BigDecimal("200"));

        TransactionException exception = assertThrows(TransactionException.class, () -> {
            underTest.sendBulkTransfers(transactionsLite, user);
        });

        assertEquals("Dein Kontostand ist zu gering um die Transaktionen auszuführen", exception.getMessage());
        verify(txRepo, never()).save(any());
    }

    @Test
    public void testFindUserBalanceAfterTransaction() {
        long userId = 1L;
        long transactionId = 10L;
        BigDecimal expectedBalance = new BigDecimal("350.00");

        when(txRepo.findUserBalanceAfterTransaction(userId, transactionId)).thenReturn(expectedBalance);

        BigDecimal result = underTest.findUserBalanceAfterTransaction(userId, transactionId);

        assertEquals(expectedBalance, result);
        verify(txRepo, times(1)).findUserBalanceAfterTransaction(userId, transactionId);
    }

    @Test
    public void testTransactionInfosToTransactionLiteEinzahlung() {
        TransactionLite result = underTest.transactionInfosToTransactionLite(
                "100.50",
                "sender@test.de",
                "recipient@test.de",
                "Test description",
                TransactionType.EINZAHLUNG
        );

        assertNotNull(result);
        assertEquals(new BigDecimal("100.50"), result.amount());
        assertEquals(TransactionType.EINZAHLUNG, result.type());
    }

    @Test
    public void testTransactionInfosToTransactionLiteAuszahlung() {
        TransactionLite result = underTest.transactionInfosToTransactionLite(
                "50.75",
                "sender@test.de",
                "recipient@test.de",
                "Test withdrawal",
                TransactionType.AUSZAHLUNG
        );

        assertNotNull(result);
        assertEquals(new BigDecimal("50.75"), result.amount());
        assertEquals(TransactionType.AUSZAHLUNG, result.type());
    }

    @Test
    public void testTransactionInfosToTransactionLiteUeberweisungSuccess() {
        User recipient = new User("recipient@test.de", "hash");
        recipient.setId(2L);

        when(userService.findByUsername("recipient@test.de")).thenReturn(recipient);

        TransactionLite result = underTest.transactionInfosToTransactionLite(
                "75.00",
                "sender@test.de",
                "recipient@test.de",
                "Test transfer",
                TransactionType.UEBERWEISUNG
        );

        assertNotNull(result);
        assertEquals(new BigDecimal("75.00"), result.amount());
        assertEquals(TransactionType.UEBERWEISUNG, result.type());
        verify(userService, times(1)).findByUsername("recipient@test.de");
    }

    @Test
    public void testTransactionInfosToTransactionLiteUeberweisungSameUser() {
        TransactionException exception = assertThrows(TransactionException.class, () -> {
            underTest.transactionInfosToTransactionLite(
                    "75.00",
                    "user@test.de",
                    "user@test.de",
                    "Test transfer",
                    TransactionType.UEBERWEISUNG
            );
        });

        assertEquals("Der angegebene Empfänger existiert nicht.", exception.getMessage());
    }

    @Test
    public void testTransactionInfosToTransactionLiteNoTypeSelected() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            underTest.transactionInfosToTransactionLite(
                    "100.00",
                    "sender@test.de",
                    "recipient@test.de",
                    "Test",
                    null
            );
        });

        assertEquals("Kein Transaktionstyp ausgewählt.", exception.getMessage());
    }
}

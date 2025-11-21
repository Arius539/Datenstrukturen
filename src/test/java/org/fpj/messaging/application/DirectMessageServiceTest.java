package org.fpj.messaging.application;

import org.fpj.exceptions.DataNotPresentException;
import org.fpj.messaging.domain.ChatPreview;
import org.fpj.messaging.domain.DirectMessage;
import org.fpj.messaging.domain.DirectMessageRepository;
import org.fpj.messaging.domain.DirectMessageRow;
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
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DirectMessageServiceTest {

    @Mock
    private DirectMessageRepository dmRepo;

    @Mock
    private UserService userService;

    @InjectMocks
    private DirectMessageService underTest;

    @Test
    public void testGetChatPreviewsWithMessages() {
        User currentUser = new User("current@test.de", "hash");
        currentUser.setId(1L);

        User contact1 = new User("contact1@test.de", "hash");
        contact1.setId(2L);

        User contact2 = new User("contact2@test.de", "hash");
        contact2.setId(3L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> contactsPage = new PageImpl<>(Arrays.asList(contact1, contact2));

        DirectMessage lastMessage1 = new DirectMessage();
        lastMessage1.setId(1L);
        lastMessage1.setSender(contact1);
        lastMessage1.setRecipient(currentUser);
        lastMessage1.setContent("Hello from contact1");
        lastMessage1.setCreatedAt(Instant.now());

        DirectMessage lastMessage2 = new DirectMessage();
        lastMessage2.setId(2L);
        lastMessage2.setSender(currentUser);
        lastMessage2.setRecipient(contact2);
        lastMessage2.setContent("Hello to contact2");
        lastMessage2.setCreatedAt(Instant.now());

        when(userService.findContacts(currentUser, pageable)).thenReturn(contactsPage);
        when(dmRepo.lastMessageNative(1L, 2L)).thenReturn(Optional.of(lastMessage1));
        when(dmRepo.lastMessageNative(1L, 3L)).thenReturn(Optional.of(lastMessage2));

        Page<ChatPreview> result = underTest.getChatPreviews(currentUser, pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());

        ChatPreview preview1 = result.getContent().get(0);
        assertEquals("contact1@test.de", preview1.name());
        assertEquals("Hello from contact1", preview1.lastMessage());
        assertNotNull(preview1.timestamp());

        ChatPreview preview2 = result.getContent().get(1);
        assertEquals("contact2@test.de", preview2.name());
        assertEquals("Hello to contact2", preview2.lastMessage());

        verify(userService, times(1)).findContacts(currentUser, pageable);
        verify(dmRepo, times(1)).lastMessageNative(1L, 2L);
        verify(dmRepo, times(1)).lastMessageNative(1L, 3L);
    }

    @Test
    public void testGetChatPreviewsWithoutMessages() {
        User currentUser = new User("current@test.de", "hash");
        currentUser.setId(1L);

        User contact = new User("contact@test.de", "hash");
        contact.setId(2L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> contactsPage = new PageImpl<>(Arrays.asList(contact));

        when(userService.findContacts(currentUser, pageable)).thenReturn(contactsPage);
        when(dmRepo.lastMessageNative(1L, 2L)).thenReturn(Optional.empty());

        Page<ChatPreview> result = underTest.getChatPreviews(currentUser, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        ChatPreview preview = result.getContent().get(0);
        assertEquals("contact@test.de", preview.name());
        assertNull(preview.lastMessage());
        assertNull(preview.timestamp());

        verify(userService, times(1)).findContacts(currentUser, pageable);
        verify(dmRepo, times(1)).lastMessageNative(1L, 2L);
    }

    @Test
    public void testGetConversation() {
        User userA = new User("userA@test.de", "hash");
        userA.setId(1L);

        User userB = new User("userB@test.de", "hash");
        userB.setId(2L);

        DirectMessage message1 = new DirectMessage();
        message1.setId(1L);
        message1.setSender(userA);
        message1.setRecipient(userB);
        message1.setContent("Hello");
        message1.setCreatedAt(Instant.now());

        DirectMessage message2 = new DirectMessage();
        message2.setId(2L);
        message2.setSender(userB);
        message2.setRecipient(userA);
        message2.setContent("Hi there");
        message2.setCreatedAt(Instant.now());

        Pageable pageable = PageRequest.of(0, 20);
        Page<DirectMessage> expectedPage = new PageImpl<>(Arrays.asList(message2, message1));

        when(dmRepo.findConversation(1L, 2L, pageable)).thenReturn(expectedPage);

        Page<DirectMessage> result = underTest.getConversation(userA, userB, pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(dmRepo, times(1)).findConversation(1L, 2L, pageable);
    }

    @Test
    public void testAddDirectMessage() {
        User sender = new User("sender@test.de", "hash");
        sender.setId(1L);

        User recipient = new User("recipient@test.de", "hash");
        recipient.setId(2L);

        DirectMessageRow row = new DirectMessageRow(sender, recipient, "Test message");

        DirectMessage savedMessage = new DirectMessage();
        savedMessage.setId(10L);
        savedMessage.setSender(sender);
        savedMessage.setRecipient(recipient);
        savedMessage.setContent("Test message");
        savedMessage.setCreatedAt(Instant.now());

        when(dmRepo.add(1L, 2L, "Test message")).thenReturn(10L);
        when(dmRepo.getDirectMessageById(10L)).thenReturn(Optional.of(savedMessage));

        DirectMessage result = underTest.addDirectMessage(row);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Test message", result.getContent());
        verify(dmRepo, times(1)).add(1L, 2L, "Test message");
        verify(dmRepo, times(1)).getDirectMessageById(10L);
    }

    @Test
    public void testAddDirectMessageThrowsExceptionWhenNotFound() {
        User sender = new User("sender@test.de", "hash");
        sender.setId(1L);

        User recipient = new User("recipient@test.de", "hash");
        recipient.setId(2L);

        DirectMessageRow row = new DirectMessageRow(sender, recipient, "Test message");

        when(dmRepo.add(1L, 2L, "Test message")).thenReturn(10L);
        when(dmRepo.getDirectMessageById(10L)).thenReturn(Optional.empty());

        DataNotPresentException exception = assertThrows(DataNotPresentException.class, () -> {
            underTest.addDirectMessage(row);
        });

        assertEquals("DirectMessage not found for id 10", exception.getMessage());
        verify(dmRepo, times(1)).add(1L, 2L, "Test message");
        verify(dmRepo, times(1)).getDirectMessageById(10L);
    }

    @Test
    public void testGetConversationMessageList() {
        Long userId1 = 1L;
        Long userId2 = 2L;

        DirectMessage message1 = new DirectMessage();
        message1.setId(1L);
        message1.setContent("Message 1");

        DirectMessage message2 = new DirectMessage();
        message2.setId(2L);
        message2.setContent("Message 2");

        List<DirectMessage> expectedList = Arrays.asList(message1, message2);

        when(dmRepo.findConversationAsList(userId1, userId2)).thenReturn(expectedList);

        List<DirectMessage> result = underTest.getConversationMessageList(userId1, userId2);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Message 1", result.get(0).getContent());
        assertEquals("Message 2", result.get(1).getContent());
        verify(dmRepo, times(1)).findConversationAsList(userId1, userId2);
    }

    @Test
    public void testGetConversationMessageListEmpty() {
        Long userId1 = 1L;
        Long userId2 = 2L;

        when(dmRepo.findConversationAsList(userId1, userId2)).thenReturn(Arrays.asList());

        List<DirectMessage> result = underTest.getConversationMessageList(userId1, userId2);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(dmRepo, times(1)).findConversationAsList(userId1, userId2);
    }
}

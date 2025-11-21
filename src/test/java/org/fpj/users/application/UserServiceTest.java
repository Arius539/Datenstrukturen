package org.fpj.users.application;

import org.fpj.exceptions.DataNotPresentException;
import org.fpj.users.domain.User;
import org.fpj.users.domain.UserRepository;
import org.fpj.users.domain.UsernameOnly;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService underTest;

    @Test
    public void testFindByUsernameSuccess() {
        String username = "testuser@test.de";
        User expectedUser = new User(username, "hashedPassword");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(expectedUser));

        User result = underTest.findByUsername(username);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    public void testFindByUsernameThrowsExceptionWhenNotFound() {
        String username = "nonexistent@test.de";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        DataNotPresentException exception = assertThrows(DataNotPresentException.class, () -> {
            underTest.findByUsername(username);
        });

        assertEquals("Kein User mit Username " + username + " gefunden.", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    public void testSave() {
        User user = new User("newuser@test.de", "hashedPassword");

        when(userRepository.save(user)).thenReturn(user);

        User result = underTest.save(user);

        assertNotNull(result);
        assertEquals(user.getUsername(), result.getUsername());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void testFindContacts() {
        User user = new User("user@test.de", "hashedPassword");
        user.setId(1L);

        User contact1 = new User("contact1@test.de", "hash1");
        User contact2 = new User("contact2@test.de", "hash2");

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> expectedPage = new PageImpl<>(Arrays.asList(contact1, contact2));

        when(userRepository.findContactsOrderByLastMessageDesc(user.getId(), pageable))
                .thenReturn(expectedPage);

        Page<User> result = underTest.findContacts(user, pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(userRepository, times(1)).findContactsOrderByLastMessageDesc(user.getId(), pageable);
    }

    @Test
    public void testUsernameContaining() {
        String searchTerm = "test";

        UsernameOnly user1 = () -> "testuser1@test.de";
        UsernameOnly user2 = () -> "testuser2@test.de";

        List<UsernameOnly> usernames = Arrays.asList(user1, user2);

        when(userRepository.findTop10ByUsernameContainingIgnoreCaseOrderByUsernameAsc(searchTerm))
                .thenReturn(usernames);

        List<String> result = underTest.usernameContaining(searchTerm);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("testuser1@test.de", result.get(0));
        assertEquals("testuser2@test.de", result.get(1));
        verify(userRepository, times(1)).findTop10ByUsernameContainingIgnoreCaseOrderByUsernameAsc(searchTerm);
    }

    @Test
    public void testUsernameExists() {
        String username = "existing@test.de";

        when(userRepository.existsByUsername(username)).thenReturn(true);

        boolean result = underTest.usernameExists(username);

        assertTrue(result);
        verify(userRepository, times(1)).existsByUsername(username);
    }

    @Test
    public void testUsernameDoesNotExist() {
        String username = "nonexisting@test.de";

        when(userRepository.existsByUsername(username)).thenReturn(false);

        boolean result = underTest.usernameExists(username);

        assertFalse(result);
        verify(userRepository, times(1)).existsByUsername(username);
    }
}

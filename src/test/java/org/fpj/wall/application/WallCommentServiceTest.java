package org.fpj.wall.application;

import org.fpj.users.domain.User;
import org.fpj.wall.domain.WallComment;
import org.fpj.wall.domain.WallCommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WallCommentServiceTest {

    @Mock
    private WallCommentRepository wallCommentRepository;

    @Mock
    private WallCommentManagingService wallcommentManagingService;

    @Mock
    private ApplicationContext context;

    @InjectMocks
    private WallCommentService underTest;

    @Test
    public void testGetWallCommentsByAuthor() {
        long userId = 1L;
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<WallComment> expectedPage = new PageImpl<>(Arrays.asList(new WallComment()));

        when(wallCommentRepository.findByAuthor_IdOrderByCreatedAtDesc(userId, pageRequest))
                .thenReturn(expectedPage);

        Page<WallComment> result = underTest.getWallCommentsByAuthor(userId, pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(wallCommentRepository, times(1)).findByAuthor_IdOrderByCreatedAtDesc(userId, pageRequest);
    }

    @Test
    public void testGetWallCommentsCreatedByWallOwner() {
        long userId = 2L;
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<WallComment> expectedPage = new PageImpl<>(Arrays.asList(new WallComment()));

        when(wallCommentRepository.findByWallOwner_IdOrderByCreatedAtDesc(userId, pageRequest))
                .thenReturn(expectedPage);

        Page<WallComment> result = underTest.getWallCommentsCreatedByWallOwner(userId, pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(wallCommentRepository, times(1)).findByWallOwner_IdOrderByCreatedAtDesc(userId, pageRequest);
    }

    @Test
    public void testAddSuccess() {
        WallComment comment = new WallComment();
        User wallOwner = new User();
        wallOwner.setUsername("owner");
        User author = new User();
        author.setUsername("author");
        comment.setWallOwner(wallOwner);
        comment.setAuthor(author);
        comment.setContent("Test comment");

        when(wallCommentRepository.save(comment)).thenReturn(comment);

        WallComment result = underTest.add(comment);

        assertNotNull(result);
        verify(wallCommentRepository, times(1)).save(comment);
    }

    @Test
    public void testAddThrowsExceptionWhenWallOwnerIsNull() {
        WallComment comment = new WallComment();
        comment.setWallOwner(null);
        comment.setAuthor(new User());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            underTest.add(comment);
        });

        assertEquals("Es ist ein Fehler beim laden der nötigen Informationen aufgetreten", exception.getMessage());
        verify(wallCommentRepository, never()).save(any());
    }

    @Test
    public void testAddThrowsExceptionWhenAuthorIsNull() {
        WallComment comment = new WallComment();
        comment.setWallOwner(new User());
        comment.setAuthor(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            underTest.add(comment);
        });

        assertEquals("Es ist ein Fehler beim laden der nötigen Informationen aufgetreten", exception.getMessage());
        verify(wallCommentRepository, never()).save(any());
    }

    @Test
    public void testAddThrowsExceptionWhenAuthorAndWallOwnerAreSame() {
        WallComment comment = new WallComment();
        User user = new User();
        user.setUsername("sameUser");
        comment.setWallOwner(user);
        comment.setAuthor(user);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            underTest.add(comment);
        });

        assertEquals("Du kannst nicht auf deiner eigenen Pinnwand kommentieren", exception.getMessage());
        verify(wallCommentRepository, never()).save(any());
    }

    @Test
    public void testToListByAuthor() {
        Long authorId = 1L;
        List<WallComment> expectedList = Arrays.asList(new WallComment(), new WallComment());

        when(wallCommentRepository.toListByAuthor(authorId)).thenReturn(expectedList);

        List<WallComment> result = underTest.toListByAuthor(authorId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(wallCommentRepository, times(1)).toListByAuthor(authorId);
    }

    @Test
    public void testToListByWallOwner() {
        Long ownerId = 2L;
        List<WallComment> expectedList = Arrays.asList(new WallComment());

        when(wallCommentRepository.toListByWallOwner(ownerId)).thenReturn(expectedList);

        List<WallComment> result = underTest.toListByWallOwner(ownerId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(wallCommentRepository, times(1)).toListByWallOwner(ownerId);
    }

    @Test
    public void testSeeMyPinwall() {
        User loggedInUser = new User();
        loggedInUser.setUsername("loggedInUser");

        when(context.getBean("loggedInUser")).thenReturn(loggedInUser);

        Page<WallComment> result = underTest.seeMyPinwall();

        assertNull(result); // Da seePinwall null zurückgibt
        verify(context, times(1)).getBean("loggedInUser");
    }

    @Test
    public void testSeePinwall() {
        User pinwallOwner = new User();
        pinwallOwner.setUsername("owner");

        Page<WallComment> result = underTest.seePinwall(pinwallOwner);

        assertNull(result); // Da die Methode null zurückgibt
    }
}

package org.fpj.wall.application;

import org.fpj.wall.domain.WallCommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class WallCommentManagingServiceTest {

    @Mock
    private WallCommentRepository wallComments;

    @InjectMocks
    private WallCommentManagingService underTest;

    @Test
    public void testConstructor() {
        assertNotNull(underTest);
    }

    // Da alle Methoden auskommentiert sind, gibt es aktuell keine weiteren Tests
    // Die Testklasse ist vorbereitet für zukünftige Implementierungen
}

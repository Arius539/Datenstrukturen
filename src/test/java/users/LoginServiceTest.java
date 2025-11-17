package users;

import org.fpj.users.application.LoginService;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoginServiceTest {

    @Mock
    UserService userService;

    @Mock
    GenericApplicationContext context;

    @Mock
    User user;

    @InjectMocks
    LoginService underTest;

    private static final String USERNAME = "testUsername@abc.de";
    private static final String RAW_PASSWORD = "password123";
    private static String hashedPassword;

    @BeforeAll
    public static void init(){
        final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        hashedPassword = passwordEncoder.encode(RAW_PASSWORD);
    }


    @Test
    public void testLoginSuccessful(){
        //Mocks
        when(userService.findByUsername(USERNAME)).thenReturn(user);
        when(user.getPasswordHash()).thenReturn(hashedPassword);
        doNothing().when(context).registerBean(anyString(), any(), eq(user));

        //Ausführen der Methode
        underTest.login(USERNAME, RAW_PASSWORD);

        //verifications
        verify(context, times(1)).registerBean(anyString(), any(), eq(user));
    }
}

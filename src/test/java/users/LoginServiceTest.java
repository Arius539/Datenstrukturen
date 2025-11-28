package users;

import org.fpj.exceptions.LoginFailedException;
import org.fpj.users.application.LoginService;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
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
    ConfigurableApplicationContext context;

    @Mock
    ConfigurableListableBeanFactory beanFactory;

    @Mock
    User user;

    @InjectMocks
    LoginService underTest;

    private static final String USERNAME = "testUsername@abc.de";
    private static final String USERNAMENOTPRESENT = "testUsernameNotPresent@abc.de";
    private static final String USERNAMENOTVALIDREGEX = "testUserNotValid.de";
    private static final String RAW_PASSWORD = "Password123$";
    private static final String RAW_PASSWORDNOTMATCHING = "Password1234$";
    private static String hashedPassword;
    private static final String REGEX_USERNAME_VALIDATOR =
            "^(?!.*\\.\\.)(?=.{8,255})[A-Za-z0-9_%.+-]{2,64}@[A-Za-z0-9.-]{2,}\\.[A-Za-z]{2,}$";

    @BeforeAll
    public static void init(){
        final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        hashedPassword = passwordEncoder.encode(RAW_PASSWORD);
    }


    @Test
    public void testLoginSuccessful(){
        //Mocks
        when(userService.findByUsername(USERNAME.toLowerCase())).thenReturn(user);
        when(user.getPasswordHash()).thenReturn(hashedPassword);
        when(context.getBeanFactory()).thenReturn(beanFactory);
        doNothing().when(beanFactory).registerSingleton(anyString(), eq(user));
        //Ausführen der Methode
        underTest.login(USERNAME, RAW_PASSWORD);
        //verifications
        verify(beanFactory, times(1)).registerSingleton(anyString(), eq(user));
    }

    @Test
    public void testLoginFailedNoUserFound(){
        //Mocks
        doThrow(LoginFailedException.class).when(userService).findByUsername(USERNAMENOTPRESENT.toLowerCase());
        //Ausführen der Methode
        Assertions.assertThrows(LoginFailedException.class,()->underTest.login(USERNAMENOTPRESENT, RAW_PASSWORD));
        //verifications
        verify(beanFactory, times(0)).registerSingleton(anyString(), eq(user));
    }

    @Test
    public void testLoginFailedWrongPassword(){
        when(userService.findByUsername(USERNAME.toLowerCase())).thenReturn(user);
        when(user.getPasswordHash()).thenReturn(hashedPassword);

        Assertions.assertThrows(LoginFailedException.class, ()->underTest.login(USERNAME, RAW_PASSWORDNOTMATCHING));
        verify(beanFactory, times(0)).registerSingleton(anyString(), eq(user));
        verify(userService, times(1)).findByUsername(USERNAME.toLowerCase());
    }

    @Test
    public void testRegisterSuccessful() {
        when(userService.usernameExists(USERNAME.toLowerCase())).thenReturn(false);
        when(userService.save(any())).thenReturn(user);
        underTest.register(USERNAME, RAW_PASSWORD, RAW_PASSWORD);
        verify(userService, times(1)).save(any());
    }

    @Test
    public void testRegisterFailedUsernameRegex() {
        when(userService.usernameExists(USERNAMENOTVALIDREGEX.toLowerCase())).thenReturn(false);
        Assertions.assertThrows(LoginFailedException.class,()-> underTest.register(USERNAMENOTVALIDREGEX, RAW_PASSWORD, RAW_PASSWORD));
        verify(userService, times(0)).save(user);
    }

    @Test
    public void testRegisterPasswordNotEqual() {
        when(userService.usernameExists(USERNAME.toLowerCase())).thenReturn(false);
        Assertions.assertThrows(LoginFailedException.class,()-> underTest.register(USERNAME, RAW_PASSWORD, RAW_PASSWORDNOTMATCHING));
        verify(userService, times(0)).save(user);
    }

    @Test
    public void testRegisterUsernameAlreadyExists() {
        when(userService.usernameExists(USERNAME.toLowerCase())).thenReturn(true);
        Assertions.assertThrows(LoginFailedException.class,()-> underTest.register(USERNAME, RAW_PASSWORD, RAW_PASSWORDNOTMATCHING));

        verify(userService, times(0)).save(user);
    }

    @Test
    public void testRegisterPasswordNotValid() {
        when(userService.usernameExists(USERNAME.toLowerCase())).thenReturn(false);
        Assertions.assertThrows(LoginFailedException.class,()-> underTest.register(USERNAME, "abcd", "abcd"));

        verify(userService, times(0)).save(user);
    }
}

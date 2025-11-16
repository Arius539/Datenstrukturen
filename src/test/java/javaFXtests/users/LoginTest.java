package javaFXtests.users;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.fpj.AlertService;
import org.fpj.ViewNavigator;
import org.fpj.javafxController.LoginController;
import org.fpj.users.application.LoginService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.GenericApplicationContext;

import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoginTest {

    private static final String USERNAME = "testUsername@abc.de";
    private static final String RAW_PASSWORD = "password123";

    private static final String REGISTER_STRING = "registrieren";
    private static final String LOGIN_STRING = "anmelden";
    private static final String NO_ACCOUNT = "noch kein Konto?";
    private static final String ACCOUNT_EXISTENT = "du hast bereits ein Konto?";

    private Button loginBtn;
    private Button toggleBtn;
    private PasswordField passwordInput;
    private PasswordField passwordCheck;
    private TextField usernameInput;

    @Mock
    LoginService loginService;
    @Mock
    AlertService alertService;
    @Mock
    GenericApplicationContext context;
    @Mock
    ViewNavigator viewNavigator;

    private LoginController underTest;

    @BeforeAll
    public static void init(){
        JfxTestInitializer.init();
    }

    @BeforeEach
    public void setUp(){
        underTest = new LoginController(context, viewNavigator, loginService, alertService);

        loginBtn = new Button();
        toggleBtn = new Button();
        passwordInput = new PasswordField();
        passwordCheck = new PasswordField();
        usernameInput = new TextField();

        underTest.setLoginButton(loginBtn);
        underTest.setPasswordInput(passwordInput);
        underTest.setUsernameInput(usernameInput);
        underTest.setPasswordCheck(passwordCheck);
        underTest.setToggleButton(toggleBtn);

        loginBtn.setText(REGISTER_STRING);
    }

    @Test
    void testSubmitRegister() throws Exception {
        usernameInput.setText(USERNAME);
        passwordInput.setText(RAW_PASSWORD);
        passwordCheck.setText(RAW_PASSWORD);

        doNothing().when(loginService).register(USERNAME, RAW_PASSWORD, RAW_PASSWORD);

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                underTest.submit(new ActionEvent(loginBtn, null));
            } catch (Exception e){
                throw new RuntimeException(e);
            }
            latch.countDown();
        });

        latch.await();

        verify(loginService, times(1)).register(USERNAME, RAW_PASSWORD, RAW_PASSWORD);

        Assertions.assertEquals(LOGIN_STRING, loginBtn.getText());
    }
}

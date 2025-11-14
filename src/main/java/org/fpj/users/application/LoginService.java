package org.fpj.users.application;

import org.fpj.Exceptions.LoginFailedException;
import org.fpj.users.domain.User;
import org.fpj.users.domain.UserManagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final GenericApplicationContext context;
    private final UserManagingService userManagingService;

    @Autowired
    public LoginService(GenericApplicationContext context, UserManagingService userManagingService){
        this.context = context;
        this.userManagingService = userManagingService;
    }

    public void login(final String username, final String password) {
        final User user = userManagingService.getUserByUsername(username);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (passwordEncoder.matches(password, user.getPasswordHash())){
            context.registerBean("loggedInUser", User.class, user);
        }
        else {
            throw new LoginFailedException("Passwort falsch");
        }
    }

    public void register(final String username, final String password, final String passwordCheck){
        if (passwordCheck.equals(password)){
            final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            final String hashedPassword = passwordEncoder.encode(password);
            final User newUser = new User(username, hashedPassword);
            final User savedUser = userManagingService.save(newUser);
        }
        else {
            throw new LoginFailedException("Passwörter stimmen nicht überein");
        }
    }
}

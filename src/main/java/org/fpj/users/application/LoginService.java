package org.fpj.users.application;

import org.fpj.Exceptions.DataNotPresentException;
import org.fpj.Exceptions.LoginFailedException;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginService {

    private final GenericApplicationContext context;
    private final UserService userService;

    @Autowired
    public LoginService(GenericApplicationContext context, UserService userService){
        this.context = context;
        this.userService = userService;
    }

    public void login(final String username, final String password) {
        final User user;
        try {
            user = userService.findByUsername(username);
        }
        catch (DataNotPresentException e){
            throw new LoginFailedException("Kein User mit Username " + username + " vorhanden.");
        }

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
            final User savedUser = userService.save(newUser);
        }
        else {
            throw new LoginFailedException("Passwörter stimmen nicht überein");
        }
    }
}

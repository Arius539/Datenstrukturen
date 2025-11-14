package org.fpj.users.domain;

import org.fpj.Exceptions.DataNotPresentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserManagingService {

    private final UserRepository userRepo;

    @Autowired
    public UserManagingService(UserRepository userRepo){
        this.userRepo = userRepo;
    }

    public User getUserByUsername(final String username){
        Optional<User> user = userRepo.findByUsername(username);
        if (user.isPresent()){
            return user.get();
        }
        throw new DataNotPresentException("Kein User mit Username " + username + " gefunden.");
    }

    public User save(final User user){
        return userRepo.save(user);
    }
}

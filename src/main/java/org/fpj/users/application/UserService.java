package org.fpj.users.application;

import org.fpj.Exceptions.DataNotPresentException;
import org.fpj.users.domain.User;
import org.fpj.users.domain.UserRepository;
import org.fpj.users.domain.UsernameOnly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepo;

    @Autowired
    public UserService(UserRepository userRepo){
        this.userRepo = userRepo;
    }

    //TODO: wir sollten meiner Meinung nach das Optional-Handling schon hier machen und die Exception im Service/ Controller behandeln
    public User findByUsername(final String username){
        Optional<User> user = userRepo.findByUsername(username);
        if (user.isPresent()){
            return user.get();
        }
        throw new DataNotPresentException("Kein User mit Username " + username + " gefunden.");
    }

    public User save(final User user){
        return userRepo.save(user);
    }

    public Page<User> findContacts(User user, Pageable pageable){
        return userRepo.findContactsOrderByLastMessageDesc(user.getId(), pageable);
    }

    public List<String> usernameContaining(String username){
        return userRepo.findTop10ByUsernameContainingIgnoreCaseOrderByUsernameAsc(
                username).stream().map(UsernameOnly::getUsername).toList();
    }

    //nur zu Testzwecken
    @Transactional(readOnly = true)
    public User currentUser() {
        Optional<User> user= userRepo.findByUsername("test1@test.com");
        if(user.isPresent()) {
            return user.get();
        }
        throw new DataNotPresentException("User not found");
    }
}

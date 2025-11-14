package org.fpj.users.application;

import org.fpj.Exceptions.DataNotPresentException;
import org.fpj.users.domain.User;
import org.fpj.users.domain.UserRepository;
import org.fpj.users.domain.UsernameOnly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User currentUser() {
      Optional<User> user= userRepository.findByUsername("test2@test.com");
      if(user.isPresent()) {
          return user.get();
      }
        throw new DataNotPresentException("User not found");
    }

    public Optional<User> findByUsername(String username){
       return userRepository.findByUsername(username);
    }

    public Page<User> findContacts(User user, Pageable pageable){
       return userRepository.findContactsOrderByLastMessageDesc(user.getId(), pageable);
    }

    public List<String> usernameContaining(String username){
        return userRepository.findTop10ByUsernameContainingIgnoreCaseOrderByUsernameAsc(username).stream()
                .map(UsernameOnly::getUsername)
                .toList();
    }
}

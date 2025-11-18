package org.fpj.users.application;

import org.fpj.Exceptions.DataNotPresentException;
import org.fpj.users.domain.ConversationMessageView;
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

    public User findByUsername(final String username){
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()){
            return user.get();
        }
        throw new DataNotPresentException("Kein User mit Username " + username + " gefunden.");
    }

    public User save(final User user){
        return userRepository.save(user);
    }

    public Page<User> findContacts(User user, Pageable pageable){
        return userRepository.findContactsOrderByLastMessageDesc(user.getId(), pageable);
    }

    public List<String> usernameContaining(String username){
        return userRepository.findTop10ByUsernameContainingIgnoreCaseOrderByUsernameAsc(
                username).stream().map(UsernameOnly::getUsername).toList();
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public List<ConversationMessageView> getConversationMessageView(Long userId1, Long userId2){
       return this.userRepository.findConversationBetweenUsers(userId1, userId2);
    }
}

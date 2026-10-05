package com.tableit.tableit.service;

import com.tableit.tableit.dto.user.UserRequest;
import com.tableit.tableit.dto.user.UserResponse;
import com.tableit.tableit.exception.ResourceNotFoundException;
import com.tableit.tableit.exception.UnauthorizedException;
import com.tableit.tableit.model.User;
import com.tableit.tableit.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public List<User> findByUsernames(List<String> usernames) {
        List<User> result = new ArrayList<>();
        for(String username : usernames) {
            result.add(findByUsername(username));
        }
        return result;
    }

    public Map<String, User> findMapByUsernames(List<String> usernames){
        Map<String, User> result = new HashMap<>();
        for(String username : usernames) {
            result.put(username, findByUsername(username));
        }
        return result;
    }

    public UserResponse getMe() {
        User currentUser = getCurrentUser();
        return buildUserResponse(currentUser);
    }

    @Transactional
    public UserResponse update(UserRequest request) {
        User user = getCurrentUser();

        log.info("User request: id={} username={} firstName={} lastName={}", user.getId(), user.getUsername(), request.getFirstName(), request.getLastName());

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        if (request.getSex() != null) {
            user.setSex(request.getSex());
        }

        saveUser(user);
        log.info("User updated: id={} username={} firstName={} lastName={}", user.getId(), user.getUsername(), user.getFirstName(), user.getLastName());
        
        return buildUserResponse(user);
    }

    private UserResponse buildUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .sex(user.getSex())
                .build();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Invalid token");
        }
        String username = auth.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }


}

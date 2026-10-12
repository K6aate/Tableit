package com.tableit.userservice.service;

import com.tableit.userservice.dto.user.UserResponse;
import com.tableit.userservice.exception.ResourceNotFoundException;
import com.tableit.userservice.exception.UnauthorizedException;
import com.tableit.userservice.model.User;
import com.tableit.userservice.repository.UserRepository;
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

    private UserResponse buildUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
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

package com.example.backend.service;

import com.example.backend.entity.AppUser;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser requireUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new NotFoundException("ログインが必要です。");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new NotFoundException("利用者が見つかりません。"));
    }

    public AppUser requireUserForUpdate(Authentication authentication) {
        AppUser user = requireUser(authentication);
        return userRepository.findByIdForUpdate(user.getUserId())
                .orElseThrow(() -> new NotFoundException("利用者が見つかりません。"));
    }
}

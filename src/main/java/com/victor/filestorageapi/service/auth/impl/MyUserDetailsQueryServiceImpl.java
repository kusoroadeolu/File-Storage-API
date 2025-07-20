package com.victor.filestorageapi.service.auth.impl;

import com.victor.filestorageapi.exception.UserNotFoundException;
import com.victor.filestorageapi.models.entities.User;
import com.victor.filestorageapi.models.entities.UserPrincipal;
import com.victor.filestorageapi.repository.UserRepository;
import com.victor.filestorageapi.service.auth.MyUserDetailsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MyUserDetailsQueryServiceImpl implements MyUserDetailsQueryService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user =  userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                String.format("User with username '%s' was not found", username)
                        )
                );
        return new UserPrincipal(user);
    }
}

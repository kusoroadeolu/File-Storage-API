package com.victor.filestorageapi.service.impl;

import com.victor.filestorageapi.models.User;
import com.victor.filestorageapi.models.UserPrincipal;
import com.victor.filestorageapi.repository.UserRepository;
import com.victor.filestorageapi.service.MyUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MyUserDetailsServiceImpl implements MyUserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user =  userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                String.format("User with username '%s' was not found", username)
                        )
                );
        return new UserPrincipal(user);
    }
}

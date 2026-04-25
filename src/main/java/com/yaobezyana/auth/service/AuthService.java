package com.yaobezyana.auth.service;

import com.yaobezyana.auth.dto.AuthResponse;
import com.yaobezyana.auth.dto.LoginRequest;
import com.yaobezyana.auth.dto.RegisterRequest;
import com.yaobezyana.auth.security.JwtTokenProvider;
import com.yaobezyana.sprint.service.SprintService;
import com.yaobezyana.user.entity.User;
import com.yaobezyana.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SprintService sprintService;

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .ipAddress(ipAddress)
                .build();

        User saved = userRepository.save(user);
        sprintService.createNewSprintForUser(saved);

        return AuthResponse.builder()
                .token(jwtTokenProvider.generateToken(user.getEmail()))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return AuthResponse.builder()
                .token(jwtTokenProvider.generateToken(user.getEmail()))
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}

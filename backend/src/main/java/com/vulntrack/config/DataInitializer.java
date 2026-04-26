package com.vulntrack.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.vulntrack.enums.IssueStatus;
import com.vulntrack.enums.Role;
import com.vulntrack.enums.Severity;
import com.vulntrack.model.Issue;
import com.vulntrack.model.SecurityUser;
import com.vulntrack.repository.IssueRepository;
import com.vulntrack.repository.UserRepository;
import com.vulntrack.service.JwtService;

import lombok.RequiredArgsConstructor;

import com.vulntrack.service.IssueService;

import com.vulntrack.model.User;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final IssueService issueService;
    private final IssueRepository issueRepository;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {

        // Create one of each Role for testing
        User admin = createUser("admin", Role.ROLE_ADMIN);
        User researcher = createUser("researcher", Role.ROLE_SECURITY_ADMIN);
        User dev1 = createUser("dev1", Role.ROLE_DEVELOPER);
        User dev2 = createUser("dev2", Role.ROLE_DEVELOPER);

        Random rand = new Random();
        List<User> developers = List.of(dev1, dev2);

        // Create 10 issues owned by the Researcher
        for (int i = 1; i <= 10; i++) {
            Severity severity = Severity.values()[rand.nextInt(Severity.values().length)];

            Issue issue = Issue.builder()
                .title("Vulnerability " + i)
                .description("Automated security finding #" + i)
                .severity(severity)
                .status(IssueStatus.OPEN)
                .owner(researcher) // The researcher found it
                .assignee(developers.get(rand.nextInt(developers.size())))
                .build();

            issueService.createInternal(issue);
        }

        System.out.println("=== Test Data Initialized ===");
        System.out.println("Admin Login -> admin : password");
        System.out.println("Dev1 Login  -> dev1 : password");
        System.out.println("Dev2 Login  -> dev2 : password");
    }

    private User createUser(String username, Role role) {

        return userRepository.findByUsername(username).orElseGet(() -> {
            String encodedPassword = passwordEncoder.encode("password");

            User user = User.builder()
                .username(username)
                .email(username + "@vulntrack.com")
                .password(encodedPassword)
                .role(role)
                .token("init-token")
                .lastLogin(System.currentTimeMillis())
                .build();

            System.out.println("Creating new test user: " + username);
            return userRepository.save(user);
        });
    }
}
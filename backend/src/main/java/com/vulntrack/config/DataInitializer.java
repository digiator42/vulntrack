package com.vulntrack.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.vulntrack.enums.IssueStatus;
import com.vulntrack.enums.Role;
import com.vulntrack.enums.Severity;
import com.vulntrack.model.Issue;
import com.vulntrack.repository.UserRepository;
import com.vulntrack.service.JwtService;
import com.vulntrack.service.IssueService;

import lombok.RequiredArgsConstructor;

import com.vulntrack.model.User;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final IssueService issueService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        createUsers();

        Random rand = new Random();

        Map<Integer, IssueStatus> statusTypes = new HashMap<>();
        statusTypes.put(0, IssueStatus.OPEN);
        statusTypes.put(1, IssueStatus.IN_PROGRESS);
        statusTypes.put(2, IssueStatus.RESOLVED);
        statusTypes.put(3, IssueStatus.CLOSED);

        for (int i = 1; i <= 25; i++) {
            int r = rand.nextInt(4);
            User owner = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("User not found"));
            Severity severity = Severity.values()[rand.nextInt(Severity.values().length)];
            Issue issue = new Issue("Valid title " + i, "Desc", severity, owner);
            issue.setStatus(statusTypes.get(r));
            issueService.create(issue, owner);
        }
    }

    public void createUsers() {
        for (Long i = 1L; i <= 5L; i++) {
            String username = "user" + i;
            String password = "password";
            String token = jwtService.generateToken(username, i, Role.ROLE_USER.name());
            userRepository.save(new User(i, username, password, Role.ROLE_USER));
            System.out
                .println("Username: " + username + ", Password: " + password + ", Token: " + token);
        }
    }
}
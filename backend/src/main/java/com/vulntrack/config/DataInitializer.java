package com.vulntrack.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.vulntrack.model.Issue;
import com.vulntrack.model.IssueStatus;
import com.vulntrack.security.JwtService;
import com.vulntrack.service.IssueService;

@Component
public class DataInitializer {

    private final IssueService issueService;
    private final JwtService jwtService;

    public DataInitializer(IssueService issueService, JwtService jwtService) {
        this.issueService = issueService;
        this.jwtService = jwtService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Random rand = new Random();

        Map<Integer, IssueStatus> statusTypes = new HashMap<>();
        statusTypes.put(0, IssueStatus.OPEN);
        statusTypes.put(1, IssueStatus.IN_PROGRESS);
        statusTypes.put(2, IssueStatus.RESOLVED);
        statusTypes.put(3, IssueStatus.CLOSED);

        // for (int i = 1; i <= 25; i++) {
        // int r = rand.nextInt(4);
        // Issue issue = new Issue(Long.valueOf(i), "Valid title " + i, "Desc", i, 10L);
        // issue.setStatus(statusTypes.get(r));
        // issueService.create(issue, 1L);
        // }
    }
}
package com.megastar.vulntrack.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.megastar.vulntrack.model.Issue;
import com.megastar.vulntrack.service.IssueService;

@Component
public class DataInitializer {

    private final IssueService issueService;

    public DataInitializer(IssueService issueService) {
        this.issueService = issueService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        for (int i = 1; i <= 25; i++) {
            issueService.create(
                    new Issue(Long.valueOf(i), "Walid title " + i, "Desc", i, 1L), 1L);
        }
    }
}
package com.megastar.vulntrack.service;


import com.megastar.vulntrack.model.Issue;
import com.megastar.vulntrack.repository.IssueRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IssueService {
    private final IssueRepository issueRepository = new IssueRepository();

    public Issue create(Issue issue) {
        return issueRepository.save(issue);
    }

    public List<Issue> getAll() {
        return issueRepository.findAll();
    }

    public Issue getById(long id) {
        return issueRepository.findById(id).orElse(null);
    }
}

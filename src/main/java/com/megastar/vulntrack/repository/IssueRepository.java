package com.megastar.vulntrack.repository;

import com.megastar.vulntrack.model.Issue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IssueRepository {
    private final Map<Long, Issue> store = new HashMap<>();
    private long counter;

    public Issue save(Issue issue) {
        issue.setId(counter++);
        store.put(issue.getId(), issue);
        return issue;
    }

    public Optional<Issue> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Issue> findAll() {
        return store.values().stream().toList();
    }
}

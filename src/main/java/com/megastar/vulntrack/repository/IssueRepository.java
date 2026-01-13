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
        System.out.println(issue.toString());
        return issue;
    }

    public Optional<Issue> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    public boolean existsByTitle(String title) {
        return store.values().stream().anyMatch(issue -> issue.getTitle().equals(title));
    }

    public List<Issue> findAll() {
        return store.values().stream().toList();
    }
}

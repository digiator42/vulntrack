package com.vulntrack.repository;

import com.vulntrack.model.Issue;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IssueRepositoryOldImpl {
    private final Map<Long, Issue> store = new HashMap<>();
    private long counter;

    public Issue save(Issue issue) {
        issue.setId(counter++);
        store.put(issue.getId(), issue);
        System.out.println(issue.toString());
        return issue;
    }

    public Optional<Issue> findById(Long id) { return Optional.ofNullable(store.get(id)); }

    public boolean existsByTitle(String title) {
        return store.values()
            .stream()
            .anyMatch(issue -> issue.getTitle().equals(title));
    }

    public List<Issue> findAll() { return store.values().stream().toList(); }

    public List<Issue> findAllByOwnerId(Long ownerId) {
        return store.values()
            .stream()
            .filter(issue -> issue.getOwnerId().equals(ownerId))
            .toList();
    }

    public List<Issue> listIssuesByOwnerId(
        int page,
        int size,
        String field,
        String direction,
        Long ownerId
    ) {
        // /issues?page=1&size=20&sort=severity,asc

        Comparator<Issue> comparator = switch (field) {
        case "severity" -> Comparator.comparing(Issue::getSeverity);
        case "title" -> Comparator.comparing(Issue::getTitle);
        default -> Comparator.comparing(Issue::getCreatedAt);
        };

        if (direction.equals("desc")) {
            comparator = comparator.reversed();
        }

        return store.values()
            .stream()
            .filter(v -> v.getOwnerId() == ownerId)
            .sorted(comparator)
            .skip(page * size)
            .limit(size)
            .toList();
    }

    public void delete(Long id) { store.remove(id); }
}

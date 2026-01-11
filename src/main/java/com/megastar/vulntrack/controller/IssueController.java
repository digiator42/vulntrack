package com.megastar.vulntrack.controller;

import com.megastar.vulntrack.model.Issue;
import com.megastar.vulntrack.service.IssueService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/issues")
public class IssueController {
    private final IssueService service;

    public IssueController(IssueService issueService) {
        this.service = issueService;
    }

    @PostMapping
    public Issue create(@RequestBody Issue issue) {
        System.out.println(issue.toString());
        return service.create(issue);
    }

    @GetMapping()
    public List<Issue> getAllIssues() {
        return  service.getAll();
    }

    @GetMapping("/{id}")
    public Issue findById(@PathVariable long id) {
        return service.getById(id);
    }
}

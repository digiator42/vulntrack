package com.megastar.vulntrack.controller;

import com.megastar.vulntrack.config.UserContext;
import com.megastar.vulntrack.dto.CreateIssueRequest;
import com.megastar.vulntrack.model.Issue;
import com.megastar.vulntrack.service.IssueService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/web")
public class WebController {

    private final IssueService issueService;
    private final UserContext userContext;

    public WebController(IssueService issueService, UserContext userContext) {
        this.issueService = issueService;
        this.userContext = userContext;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String username,
            @RequestParam String password,
            HttpSession session) {

        if ("admin".equals(username) && "password123".equals(password)) {
            session.setAttribute("LOGGED_IN_USER", username);
            session.setAttribute("USER_ID", 1L);
            return "redirect:/web/issues";
        }
        return "redirect:/web/login?error=true";
    }

    @GetMapping("/new")
    public String showCreateForm() {
        return "issue-form";
    }

    @PostMapping("/issues")
    public String createIssue(@Valid @ModelAttribute CreateIssueRequest request) {
        Long userId = userContext.getUserId();

        System.out.println("===>>> " + userId);

        Issue issue = new Issue(
                null,
                request.title(),
                request.description(),
                request.severity(),
                userId);

        issueService.create(issue, userId);
        return "redirect:/web/issues";
    }

    @GetMapping("/issues")
    public String listIssues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Long userId = userContext.getUserId();
        System.out.println("===>>> " + userId);
        List<Issue> issues = issueService.getAllByOwnerId(userId);

        model.addAttribute("issues", issues);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);

        return "issues";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> stats = issueService.getStats();
        model.addAttribute("stats", stats);
        return "dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/web/login?logout";
    }
}
package com.megastar.vulntrack.controller;

import com.megastar.vulntrack.config.UserContext;
import com.megastar.vulntrack.dto.CreateIssueRequest;
import com.megastar.vulntrack.dto.IssuePageRequest;
import com.megastar.vulntrack.dto.IssueResponse;
import com.megastar.vulntrack.model.Issue;
import com.megastar.vulntrack.service.IssueService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
    public String loginPage(HttpSession session) {

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/web/issues";
        }

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
    public String createIssue(@Valid @ModelAttribute CreateIssueRequest request, HttpSession session) {

        Long userId = userContext.getUserId();

        Issue issue = new Issue(
                null,
                request.title(),
                request.description(),
                request.severity(),
                userId);

        issueService.create(issue, userId);
        return "redirect:/web/issues";
    }


    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> stats = issueService.getStats();
        model.addAttribute("stats", stats);
        return "dashboard";
    }

    @GetMapping("/issues")
    public String listIssues(@Valid @ModelAttribute IssuePageRequest request, Model model)
            throws Exception {

        var page = issueService.getSortedListIssues(userContext.getUserId(), request.toPeagable());

        model.addAttribute("issues", page.getContent());
        model.addAttribute("currentPage", page.getNumber());
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("pageSize", page.getSize());

        return "issues";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/web/login?logout";
    }
}
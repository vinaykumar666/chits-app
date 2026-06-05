package com.ygc.controller;

import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HelpController extends BaseController {
    private final UserRepository userRepository;

    @GetMapping("/terms")
    public String terms(Authentication auth, Model model) {
        if (auth != null) {
            userRepository.findByEmail(auth.getName()).ifPresent(u -> model.addAttribute("user", u));
        }
        return "terms";
    }

    @GetMapping("/help")
    public String help(Authentication auth, Model model) {
        if (auth != null) {
            userRepository.findByEmail(auth.getName()).ifPresent(u -> model.addAttribute("user", u));
        }
        return "help";
    }

    @GetMapping("/member/help")
    public String memberHelp(Authentication auth, Model model) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        model.addAttribute("user", user);
        return "member/help";
    }

    @GetMapping("/member/terms")
    public String memberTerms(Authentication auth, Model model) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        model.addAttribute("user", user);
        return "terms";
    }
}

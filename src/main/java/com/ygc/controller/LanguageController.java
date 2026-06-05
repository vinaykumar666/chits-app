package com.ygc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/api/language")
public class LanguageController {

    @GetMapping("/available")
    @ResponseBody
    public ResponseEntity<List<String>> getAvailableLanguages() {
        return ResponseEntity.ok(Arrays.asList("en", "hi", "ta", "te", "kn"));
    }

    @PostMapping("/set/{lang}")
    @ResponseBody
    public ResponseEntity<String> setLanguage(@PathVariable String lang) {
        List<String> supported = Arrays.asList("en", "hi", "ta", "te", "kn");
        if (!supported.contains(lang)) {
            return ResponseEntity.badRequest().body("Unsupported language");
        }
        return ResponseEntity.ok(lang);
    }
}

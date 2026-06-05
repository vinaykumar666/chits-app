package com.ygc.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;
import java.util.Locale;

public class BaseController {

    @ModelAttribute
    public void addLanguageAttributes(Model model, Locale locale) {
        model.addAttribute("availableLanguages", Arrays.asList("en", "hi", "ta", "te", "kn"));
        String currentLang = locale.getLanguage();
        // Map full locale to language code
        if (currentLang.equalsIgnoreCase("en")) {
            model.addAttribute("currentLanguage", "en");
        } else if (currentLang.equalsIgnoreCase("hi")) {
            model.addAttribute("currentLanguage", "hi");
        } else if (currentLang.equalsIgnoreCase("ta")) {
            model.addAttribute("currentLanguage", "ta");
        } else if (currentLang.equalsIgnoreCase("te")) {
            model.addAttribute("currentLanguage", "te");
        } else if (currentLang.equalsIgnoreCase("kn")) {
            model.addAttribute("currentLanguage", "kn");
        } else {
            model.addAttribute("currentLanguage", "en");
        }
    }
}


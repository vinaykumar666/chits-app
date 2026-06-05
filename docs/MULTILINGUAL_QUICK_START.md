# Multilingual Quick Start Guide

## For End Users

### Switching Languages
1. Log in to YGC Internal
2. Look for the **globe icon** (🌐) in the top-right corner of the page
3. Click it to open the language dropdown menu
4. Select your preferred language:
   - 🇬🇧 English
   - 🇮🇳 हिन्दी (Hindi)
   - தமிழ் (Tamil)
   - తెలుగు (Telugu)
   - ಕನ್ನಡ (Kannada)
5. The page will reload automatically with the new language

### Language Persistence
- Your language choice is saved automatically
- It will be remembered for the next 1 year
- You can switch anytime by clicking the globe icon again

---

## For Developers

### Adding New Translation Keys

1. **Add to all property files:**
   - `src/main/resources/messages.properties` (English)
   - `src/main/resources/messages_hi.properties` (Hindi)
   - `src/main/resources/messages_ta.properties` (Tamil)
   - `src/main/resources/messages_te.properties` (Telugu)
   - `src/main/resources/messages_kn.properties` (Kannada)

2. **Use same key in all files:**
   ```
   example.key=English translation
   ```

3. **Use in Thymeleaf templates:**
   ```html
   <h1 th:text="#{example.key}">Default Text</h1>
   ```

4. **Use in Java code (if needed):**
   ```java
   String message = messageSource.getMessage("example.key", null, locale);
   ```

### Existing Translation Categories

- **app.*** - Application name and tagline
- **nav.*** - Navigation menu items
- **label.*** - Common UI labels
- **dashboard.*** - Dashboard-specific labels
- **msg.*** - Message types (success, error, warning, info)
- **form.*** - Form field labels

### Adding a New Language

1. Create `src/main/resources/messages_XX.properties` (where XX is language code)
2. Add all keys from the English version
3. No configuration changes needed - Spring auto-detects it
4. Add language option to fragments.html language dropdown:
   ```html
   <span th:case="'xx'">🚩 Language Name</span>
   ```

---

## Architecture Overview

```
User selects language in dropdown
        ↓
changeLanguage() JavaScript function called
        ↓
Sets ygc_lang cookie + calls API
        ↓
Page reloads
        ↓
LocaleChangeInterceptor reads ygc_lang parameter
        ↓
CookieLocaleResolver sets Locale object
        ↓
Thymeleaf uses #{} syntax to fetch translations
        ↓
MessageSource reads from messages_XX.properties
        ↓
Translated page rendered
```

---

## Files Changed/Created

### New Files:
- `src/main/java/com/ygc/config/LocaleConfig.java` - Spring i18n config
- `src/main/java/com/ygc/controller/BaseController.java` - Base for all controllers
- `src/main/java/com/ygc/controller/LanguageController.java` - Language API endpoints
- `src/main/resources/messages.properties` - English translations
- `src/main/resources/messages_hi.properties` - Hindi translations
- `src/main/resources/messages_ta.properties` - Tamil translations
- `src/main/resources/messages_te.properties` - Telugu translations
- `src/main/resources/messages_kn.properties` - Kannada translations

### Modified Files:
- `src/main/resources/application.properties` - Added i18n config
- `src/main/resources/static/js/app.js` - Added changeLanguage() function
- `src/main/resources/templates/fragments.html` - Added language dropdown (already existed)
- All controller classes - Extended BaseController

---

## Testing Checklist

- [ ] Build completes successfully (`mvn clean package`)
- [ ] No compilation errors related to LocaleConfig
- [ ] Can log in to the application
- [ ] Language dropdown appears in top-right corner
- [ ] Can select English, Hindi, Tamil, Telugu, or Kannada
- [ ] Page reloads when language is selected
- [ ] Language persists on page refresh
- [ ] Navigation menu items are translated
- [ ] Dashboard labels are translated
- [ ] Can switch between languages multiple times

---

## Troubleshooting

### Language dropdown not appearing
- Check that `currentLanguage` and `availableLanguages` are passed to the template
- Verify controller extends BaseController
- Check browser console for JavaScript errors

### Translations not showing
- Clear browser cache and cookies
- Verify property files have the correct key
- Check message key spelling in template (case-sensitive)
- Ensure messages.properties has the English fallback

### Language not persisting
- Check browser cookie settings (not in private/incognito mode)
- Verify CookieLocaleResolver bean is configured
- Check cookie name: `ygc_lang`

---

## Next Steps

- Consider adding more languages as needed
- Implement date/number formatting by locale
- Add language-specific email templates
- Consider client-side translation library for dynamic content


package com.ygc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to support multilingual messages for Indian languages
 * Supports: English, Hindi, Tamil, Telugu, Kannada, and other Indian languages
 */
@Service
@RequiredArgsConstructor
public class MultilingualMessageService {

    private static final Map<String, Map<String, String>> LANGUAGE_MESSAGES = new HashMap<>();

    static {
        initializeMessages();
    }

    private static void initializeMessages() {
        // English messages
        Map<String, String> enMessages = new HashMap<>();
        enMessages.put("document.approval.pending", "Your document is pending admin approval");
        enMessages.put("document.approval.approved", "Your document has been approved by admin");
        enMessages.put("document.approval.rejected", "Your document has been rejected. Reason: {reason}");
        enMessages.put("document.upload.success", "Document uploaded successfully");
        enMessages.put("document.upload.error", "Error uploading document");
        enMessages.put("document.view.title", "Document Review and Approval");
        enMessages.put("document.member.name", "Member Name");
        enMessages.put("document.type", "Document Type");
        enMessages.put("document.status", "Document Status");
        enMessages.put("document.uploaded.date", "Uploaded Date");
        enMessages.put("document.approve", "Approve");
        enMessages.put("document.reject", "Reject");
        enMessages.put("document.comments", "Approval Comments");
        enMessages.put("document.rejection.reason", "Rejection Reason");
        enMessages.put("document.no.pending", "No pending documents for approval");
        enMessages.put("email.document.approval.subject", "Document Approval - YGC Internal");
        enMessages.put("email.document.approval.body", "Dear {name},\\n\\nYour document has been approved.\\n\\nDocument: {document}\\nStatus: Approved\\n\\nRegards,\\nYGC Internal Team");
        enMessages.put("email.document.rejection.subject", "Document Rejection - YGC Internal");
        enMessages.put("email.document.rejection.body", "Dear {name},\\n\\nYour document has been rejected.\\n\\nDocument: {document}\\nReason: {reason}\\n\\nPlease resubmit the document.\\n\\nRegards,\\nYGC Internal Team");
        LANGUAGE_MESSAGES.put("en", enMessages);

        // Hindi messages
        Map<String, String> hiMessages = new HashMap<>();
        hiMessages.put("document.approval.pending", "आपके दस्तावेज़ को व्यवस्थापक की मंजूरी के लिए प्रतीक्षा है");
        hiMessages.put("document.approval.approved", "आपके दस्तावेज़ को व्यवस्थापक द्वारा मंजूरी दे दी गई है");
        hiMessages.put("document.approval.rejected", "आपके दस्तावेज़ को अस्वीकार कर दिया गया है। कारण: {reason}");
        hiMessages.put("document.upload.success", "दस्तावेज़ सफलतापूर्वक अपलोड हो गया");
        hiMessages.put("document.upload.error", "दस्तावेज़ अपलोड करने में त्रुटि");
        hiMessages.put("document.view.title", "दस्तावेज़ समीक्षा और मंजूरी");
        hiMessages.put("document.member.name", "सदस्य का नाम");
        hiMessages.put("document.type", "दस्तावेज़ का प्रकार");
        hiMessages.put("document.status", "दस्तावेज़ की स्थिति");
        hiMessages.put("document.uploaded.date", "अपलोड की तारीख");
        hiMessages.put("document.approve", "मंजूरी दें");
        hiMessages.put("document.reject", "अस्वीकार करें");
        hiMessages.put("document.comments", "मंजूरी टिप्पणियां");
        hiMessages.put("document.rejection.reason", "अस्वीकृति का कारण");
        hiMessages.put("document.no.pending", "मंजूरी के लिए कोई लंबित दस्तावेज़ नहीं");
        hiMessages.put("email.document.approval.subject", "दस्तावेज़ मंजूरी - YGC Internal");
        hiMessages.put("email.document.approval.body", "प्रिय {name},\\n\\nआपके दस्तावेज़ को मंजूरी दे दी गई है।\\n\\nदस्तावेज़: {document}\\nस्थिति: मंजूर\\n\\nशुभकामनाएं,\\nYGC Internal टीम");
        hiMessages.put("email.document.rejection.subject", "दस्तावेज़ अस्वीकृति - YGC Internal");
        hiMessages.put("email.document.rejection.body", "प्रिय {name},\\n\\nआपके दस्तावेज़ को अस्वीकार कर दिया गया है।\\n\\nदस्तावेज़: {document}\\nकारण: {reason}\\n\\nकृपया दस्तावेज़ को फिर से जमा करें।\\n\\nशुभकामनाएं,\\nYGC Internal टीम");
        LANGUAGE_MESSAGES.put("hi", hiMessages);

        // Tamil messages
        Map<String, String> taMessages = new HashMap<>();
        taMessages.put("document.approval.pending", "உங்கள் ஆவணம் நிர்வாகியின் ஒப்புதலுக்காக நிலுவையில் உள்ளது");
        taMessages.put("document.approval.approved", "உங்கள் ஆவணம் நிர்வாகி மூலம் ஒப்புக் கொள்ளப்பட்டுள்ளது");
        taMessages.put("document.approval.rejected", "உங்கள் ஆவணம் நிராகரிக்கப்பட்டுள்ளது. காரணம்: {reason}");
        taMessages.put("document.upload.success", "ஆவணம் வெற்றிகரமாக பதிவேற்றப்பட்டுள்ளது");
        taMessages.put("document.upload.error", "ஆவணம் பதிவேற்றுவதில் பிழை");
        taMessages.put("document.view.title", "ஆவண மதிப்பாய்வு மற்றும் ஒப்புதல்");
        taMessages.put("document.member.name", "உறுப்பினர் பெயர்");
        taMessages.put("document.type", "ஆவணத்தின் வகை");
        taMessages.put("document.status", "ஆவணத்தின் நிலை");
        taMessages.put("document.uploaded.date", "பதிவேற்ற தேதி");
        taMessages.put("document.approve", "ஒப்புக் கொள்ளவும்");
        taMessages.put("document.reject", "நிராகரிக்கவும்");
        taMessages.put("document.comments", "ஒப்புதல் கருத்துகள்");
        taMessages.put("document.rejection.reason", "நிராகரிப்பின் காரணம்");
        taMessages.put("document.no.pending", "ஒப்புதலுக்கான நிலுவையான ஆவணங்கள் இல்லை");
        taMessages.put("email.document.approval.subject", "ஆவண ஒப்புதல் - YGC Internal");
        taMessages.put("email.document.approval.body", "அன்பார்ந்த {name},\\n\\nஉங்கள் ஆவணம் ஒப்புக் கொள்ளப்பட்டுள்ளது।\\n\\nஆவணம்: {document}\\nநிலை: ஒப்புக் கொள்ளப்பட்டது\\n\\nவணக்கம்,\\nYGC Internal குழு");
        taMessages.put("email.document.rejection.subject", "ஆவண நிராகரிப்பு - YGC Internal");
        taMessages.put("email.document.rejection.body", "அன்பார்ந்த {name},\\n\\nஉங்கள் ஆவணம் நிராகரிக்கப்பட்டுள்ளது।\\n\\nஆவணம்: {document}\\nகாரணம்: {reason}\\n\\nதயவுசெய்து ஆவணத்தை மீண்டும் சமர்ப்பிக்கவும்.\\n\\nவணக்கம்,\\nYGC Internal குழு");
        LANGUAGE_MESSAGES.put("ta", taMessages);

        // Telugu messages
        Map<String, String> teMessages = new HashMap<>();
        teMessages.put("document.approval.pending", "మీ పత్రం నిర్వాహక ఆమోదం కోసం పెండింగ్ ఉంది");
        teMessages.put("document.approval.approved", "మీ పత్రం నిర్వాహకచే ఆమోదించబడింది");
        teMessages.put("document.approval.rejected", "మీ పత్రం తిరస్కరించబడింది. కారణం: {reason}");
        teMessages.put("document.upload.success", "పత్రం విజయవంతంగా అపload్ చేయబడింది");
        teMessages.put("document.upload.error", "పత్రం అપload్ చేయడంలో లోపం");
        teMessages.put("document.view.title", "పత్రం సమీక్ష మరియు ఆమోదం");
        teMessages.put("document.member.name", "సభ్య పేరు");
        teMessages.put("document.type", "పత్రం రకం");
        teMessages.put("document.status", "పత్రం స్థితి");
        teMessages.put("document.uploaded.date", "అపload్ చేసిన తేదీ");
        teMessages.put("document.approve", "ఆమోదించండి");
        teMessages.put("document.reject", "తిరస్కరించండి");
        teMessages.put("document.comments", "ఆమోదన వ్యాఖ్యలు");
        teMessages.put("document.rejection.reason", "తిరస్కరణ కారణం");
        teMessages.put("document.no.pending", "ఆమోదం కోసం పెండింగ్ పత్రాలు లేవు");
        teMessages.put("email.document.approval.subject", "పత్రం ఆమోదం - YGC Internal");
        teMessages.put("email.document.approval.body", "ప్రియమైన {name},\\n\\nమీ పత్రం ఆమోదించబడింది.\\n\\nపత్రం: {document}\\nస్థితి: ఆమోదించబడింది\\n\\nధన్యవాదాలు,\\nYGC Internal టీమ్");
        teMessages.put("email.document.rejection.subject", "పత్రం తిరస్కరణ - YGC Internal");
        teMessages.put("email.document.rejection.body", "ప్రియమైన {name},\\n\\nమీ పత్రం తిరస్కరించబడింది.\\n\\nపత్రం: {document}\\nకారణం: {reason}\\n\\nపత్రాన్ని తిరిగి సమర్పించండి.\\n\\nధన్యవాదాలు,\\nYGC Internal టీమ్");
        LANGUAGE_MESSAGES.put("te", teMessages);

        // Kannada messages
        Map<String, String> knMessages = new HashMap<>();
        knMessages.put("document.approval.pending", "ನಿಮ್ಮ ದಸ್ತಾವೇಜ್ ನಿರ್ವಾಹಕ ಅನುಮೋದನೆಯ ಕಾರಣ ಬಾಕಿ ಇದೆ");
        knMessages.put("document.approval.approved", "ನಿಮ್ಮ ದಸ್ತಾವೇಜ್ ನಿರ್ವಾಹಕ ಅನುಮೋದಿಸಲಾಗಿದೆ");
        knMessages.put("document.approval.rejected", "ನಿಮ್ಮ ದಸ್ತಾವೇಜ್ ನಿರಾಕರಿಸಲಾಗಿದೆ. ಕಾರಣ: {reason}");
        knMessages.put("document.upload.success", "ದಸ್ತಾವೇಜ್ ಯಶಸ್ವಿಯಾಗಿ ಅಪಲೋಡ್ ಮಾಡಲಾಗಿದೆ");
        knMessages.put("document.upload.error", "ದಸ್ತಾವೇಜ್ ಅಪಲೋಡ್ ಮಾಡುವಾಗ ಹೋಂದಾಟ");
        knMessages.put("document.view.title", "ದಸ್ತಾವೇಜ್ ಪರಿಶೀಲನೆ ಮತ್ತು ಅನುಮೋದನೆ");
        knMessages.put("document.member.name", "ಸದಸ್ಯ ಹೆಸರು");
        knMessages.put("document.type", "ದಸ್ತಾವೇಜ್ ಪ್ರಕಾರ");
        knMessages.put("document.status", "ದಸ್ತಾವೇಜ್ ಸ್ಥಿತಿ");
        knMessages.put("document.uploaded.date", "ಅಪಲೋಡ್ ಮಾಡಿದ ದಿನಾಂಕ");
        knMessages.put("document.approve", "ಅನುಮೋದಿಸಿ");
        knMessages.put("document.reject", "ನಿರಾಕರಿಸಿ");
        knMessages.put("document.comments", "ಅನುಮೋದನೆ ಪ್ರಸ್ತಾವನೆಗಳು");
        knMessages.put("document.rejection.reason", "ನಿರಾಕರಣೆ ಕಾರಣ");
        knMessages.put("document.no.pending", "ಅನುಮೋದನೆಗಾಗಿ ಬಾಕಿ ದಸ್ತಾವೇಜುಗಳಿಲ್ಲ");
        knMessages.put("email.document.approval.subject", "ದಸ್ತಾವೇಜ್ ಅನುಮೋದನೆ - YGC Internal");
        knMessages.put("email.document.approval.body", "ಪ್ರಿಯ {name},\\n\\nನಿಮ್ಮ ದಸ್ತಾವೇಜ್ ಅನುಮೋದಿಸಲಾಗಿದೆ.\\n\\nದಸ್ತಾವೇಜ್: {document}\\nಸ್ಥಿತಿ: ಅನುಮೋದಿತ\\n\\nಧನ್ಯವಾದಗಳು,\\nYGC Internal ತಂಡ");
        knMessages.put("email.document.rejection.subject", "ದಸ್ತಾವೇಜ್ ನಿರಾಕರಣೆ - YGC Internal");
        knMessages.put("email.document.rejection.body", "ಪ್ರಿಯ {name},\\n\\nನಿಮ್ಮ ದಸ್ತಾವೇಜ್ ನಿರಾಕರಿಸಲಾಗಿದೆ.\\n\\nದಸ್ತಾವೇಜ್: {document}\\nಕಾರಣ: {reason}\\n\\nದಸ್ತಾವೇಜ್ ಮರಳಿ ಸಲ್ಲಿಸಿ.\\n\\nಧನ್ಯವಾದಗಳು,\\nYGC Internal ತಂಡ");
        LANGUAGE_MESSAGES.put("kn", knMessages);
    }

    /**
     * Get a message in the specified language
     * Falls back to English if language not found
     */
    public String getMessage(String language, String messageKey) {
        String normalizedLanguage = language != null ? language.toLowerCase() : "en";
        Map<String, String> messages = LANGUAGE_MESSAGES.getOrDefault(normalizedLanguage, LANGUAGE_MESSAGES.get("en"));
        return messages.getOrDefault(messageKey, messageKey);
    }

    /**
     * Get a message with parameter replacement
     */
    public String getMessageWithParams(String language, String messageKey, Map<String, String> params) {
        String message = getMessage(language, messageKey);
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    /**
     * Get available languages
     */
    public java.util.List<String> getAvailableLanguages() {
        return new java.util.ArrayList<>(LANGUAGE_MESSAGES.keySet());
    }

    /**
     * Get language display name
     */
    public String getLanguageDisplayName(String languageCode) {
        return switch (languageCode.toLowerCase()) {
            case "en" -> "English";
            case "hi" -> "हिन्दी (Hindi)";
            case "ta" -> "தமிழ் (Tamil)";
            case "te" -> "తెలుగు (Telugu)";
            case "kn" -> "ಕನ್ನಡ (Kannada)";
            default -> languageCode;
        };
    }
}


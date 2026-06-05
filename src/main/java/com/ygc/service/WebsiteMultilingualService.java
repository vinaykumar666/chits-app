package com.ygc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Comprehensive Multilingual Service for entire YGC application
 * Supports 5 Indian languages for complete website UI and PDF generation
 * Integrates with existing report generation systems
 */
@Service
@RequiredArgsConstructor
public class WebsiteMultilingualService {

    private static final Map<String, Map<String, String>> TRANSLATIONS = new HashMap<>();

    static {
        initializeAllTranslations();
    }

    private static void initializeAllTranslations() {
        // ============ ENGLISH TRANSLATIONS ============
        Map<String, String> en = new HashMap<>();

        // Navigation & Menu
        en.put("nav.dashboard", "Dashboard");
        en.put("nav.chits", "Chits");
        en.put("nav.payments", "Payments");
        en.put("nav.auctions", "Auctions");
        en.put("nav.settlements", "Settlements");
        en.put("nav.reports", "Reports");
        en.put("nav.members", "Members");
        en.put("nav.documents", "Documents");
        en.put("nav.announcements", "Announcements");
        en.put("nav.profile", "Profile");
        en.put("nav.logout", "Logout");
        en.put("nav.settings", "Settings");

        // Buttons & Actions
        en.put("btn.approve", "Approve");
        en.put("btn.reject", "Reject");
        en.put("btn.submit", "Submit");
        en.put("btn.save", "Save");
        en.put("btn.cancel", "Cancel");
        en.put("btn.delete", "Delete");
        en.put("btn.edit", "Edit");
        en.put("btn.view", "View");
        en.put("btn.download", "Download");
        en.put("btn.upload", "Upload");
        en.put("btn.back", "Back");

        // Dashboard
        en.put("dashboard.title", "Dashboard");
        en.put("dashboard.welcome", "Welcome");
        en.put("dashboard.total_chits", "Total Chits");
        en.put("dashboard.active_chits", "Active Chits");
        en.put("dashboard.total_members", "Total Members");
        en.put("dashboard.pending_payments", "Pending Payments");
        en.put("dashboard.pending_settlements", "Pending Settlements");
        en.put("dashboard.pending_documents", "Pending Documents");
        en.put("dashboard.open_auctions", "Open Auctions");

        // Chits
        en.put("chit.name", "Chit Name");
        en.put("chit.description", "Description");
        en.put("chit.monthly_amount", "Monthly Amount (₹)");
        en.put("chit.total_members", "Total Members");
        en.put("chit.duration", "Duration (Months)");
        en.put("chit.status", "Status");
        en.put("chit.start_date", "Start Date");
        en.put("chit.end_date", "End Date");
        en.put("chit.active", "Active");
        en.put("chit.completed", "Completed");
        en.put("chit.cancelled", "Cancelled");

        // Payments
        en.put("payment.amount", "Amount");
        en.put("payment.due_date", "Due Date");
        en.put("payment.paid_date", "Paid Date");
        en.put("payment.status", "Status");
        en.put("payment.approved", "Approved");
        en.put("payment.rejected", "Rejected");
        en.put("payment.pending", "Pending");
        en.put("payment.overdue", "Overdue");
        en.put("payment.late_fine", "Late Fine (₹)");

        // Auctions
        en.put("auction.title", "Auction");
        en.put("auction.month", "Month");
        en.put("auction.date", "Auction Date");
        en.put("auction.status", "Status");
        en.put("auction.winner", "Winner");
        en.put("auction.winning_bid", "Winning Bid");
        en.put("auction.open", "Open");
        en.put("auction.closed", "Closed");

        // Members & Users
        en.put("member.name", "Member Name");
        en.put("member.email", "Email");
        en.put("member.phone", "Phone");
        en.put("member.address", "Address");
        en.put("member.join_date", "Join Date");
        en.put("member.status", "Status");
        en.put("member.active", "Active");
        en.put("member.inactive", "Inactive");

        // Documents
        en.put("document.upload", "Upload Document");
        en.put("document.my_documents", "My Documents");
        en.put("document.status", "Status");
        en.put("document.uploaded", "Uploaded");
        en.put("document.language", "Language");
        en.put("document.type", "Document Type");
        en.put("document.approved", "Approved");
        en.put("document.rejected", "Rejected");
        en.put("document.pending_approval", "Pending Approval");

        // Reports
        en.put("report.title", "Reports");
        en.put("report.chit_analysis", "Chit Analysis");
        en.put("report.payment_report", "Payment Report");
        en.put("report.commission", "Commission Report");
        en.put("report.settlement", "Settlement Report");
        en.put("report.export_pdf", "Export to PDF");
        en.put("report.generated", "Report Generated");

        // Messages
        en.put("msg.success", "Success");
        en.put("msg.error", "Error");
        en.put("msg.confirm", "Confirm");
        en.put("msg.warning", "Warning");
        en.put("msg.info", "Information");
        en.put("msg.saved_successfully", "Saved successfully");
        en.put("msg.deleted_successfully", "Deleted successfully");
        en.put("msg.operation_failed", "Operation failed");

        TRANSLATIONS.put("en", en);

        // ============ HINDI TRANSLATIONS ============
        Map<String, String> hi = new HashMap<>();

        // Navigation & Menu
        hi.put("nav.dashboard", "डैशबोर्ड");
        hi.put("nav.chits", "चिट्स");
        hi.put("nav.payments", "भुगतान");
        hi.put("nav.auctions", "नीलामियां");
        hi.put("nav.settlements", "निपटारे");
        hi.put("nav.reports", "रिपोर्टें");
        hi.put("nav.members", "सदस्य");
        hi.put("nav.documents", "दस्तावेज़");
        hi.put("nav.announcements", "घोषणाएं");
        hi.put("nav.profile", "प्रोफ़ाइल");
        hi.put("nav.logout", "लॉगआउट");
        hi.put("nav.settings", "सेटिंग्स");

        // Buttons & Actions
        hi.put("btn.approve", "मंजूर करें");
        hi.put("btn.reject", "अस्वीकार करें");
        hi.put("btn.submit", "जमा करें");
        hi.put("btn.save", "सहेजें");
        hi.put("btn.cancel", "रद्द करें");
        hi.put("btn.delete", "हटाएं");
        hi.put("btn.edit", "संपादित करें");
        hi.put("btn.view", "देखें");
        hi.put("btn.download", "डाउनलोड करें");
        hi.put("btn.upload", "अपलोड करें");
        hi.put("btn.back", "वापस");

        // Dashboard
        hi.put("dashboard.title", "डैशबोर्ड");
        hi.put("dashboard.welcome", "स्वागत है");
        hi.put("dashboard.total_chits", "कुल चिट्स");
        hi.put("dashboard.active_chits", "सक्रिय चिट्स");
        hi.put("dashboard.total_members", "कुल सदस्य");
        hi.put("dashboard.pending_payments", "लंबित भुगतान");
        hi.put("dashboard.pending_settlements", "लंबित निपटारे");
        hi.put("dashboard.pending_documents", "लंबित दस्तावेज़");
        hi.put("dashboard.open_auctions", "खुली नीलामियां");

        // Chits
        hi.put("chit.name", "चिट का नाम");
        hi.put("chit.description", "विवरण");
        hi.put("chit.monthly_amount", "मासिक राशि (₹)");
        hi.put("chit.total_members", "कुल सदस्य");
        hi.put("chit.duration", "अवधि (महीने)");
        hi.put("chit.status", "स्थिति");
        hi.put("chit.start_date", "प्रारंभ तारीख");
        hi.put("chit.end_date", "समाप्ति तारीख");
        hi.put("chit.active", "सक्रिय");
        hi.put("chit.completed", "पूर्ण");
        hi.put("chit.cancelled", "रद्द");

        // Payments
        hi.put("payment.amount", "राशि");
        hi.put("payment.due_date", "देय तारीख");
        hi.put("payment.paid_date", "भुगतान की तारीख");
        hi.put("payment.status", "स्थिति");
        hi.put("payment.approved", "मंजूर");
        hi.put("payment.rejected", "अस्वीकृत");
        hi.put("payment.pending", "लंबित");
        hi.put("payment.overdue", "अतिदेय");
        hi.put("payment.late_fine", "विलंब शुल्क (₹)");

        // Auctions
        hi.put("auction.title", "नीलामी");
        hi.put("auction.month", "महीना");
        hi.put("auction.date", "नीलामी की तारीख");
        hi.put("auction.status", "स्थिति");
        hi.put("auction.winner", "विजेता");
        hi.put("auction.winning_bid", "विजयी बोली");
        hi.put("auction.open", "खुली");
        hi.put("auction.closed", "बंद");

        // Members & Users
        hi.put("member.name", "सदस्य का नाम");
        hi.put("member.email", "ईमेल");
        hi.put("member.phone", "फोन");
        hi.put("member.address", "पता");
        hi.put("member.join_date", "शामिल होने की तारीख");
        hi.put("member.status", "स्थिति");
        hi.put("member.active", "सक्रिय");
        hi.put("member.inactive", "निष्क्रिय");

        // Documents
        hi.put("document.upload", "दस्तावेज़ अपलोड करें");
        hi.put("document.my_documents", "मेरे दस्तावेज़");
        hi.put("document.status", "स्थिति");
        hi.put("document.uploaded", "अपलोड किया गया");
        hi.put("document.language", "भाषा");
        hi.put("document.type", "दस्तावेज़ प्रकार");
        hi.put("document.approved", "मंजूर");
        hi.put("document.rejected", "अस्वीकृत");
        hi.put("document.pending_approval", "मंजूरी के लिए लंबित");

        // Reports
        hi.put("report.title", "रिपोर्टें");
        hi.put("report.chit_analysis", "चिट विश्लेषण");
        hi.put("report.payment_report", "भुगतान रिपोर्ट");
        hi.put("report.commission", "कमीशन रिपोर्ट");
        hi.put("report.settlement", "निपटारा रिपोर्ट");
        hi.put("report.export_pdf", "PDF में निर्यात करें");
        hi.put("report.generated", "रिपोर्ट उत्पन्न");

        // Messages
        hi.put("msg.success", "सफलता");
        hi.put("msg.error", "त्रुटि");
        hi.put("msg.confirm", "पुष्टि करें");
        hi.put("msg.warning", "चेतावनी");
        hi.put("msg.info", "जानकारी");
        hi.put("msg.saved_successfully", "सफलतापूर्वक सहेजा गया");
        hi.put("msg.deleted_successfully", "सफलतापूर्वक हटाया गया");
        hi.put("msg.operation_failed", "ऑपरेशन विफल");

        TRANSLATIONS.put("hi", hi);

        // ============ TAMIL TRANSLATIONS ============
        Map<String, String> ta = new HashMap<>();

        ta.put("nav.dashboard", "டாஷ்போர்ড்");
        ta.put("nav.chits", "சிட்கள்");
        ta.put("nav.payments", "பணம் செலுத்துதல்");
        ta.put("nav.auctions", "ஏலங்கள்");
        ta.put("nav.settlements", "தீர்வுகள்");
        ta.put("nav.reports", "அறிக்கைகள்");
        ta.put("nav.members", "உறுப்பினர்கள்");
        ta.put("nav.documents", "ஆவணங்கள்");
        ta.put("nav.announcements", "அறிவிப்புகள்");
        ta.put("nav.profile", "சுயவிவரம்");
        ta.put("nav.logout", "வெளியேறு");
        ta.put("nav.settings", "அமைப்புகள்");

        ta.put("btn.approve", "ஏற்கவும்");
        ta.put("btn.reject", "நிராகரிக்கவும்");
        ta.put("btn.submit", "சமர்ப்பிக்கவும்");
        ta.put("btn.save", "சேமிக்கவும்");
        ta.put("btn.cancel", "ரத்து செய்கவும்");
        ta.put("btn.delete", "நீக்கவும்");
        ta.put("btn.edit", "திருத்தவும்");
        ta.put("btn.view", "காணவும்");
        ta.put("btn.download", "பதிவிறக்கவும்");
        ta.put("btn.upload", "பதிவேற்றவும்");
        ta.put("btn.back", "திரும்பவும்");

        ta.put("dashboard.title", "டாஷ்போர்ட்");
        ta.put("dashboard.welcome", "வரவேற்கிறோம்");
        ta.put("dashboard.total_chits", "மொத்த சிட்கள்");
        ta.put("dashboard.active_chits", "செயல்படும் சிட்கள்");
        ta.put("dashboard.total_members", "மொத்த உறுப்பினர்கள்");
        ta.put("dashboard.pending_payments", "நிலுவையில் உள்ள பணம் செலுத்துதல்");
        ta.put("dashboard.pending_settlements", "நிலுவையில் உள்ள தீர்வுகள்");
        ta.put("dashboard.pending_documents", "நிலுவையில் உள்ள ஆவணங்கள்");
        ta.put("dashboard.open_auctions", "திறந்த ஏலங்கள்");

        ta.put("chit.name", "சிட்டின் பெயர்");
        ta.put("chit.description", "விளக்கம்");
        ta.put("chit.monthly_amount", "மாதिक தொகை (₹)");
        ta.put("chit.total_members", "மொத்த உறுப்பினர்கள்");
        ta.put("chit.duration", "காலம் (மாதங்கள்)");
        ta.put("chit.status", "நிலை");
        ta.put("chit.start_date", "தொடக்க தேதி");
        ta.put("chit.end_date", "இறுதி தேதி");
        ta.put("chit.active", "செயல்படும்");
        ta.put("chit.completed", "முடிந்த");
        ta.put("chit.cancelled", "ரத்து செய்யப்பட்ட");

        ta.put("payment.amount", "தொகை");
        ta.put("payment.due_date", "பணம் செலுத்த வேண்டிய தேதி");
        ta.put("payment.paid_date", "பணம் செலுத்தப்பட்ட தேதி");
        ta.put("payment.status", "நிலை");
        ta.put("payment.approved", "ஏற்கப்பட்ட");
        ta.put("payment.rejected", "நிராகரிக்கப்பட்ட");
        ta.put("payment.pending", "நிலுவையில் உள்ள");
        ta.put("payment.overdue", "தாமதம்");
        ta.put("payment.late_fine", "தாமதக் கட்டணம் (₹)");

        ta.put("auction.title", "ஏலம்");
        ta.put("auction.month", "மாதம்");
        ta.put("auction.date", "ஏல தேதி");
        ta.put("auction.status", "நிலை");
        ta.put("auction.winner", "வெற்றியாளர்");
        ta.put("auction.winning_bid", "வெற்றிகரமான பகுப்பாய்வு");
        ta.put("auction.open", "திறந்த");
        ta.put("auction.closed", "மூடிய");

        ta.put("member.name", "உறுப்பினரின் பெயர்");
        ta.put("member.email", "மின்னஞ்சல்");
        ta.put("member.phone", "ஃபோன்");
        ta.put("member.address", "முகவரி");
        ta.put("member.join_date", "சேர்ந்த தேதி");
        ta.put("member.status", "நிலை");
        ta.put("member.active", "செயல்படும்");
        ta.put("member.inactive", "செயல்படாத");

        ta.put("document.upload", "ஆவணத்தை பதிவேற்றவும்");
        ta.put("document.my_documents", "என் ஆவணங்கள்");
        ta.put("document.status", "நிலை");
        ta.put("document.uploaded", "பதிவேற்றப்பட்ட");
        ta.put("document.language", "மொழி");
        ta.put("document.type", "ஆவணத்தின் வகை");
        ta.put("document.approved", "ஏற்கப்பட்ட");
        ta.put("document.rejected", "நிராகரிக்கப்பட்ட");
        ta.put("document.pending_approval", "ஒப்புதலுக்கு நிலுவையில்");

        ta.put("report.title", "அறிக்கைகள்");
        ta.put("report.chit_analysis", "சிட் பகுப்பாய்வு");
        ta.put("report.payment_report", "பணம் செலுத்துதல் அறிக்கை");
        ta.put("report.commission", "கமிশன் அறிக்கை");
        ta.put("report.settlement", "தீர்வு அறிக்கை");
        ta.put("report.export_pdf", "PDF க்கு ஏற்றுமதி");
        ta.put("report.generated", "உত்பादित அறிக்கை");

        ta.put("msg.success", "வெற்றி");
        ta.put("msg.error", "பிழை");
        ta.put("msg.confirm", "உறுதிப்படுத்தவும்");
        ta.put("msg.warning", "எச்சரிக்கை");
        ta.put("msg.info", "தகவல்");
        ta.put("msg.saved_successfully", "வெற்றிகரமாக சேமிக்கப்பட்டது");
        ta.put("msg.deleted_successfully", "வெற்றிகரமாக நீக்கப்பட்டது");
        ta.put("msg.operation_failed", "செயல்பாடு தோல்வியடைந்தது");

        TRANSLATIONS.put("ta", ta);

        // ============ TELUGU TRANSLATIONS ============
        Map<String, String> te = new HashMap<>();

        te.put("nav.dashboard", "డ్యాష్‌బోర్డ్");
        te.put("nav.chits", "చిట్‌లు");
        te.put("nav.payments", "చెల్లింపులు");
        te.put("nav.auctions", "వేలం");
        te.put("nav.settlements", "పరిష్కారాలు");
        te.put("nav.reports", "నివేదనలు");
        te.put("nav.members", "సభ్యులు");
        te.put("nav.documents", "పత్రాలు");
        te.put("nav.announcements", "ప్రకటనలు");
        te.put("nav.profile", "ప్రొఫైల్");
        te.put("nav.logout", "లాగ్ అవుట్");
        te.put("nav.settings", "సెట్టింపులు");

        te.put("btn.approve", "ఆమోదించండి");
        te.put("btn.reject", "తిరస్కరించండి");
        te.put("btn.submit", "సమర్పించండి");
        te.put("btn.save", "సేవ్ చేయండి");
        te.put("btn.cancel", "రద్దు చేయండి");
        te.put("btn.delete", "తొలగించండి");
        te.put("btn.edit", "సవరించండి");
        te.put("btn.view", "చూడండి");
        te.put("btn.download", "డাউన్‌లోడ్ చేయండి");
        te.put("btn.upload", "అప్‌లోడ్ చేయండి");
        te.put("btn.back", "వెనుకకు");

        te.put("dashboard.title", "డ్యాష్‌బోర్డ్");
        te.put("dashboard.welcome", "స్వాగతం");
        te.put("dashboard.total_chits", "మొత్తం చిట్‌లు");
        te.put("dashboard.active_chits", "క్రియాశీల చిట్‌లు");
        te.put("dashboard.total_members", "మొత్తం సభ్యులు");
        te.put("dashboard.pending_payments", "పెండింగ్ చెల్లింపులు");
        te.put("dashboard.pending_settlements", "పెండింగ్ పరిష్కారాలు");
        te.put("dashboard.pending_documents", "పెండింగ్ పత్రాలు");
        te.put("dashboard.open_auctions", "తెరిచిన వేలం");

        te.put("chit.name", "చిట్ పేరు");
        te.put("chit.description", "వివరణ");
        te.put("chit.monthly_amount", "నెలవారీ మొత్తం (₹)");
        te.put("chit.total_members", "మొత్తం సభ్యులు");
        te.put("chit.duration", "వ్యవధి (నెలలు)");
        te.put("chit.status", "స్థితి");
        te.put("chit.start_date", "ప్రారంభ తేదీ");
        te.put("chit.end_date", "ముగింపు తేదీ");
        te.put("chit.active", "క్రియాశీల");
        te.put("chit.completed", "పూర్తిగా");
        te.put("chit.cancelled", "రద్దు చేయబడిన");

        te.put("payment.amount", "మొత్తం");
        te.put("payment.due_date", "చెల్లించాల్సిన తేదీ");
        te.put("payment.paid_date", "చెల్లించిన తేదీ");
        te.put("payment.status", "స్థితి");
        te.put("payment.approved", "ఆమోదించబడిన");
        te.put("payment.rejected", "తిరస్కరించబడిన");
        te.put("payment.pending", "పెండింగ్");
        te.put("payment.overdue", "విలంబం");
        te.put("payment.late_fine", "విలంబ విధిని (₹)");

        te.put("auction.title", "వేలం");
        te.put("auction.month", "నెల");
        te.put("auction.date", "వేలం తేదీ");
        te.put("auction.status", "స్థితి");
        te.put("auction.winner", "విజయవంతుడు");
        te.put("auction.winning_bid", "విజయవంత బిడ్");
        te.put("auction.open", "ఓపెన్");
        te.put("auction.closed", "ముందుగా");

        te.put("member.name", "సభ్యుని పేరు");
        te.put("member.email", "ఇమెయిల్");
        te.put("member.phone", "ఫోన్");
        te.put("member.address", "చిరునామా");
        te.put("member.join_date", "చేరిన తేదీ");
        te.put("member.status", "స్థితి");
        te.put("member.active", "క్రియాశీల");
        te.put("member.inactive", "నిష్క్రియ");

        te.put("document.upload", "పత్రం అప్‌లోడ్ చేయండి");
        te.put("document.my_documents", "నా పత్రాలు");
        te.put("document.status", "స్థితి");
        te.put("document.uploaded", "అప్‌లోడ్ చేయబడిన");
        te.put("document.language", "భాష");
        te.put("document.type", "పత్రం రకం");
        te.put("document.approved", "ఆమోదించబడిన");
        te.put("document.rejected", "తిరస్కరించబడిన");
        te.put("document.pending_approval", "ఆమోదనకు పెండింగ్");

        te.put("report.title", "నివేదనలు");
        te.put("report.chit_analysis", "చిట్ విశ్లేషణ");
        te.put("report.payment_report", "చెల్లింపు నివేదన");
        te.put("report.commission", "కమిషన్ నివేదన");
        te.put("report.settlement", "పరిష్కార నివేదన");
        te.put("report.export_pdf", "PDFకు ఎగుమతి చేయండి");
        te.put("report.generated", "ఉత్పత్తి నివేదన");

        te.put("msg.success", "విజయం");
        te.put("msg.error", "లోపం");
        te.put("msg.confirm", "నిర్ధారించండి");
        te.put("msg.warning", "హెచ్చరిక");
        te.put("msg.info", "సమాచారం");
        te.put("msg.saved_successfully", "విజయవంతంగా సేవ్ చేయబడింది");
        te.put("msg.deleted_successfully", "విజయవంతంగా తొలగించబడింది");
        te.put("msg.operation_failed", "ఆపరేషన్ విఫలమైంది");

        TRANSLATIONS.put("te", te);

        // ============ KANNADA TRANSLATIONS ============
        Map<String, String> kn = new HashMap<>();

        kn.put("nav.dashboard", "ಡ್ಯಾಶ್‌ಬೋರ್ಡ್");
        kn.put("nav.chits", "ಚಿಟ್‌ಗಳು");
        kn.put("nav.payments", "ಪಾವತಿಗಳು");
        kn.put("nav.auctions", "ಹರಾಜು");
        kn.put("nav.settlements", "ತೀರುವಿಕೆ");
        kn.put("nav.reports", "ವರದಿಗಳು");
        kn.put("nav.members", "ಸದಸ್ಯರು");
        kn.put("nav.documents", "ದಸ್ತಾವೇಜುಗಳು");
        kn.put("nav.announcements", "ಘೋಷಣೆಗಳು");
        kn.put("nav.profile", "ಪ್ರೊಫೈಲ್");
        kn.put("nav.logout", "ಲಾಗ್‌ಔಟ್");
        kn.put("nav.settings", "ಸೆಟ್ಟಿಂಗ್‌ಗಳು");

        kn.put("btn.approve", "ಅನುಮೋದಿಸಿ");
        kn.put("btn.reject", "ಅಸ್ವೀಕರಿಸಿ");
        kn.put("btn.submit", "ಸರ್ವೆ ಮಾಡಿ");
        kn.put("btn.save", "ಉಳಿಸಿ");
        kn.put("btn.cancel", "ರದ್ದುಮಾಡಿ");
        kn.put("btn.delete", "ಅಳಿಸಿ");
        kn.put("btn.edit", "ಸಂಪಾದಿಸಿ");
        kn.put("btn.view", "ವೀಕ್ಷಿಸಿ");
        kn.put("btn.download", "ಡೌನ್‌ಲೋಡ್ ಮಾಡಿ");
        kn.put("btn.upload", "ಅಪ್‌ಲೋಡ್ ಮಾಡಿ");
        kn.put("btn.back", "ಹಿಂದೆ");

        kn.put("dashboard.title", "ಡ್ಯಾಶ್‌ಬೋರ್ಡ್");
        kn.put("dashboard.welcome", "ಸ್ವಾಗತ");
        kn.put("dashboard.total_chits", "ಒಟ್ಟು ಚಿಟ್‌ಗಳು");
        kn.put("dashboard.active_chits", "ಸಕ್ರಿಯ ಚಿಟ್‌ಗಳು");
        kn.put("dashboard.total_members", "ಒಟ್ಟು ಸದಸ್ಯರು");
        kn.put("dashboard.pending_payments", "ಬಾಕಿ ಪಾವತಿಗಳು");
        kn.put("dashboard.pending_settlements", "ಬಾಕಿ ತೀರುವಿಕೆ");
        kn.put("dashboard.pending_documents", "ಬಾಕಿ ದಸ್ತಾವೇಜುಗಳು");
        kn.put("dashboard.open_auctions", "ತೆರೆದ ಹರಾಜುಗಳು");

        kn.put("chit.name", "ಚಿಟ್ ಹೆಸರು");
        kn.put("chit.description", "ವಿವರಣೆ");
        kn.put("chit.monthly_amount", "ಮಾಸಿಕ ಮೊತ್ತ (₹)");
        kn.put("chit.total_members", "ಒಟ್ಟು ಸದಸ್ಯರು");
        kn.put("chit.duration", "ಅವಧಿ (ತಿಂಗಳುಗಳು)");
        kn.put("chit.status", "ಸ್ಥಿತಿ");
        kn.put("chit.start_date", "ಪ್ರಾರಂಭ ದಿನಾಂಕ");
        kn.put("chit.end_date", "ಮುಕ್ತಾಯ ದಿನಾಂಕ");
        kn.put("chit.active", "ಸಕ್ರಿಯ");
        kn.put("chit.completed", "ಪೂರ್ಣ");
        kn.put("chit.cancelled", "ರದ್ದುಗೊಂಡ");

        kn.put("payment.amount", "ಮೊತ್ತ");
        kn.put("payment.due_date", "ಬಾಕಿ ದಿನಾಂಕ");
        kn.put("payment.paid_date", "ಪಾವತಿ ದಿನಾಂಕ");
        kn.put("payment.status", "ಸ್ಥಿತಿ");
        kn.put("payment.approved", "ಅನುಮೋದಿತ");
        kn.put("payment.rejected", "ನಿರಾಕರಣೆ");
        kn.put("payment.pending", "ಬಾಕಿ");
        kn.put("payment.overdue", "ವಿಳಂಬ");
        kn.put("payment.late_fine", "ವಿಳಂಬ ದಿವಾಳಿ (₹)");

        kn.put("auction.title", "ಹರಾಜು");
        kn.put("auction.month", "ತಿಂಗಳು");
        kn.put("auction.date", "ಹರಾಜು ದಿನಾಂಕ");
        kn.put("auction.status", "ಸ್ಥಿತಿ");
        kn.put("auction.winner", "ವಿಜೇತ");
        kn.put("auction.winning_bid", "ವಿಜಯಿ ನೀಲಾಮ");
        kn.put("auction.open", "ತೆರೆದ");
        kn.put("auction.closed", "ಮುಚ್ಚಿದ");

        kn.put("member.name", "ಸದಸ್ಯನ ಹೆಸರು");
        kn.put("member.email", "ಇಮೇಲ್");
        kn.put("member.phone", "ಫೋನ್");
        kn.put("member.address", "ವಿಳಾಸ");
        kn.put("member.join_date", "ಸೇರುವ ದಿನಾಂಕ");
        kn.put("member.status", "ಸ್ಥಿತಿ");
        kn.put("member.active", "ಸಕ್ರಿಯ");
        kn.put("member.inactive", "ನಿಷ್ಕ್ರಿಯ");

        kn.put("document.upload", "ದಸ್ತಾವೇಜು ಅಪ್‌ಲೋಡ್ ಮಾಡಿ");
        kn.put("document.my_documents", "ನನ್ನ ದಸ್ತಾವೇಜುಗಳು");
        kn.put("document.status", "ಸ್ಥಿತಿ");
        kn.put("document.uploaded", "ಅಪ್‌ಲೋಡ್ ಆಗಿದೆ");
        kn.put("document.language", "ಭಾಷೆ");
        kn.put("document.type", "ದಸ್ತಾವೇಜು ಪ್ರಕಾರ");
        kn.put("document.approved", "ಅನುಮೋದಿತ");
        kn.put("document.rejected", "ನಿರಾಕರಣೆ");
        kn.put("document.pending_approval", "ಅನುಮೋದನೆಗೆ ಬಾಕಿ");

        kn.put("report.title", "ವರದಿಗಳು");
        kn.put("report.chit_analysis", "ಚಿಟ್ ವಿಶ್ಲೇಷಣೆ");
        kn.put("report.payment_report", "ಪಾವತಿ ವರದಿ");
        kn.put("report.commission", "ಆಯೋಗ ವರದಿ");
        kn.put("report.settlement", "ತೀರುವಿಕೆ ವರದಿ");
        kn.put("report.export_pdf", "PDFಗೆ ರಫ್ತು ಮಾಡಿ");
        kn.put("report.generated", "ರಚಿತ ವರದಿ");

        kn.put("msg.success", "ಯಶಸ್ಸು");
        kn.put("msg.error", "ದೋಷ");
        kn.put("msg.confirm", "ಖಚಿತಪಡಿಸಿ");
        kn.put("msg.warning", "ಎಚ್ಚರಿಕೆ");
        kn.put("msg.info", "ಮಾಹಿತಿ");
        kn.put("msg.saved_successfully", "ಯಶಸ್ವಿಯಾಗಿ ಉಳಿಸಿದೆ");
        kn.put("msg.deleted_successfully", "ಯಶಸ್ವಿಯಾಗಿ ಅಳಿಸಿದೆ");
        kn.put("msg.operation_failed", "ಕಾರ್ಯಾಚರಣೆ ವಿಫಲ");

        TRANSLATIONS.put("kn", kn);
    }

    /**
     * Get translated text for given language and key
     */
    public String getText(String language, String key) {
        String lang = language != null ? language.toLowerCase() : "en";
        Map<String, String> messages = TRANSLATIONS.getOrDefault(lang, TRANSLATIONS.get("en"));
        return messages.getOrDefault(key, key);
    }

    /**
     * Get translated text with parameter replacement
     */
    public String getTextWithParams(String language, String key, Map<String, String> params) {
        String text = getText(language, key);
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                text = text.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return text;
    }

    /**
     * Get available languages
     */
    public List<String> getAvailableLanguages() {
        return new ArrayList<>(TRANSLATIONS.keySet());
    }

    /**
     * Get language display name
     */
    public String getLanguageDisplayName(String languageCode) {
        return switch (languageCode != null ? languageCode.toLowerCase() : "en") {
            case "en" -> "🇬🇧 English";
            case "hi" -> "🇮🇳 हिन्दी (Hindi)";
            case "ta" -> "தமிழ் (Tamil)";
            case "te" -> "తెలుగు (Telugu)";
            case "kn" -> "ಕನ್ನಡ (Kannada)";
            default -> languageCode;
        };
    }

    /**
     * Check if language is supported
     */
    public boolean isSupportedLanguage(String language) {
        return TRANSLATIONS.containsKey(language != null ? language.toLowerCase() : "en");
    }
}


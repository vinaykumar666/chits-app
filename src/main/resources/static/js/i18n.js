/**
 * YGC Auto-Translation Engine
 * Automatically translates ALL page content based on the selected locale cookie.
 * Works alongside Thymeleaf #{} server-side i18n for complete coverage.
 */
(function() {
  var TRANSLATIONS = {
    te: {
      // Dashboard
      "Admin Dashboard":"అడ్మిన్ డాష్‌బోర్డ్","YGC Internal Chit Management":"YGC అంతర్గత చిట్ నిర్వహణ",
      "My Dashboard":"నా డాష్‌బోర్డ్","Welcome back":"తిరిగి స్వాగతం",
      "Chits":"చిట్లు","Members":"సభ్యులు","Pending Pay":"పెండింగ్ చెల్లింపు","Auctions":"వేలాలు","Settlements":"సెటిల్‌మెంట్లు",
      "Support":"సహాయం","Quick Actions":"త్వరిత చర్యలు","Recent Activity":"ఇటీవల కార్యకలాపం",
      "Create Chit Group":"చిట్ గ్రూప్ సృష్టించండి","Verify Payments":"చెల్లింపులు ధృవీకరించండి",
      "Manage Auctions":"వేలాలు నిర్వహించండి","Commission Report":"కమీషన్ రిపోర్ట్",
      "Requires Your Action":"మీ చర్య అవసరం","Join Requests":"చేరిక అభ్యర్థనలు",
      "Payment Approvals":"చెల్లింపు ఆమోదాలు","Exit Requests":"నిష్క్రమణ అభ్యర్థనలు","Pending Settlements":"పెండింగ్ సెటిల్‌మెంట్లు",
      "View All":"అన్నీ చూడండి","Developer Tools":"డెవలపర్ సాధనాలు","One-time use":"ఒక్కసారి ఉపయోగం",
      "Flush All Test Data":"అన్ని టెస్ట్ డేటా తొలగించు",
      // Members
      "Member Management":"సభ్యుల నిర్వహణ","View and manage all registered users":"నమోదైన వినియోగదారులందరినీ చూడండి",
      "Add New Member":"కొత్త సభ్యుడిని జోడించండి","New Member":"కొత్త సభ్యుడు","Full Name":"పూర్తి పేరు",
      "Email":"ఇమెయిల్","Phone":"ఫోన్","Role":"పాత్ర","Status":"స్థితి","Joined":"చేరిన తేదీ",
      "Active":"యాక్టివ్","Inactive":"నిష్క్రియ","First Login":"మొదటి లాగిన్",
      // Payments
      "Payment Verification":"చెల్లింపు ధృవీకరణ","QR payment proof review & approval":"QR చెల్లింపు రుజువు సమీక్ష & ఆమోదం",
      "Pending Payments":"పెండింగ్ చెల్లింపులు","All Payments":"అన్ని చెల్లింపులు",
      "Amount":"మొత్తం","Late Fine":"ఆలస్యం ఫైన్","Total":"మొత్తం","Screenshot":"స్క్రీన్‌షాట్",
      "Month":"నెల","Paid Date":"చెల్లించిన తేదీ","Due Date":"చెల్లింపు తేదీ",
      "Approved":"ఆమోదించబడింది","Rejected":"తిరస్కరించబడింది","Pending":"పెండింగ్","Overdue":"గడువు దాటినది",
      // Chits
      "Chit Groups":"చిట్ గ్రూపులు","Create and manage chit fund groups":"చిట్ ఫండ్ గ్రూపులను సృష్టించండి మరియు నిర్వహించండి",
      "New Chit":"కొత్త చిట్","Monthly":"నెలవారీ","Total Value":"మొత్తం విలువ","Duration":"వ్యవధి",
      "Start Date":"ప్రారంభ తేదీ","Commission":"కమీషన్","chit groups":"చిట్ గ్రూపులు",
      // Chit Detail
      "Member Analytics & Payment Tracking":"సభ్యుల విశ్లేషణ & చెల్లింపు ట్రాకింగ్",
      "Chit Configuration":"చిట్ కాన్ఫిగరేషన్","Members Joined":"చేరిన సభ్యులు",
      "Total Collected":"మొత్తం సేకరించబడింది","Pending Amount":"పెండింగ్ మొత్తం",
      "Paid / Unpaid":"చెల్లించిన / చెల్లించని","Overdue Payments":"గడువు దాటిన చెల్లింపులు",
      "Collection Progress":"సేకరణ పురోగతి","Add Member":"సభ్యుడిని జోడించండి",
      "Agreement":"ఒప్పందం","Won Bid":"గెలిచిన బిడ్","Paid":"చెల్లించినది","Payment":"చెల్లింపు",
      "Actions":"చర్యలు","On time":"సమయానికి","Accepted":"ఆమోదించబడింది","Not Accepted":"ఆమోదించబడలేదు",
      // Settlements
      "Settlement Management":"సెటిల్‌మెంట్ నిర్వహణ","Chit":"చిట్","Member":"సభ్యుడు",
      "Total Paid":"మొత్తం చెల్లించినది","Deduction":"తగ్గింపు","Final Amount":"చివరి మొత్తం",
      "Profit Share":"లాభ వాటా","Remarks":"వ్యాఖ్యలు","Process":"ప్రాసెస్",
      // Audit
      "Audit Log":"ఆడిట్ లాగ్","Complete immutable activity trail":"పూర్తి మార్పులేని కార్యకలాప ట్రైల్",
      "Time":"సమయం","User":"వినియోగదారుడు","Action":"చర్య","Entity":"ఎంటిటీ","IP Address":"IP చిరునామా",
      "Description":"వివరణ","Activity Log":"కార్యకలాప లాగ్",
      // Early Exits
      "Early Exit Management":"ముందస్తు నిష్క్రమణ నిర్వహణ","Review and process member exit requests":"సభ్యుల నిష్క్రమణ అభ్యర్థనలను సమీక్షించండి",
      "Reason":"కారణం","Refund":"రీఫండ్","Penalty":"జరిమానా","Requested":"అభ్యర్థించబడింది",
      // Risk
      "Risk Dashboard":"రిస్క్ డాష్‌బోర్డ్","High Risk Members":"అధిక రిస్క్ సభ్యులు",
      "Risk Score":"రిస్క్ స్కోర్","Login Activity":"లాగిన్ కార్యకలాపం",
      // Login & Security
      "Login & Security Tracking":"లాగిన్ & భద్రతా ట్రాకింగ్",
      "Monitor login attempts, Aadhaar verification, and account security":"లాగిన్ ప్రయత్నాలు, ఆధార్ ధృవీకరణ మరియు ఖాతా భద్రతను పర్యవేక్షించండి",
      "Locked Accounts":"లాక్ చేయబడిన ఖాతాలు","Failed Login Users":"విఫల లాగిన్ వినియోగదారులు",
      "Aadhaar Verified":"ఆధార్ ధృవీకరించబడింది","Aadhaar Pending":"ఆధార్ పెండింగ్",
      "Failed Attempts":"విఫల ప్రయత్నాలు","Last Login":"చివరి లాగిన్","Unlock":"అన్‌లాక్",
      "Recent Login Activity":"ఇటీవల లాగిన్ కార్యకలాపం","Failure Reason":"వైఫల్య కారణం",
      "Device":"పరికరం","Last Login IP":"చివరి లాగిన్ IP","Locked":"లాక్ చేయబడింది",
      "Login Success Rate":"లాగిన్ విజయ రేటు","Security Overview":"భద్రతా అవలోకనం",
      "Suspicious Activity":"అనుమానాస్పద కార్యకలాపం","Reset Counter":"కౌంటర్ రీసెట్",
      "Total Logins (30d)":"మొత్తం లాగిన్లు (30 రోజులు)","Failed Logins (30d)":"విఫల లాగిన్లు (30 రోజులు)",
      "Unique IPs":"ప్రత్యేక IPలు","Aadhaar Compliance":"ఆధార్ అనుసరణ",
      "Verify":"ధృవీకరించు","Unverify":"ధృవీకరణ రద్దు",
      // Member Dashboard
      "My Memberships":"నా సభ్యత్వాలు","My Chit Memberships":"నా చిట్ సభ్యత్వాలు","Open to Join":"చేరడానికి అందుబాటులో",
      "Browse Available Chits":"అందుబాటులో ఉన్న చిట్లు బ్రౌజ్ చేయండి","View Details":"వివరాలు చూడండి","Join New":"కొత్తగా చేరండి",
      "Available Chits to Join":"చేరడానికి అందుబాటులో ఉన్న చిట్లు","Request to Join":"చేరిక అభ్యర్థన",
      "Settlement Ready":"సెటిల్‌మెంట్ సిద్ధంగా ఉంది","Payment Received":"చెల్లింపు అందుకోబడింది",
      "Confirm Payment Received":"చెల్లింపు అందుకున్నట్లు నిర్ధారించండి",
      // Common
      "Save":"సేవ్","Cancel":"రద్దు","Search":"వెతుకు","Filter":"ఫిల్టర్","Export":"ఎక్స్‌పోర్ట్",
      "Download":"డౌన్‌లోడ్","Delete":"తొలగించు","Edit":"సవరించు","Approve":"ఆమోదించు",
      "Reject":"తిరస్కరించు","Submit":"సమర్పించు","Close":"మూసివేయి","Yes":"అవును","No":"కాదు",
      "Loading...":"లోడ్ అవుతోంది...","No records found":"రికార్డులు కనుగొనబడలేదు",
      "Name":"పేరు","Address":"చిరునామా","Date":"తేదీ","View":"చూడండి","Back":"వెనుకకు",
      "Confirm":"నిర్ధారించు","Reset":"రీసెట్","Export PDF":"PDF ఎక్స్‌పోర్ట్","My Statement":"నా స్టేట్‌మెంట్",
      "Terms":"నిబంధనలు","Accepted":"ఆమోదించబడింది","Won":"గెలిచింది",
      "Early Exit Request":"ముందస్తు నిష్క్రమణ అభ్యర్థన","Early Exit Settlement":"ముందస్తు నిష్క్రమణ సెటిల్‌మెంట్",
      "Request Early Exit":"ముందస్తు నిష్క్రమణ అభ్యర్థించు","Exit Reason":"నిష్క్రమణ కారణం",
      "Settlement Estimate":"సెటిల్‌మెంట్ అంచనా","Estimated Refund":"అంచనా రీఫండ్",
      "Chit History":"చిట్ చరిత్ర","Announcements":"ప్రకటనలు","Help & Rules":"సహాయం & నియమాలు",
      "Payment History":"చెల్లింపు చరిత్ర","Payment Proof":"చెల్లింపు రుజువు",
      "Admin Remarks":"అడ్మిన్ వ్యాఖ్యలు","Approve Exit":"నిష్క్రమణ ఆమోదించు",
      "Rejection Reason":"తిరస్కరణ కారణం","Awaiting":"నిరీక్షణలో",
      "No activity yet":"ఇంకా కార్యకలాపం లేదు","members":"సభ్యులు","records":"రికార్డులు",
      "months":"నెలలు","mo":"నె","overdue":"గడువు దాటినది",
      "ACTIVE":"యాక్టివ్","PENDING":"పెండింగ్","OPEN":"తెరిచి ఉంది","CANCELLED":"రద్దు","COMPLETED":"పూర్తయింది",
      "APPROVED":"ఆమోదించబడింది","REJECTED":"తిరస్కరించబడింది","EXITED":"నిష్క్రమించారు","SETTLED":"సెటిల్ అయింది",
      "OK":"సరే","FAIL":"విఫలం","Member Profile":"సభ్యుడి ప్రొఫైల్","Financial health, trust rating, and activity history":"ఆర్థిక ఆరోగ్యం, నమ్మకం రేటింగ్ మరియు కార్యకలాప చరిత్ర",
      "Trust Rating":"నమ్మకం రేటింగ్","Payment Score":"చెల్లింపు స్కోర్","On Time":"సమయానికి","Completed":"పూర్తయింది",
      "Document Center":"పత్ర కేంద్రం","Fraud Detection":"ఫ్రాడ్ గుర్తింపు","Fraud & Risk Detection":"ఫ్రాడ్ & రిస్క్ గుర్తింపు",
      "Duplicate Aadhaar":"నకిలీ ఆధార్","Duplicate Phone":"నకిలీ ఫోన్","High Risk":"అధిక రిస్క్","Watchlist":"వాచ్‌లిస్ట్",
      "PLATINUM":"ప్లాటినం","GOLD":"గోల్డ్","SILVER":"సిల్వర్","WATCHLIST":"వాచ్‌లిస్ట్",
      "Total Chit Value":"మొత్తం చిట్ విలువ","Collections":"సేకరణలు","Pending":"పెండింగ్",
      "Remaining Installments":"మిగిలిన వాయిదాలు","Collection Forecast":"సేకరణ అంచనా",
      "Personal Information":"వ్యక్తిగత సమాచారం","Chit Memberships":"చిట్ సభ్యత్వాలు","Recent Logins":"ఇటీవల లాగిన్లు"
    },
    hi: {
      "Admin Dashboard":"एडमिन डैशबोर्ड","YGC Internal Chit Management":"YGC आंतरिक चिट प्रबंधन",
      "My Dashboard":"मेरा डैशबोर्ड","Welcome back":"वापसी पर स्वागत",
      "Chits":"चिट","Members":"सदस्य","Pending Pay":"लंबित भुगतान","Auctions":"नीलामी","Settlements":"निपटान",
      "Support":"सहायता","Quick Actions":"त्वरित कार्य","Recent Activity":"हाल की गतिविधि",
      "Create Chit Group":"चिट समूह बनाएं","Verify Payments":"भुगतान सत्यापित करें",
      "Manage Auctions":"नीलामी प्रबंधित करें","Commission Report":"कमीशन रिपोर्ट",
      "Requires Your Action":"आपकी कार्रवाई आवश्यक","Join Requests":"शामिल होने के अनुरोध",
      "Payment Approvals":"भुगतान अनुमोदन","Exit Requests":"निकास अनुरोध","Pending Settlements":"लंबित निपटान",
      "View All":"सभी देखें",
      "Member Management":"सदस्य प्रबंधन","View and manage all registered users":"सभी पंजीकृत उपयोगकर्ता देखें",
      "Add New Member":"नया सदस्य जोड़ें","Full Name":"पूरा नाम","Email":"ईमेल","Phone":"फ़ोन",
      "Role":"भूमिका","Status":"स्थिति","Joined":"शामिल","Active":"सक्रिय",
      "Payment Verification":"भुगतान सत्यापन","Pending Payments":"लंबित भुगतान","All Payments":"सभी भुगतान",
      "Amount":"राशि","Late Fine":"विलंब जुर्माना","Total":"कुल","Month":"माह","Paid Date":"भुगतान तिथि",
      "Approved":"स्वीकृत","Rejected":"अस्वीकृत","Pending":"लंबित","Overdue":"अतिदेय",
      "Chit Groups":"चिट समूह","Monthly":"मासिक","Total Value":"कुल मूल्य","Duration":"अवधि",
      "Start Date":"आरंभ तिथि","Commission":"कमीशन",
      "Audit Log":"ऑडिट लॉग","Time":"समय","User":"उपयोगकर्ता","Action":"कार्रवाई",
      "Entity":"इकाई","IP Address":"IP पता","Description":"विवरण",
      "Early Exit Management":"शीघ्र निकास प्रबंधन","Reason":"कारण","Refund":"वापसी","Penalty":"जुर्माना",
      "Login & Security Tracking":"लॉगिन और सुरक्षा ट्रैकिंग",
      "Locked Accounts":"लॉक खाते","Failed Login Users":"विफल लॉगिन उपयोगकर्ता",
      "Aadhaar Verified":"आधार सत्यापित","Aadhaar Pending":"आधार लंबित",
      "Save":"सेव","Cancel":"रद्द","Search":"खोजें","Filter":"फ़िल्टर","Approve":"स्वीकृत","Reject":"अस्वीकृत",
      "Submit":"जमा करें","Delete":"हटाएं","Edit":"संपादित करें","Name":"नाम","Actions":"कार्य",
      "ACTIVE":"सक्रिय","PENDING":"लंबित","APPROVED":"स्वीकृत","REJECTED":"अस्वीकृत","OK":"ठीक","FAIL":"विफल"
    },
    kn: {
      "Admin Dashboard":"ನಿರ್ವಾಹಕ ಡ್ಯಾಶ್‌ಬೋರ್ಡ್","My Dashboard":"ನನ್ನ ಡ್ಯಾಶ್‌ಬೋರ್ಡ್",
      "Members":"ಸದಸ್ಯರು","Payments":"ಪಾವತಿಗಳು","Chits":"ಚಿಟ್‌ಗಳು","Settlements":"ಸೆಟಲ್‌ಮೆಂಟ್‌ಗಳು",
      "Auctions":"ಹರಾಜು","Amount":"ಮೊತ್ತ","Status":"ಸ್ಥಿತಿ","Actions":"ಕ್ರಿಯೆಗಳು",
      "Approved":"ಅನುಮೋದಿಸಲಾಗಿದೆ","Rejected":"ತಿರಸ್ಕರಿಸಲಾಗಿದೆ","Pending":"ಬಾಕಿ",
      "Save":"ಉಳಿಸಿ","Cancel":"ರದ್ದು","Search":"ಹುಡುಕಿ","Name":"ಹೆಸರು"
    },
    ta: {
      "Admin Dashboard":"நிர்வாகி டாஷ்போர்ட்","My Dashboard":"என் டாஷ்போர்ட்",
      "Members":"உறுப்பினர்கள்","Payments":"பணம்","Chits":"சிட்டுகள்","Settlements":"தீர்வுகள்",
      "Auctions":"ஏலம்","Amount":"தொகை","Status":"நிலை","Actions":"செயல்கள்",
      "Approved":"அங்கீகரிக்கப்பட்டது","Rejected":"நிராகரிக்கப்பட்டது","Pending":"நிலுவை",
      "Save":"சேமி","Cancel":"ரத்து","Search":"தேடு","Name":"பெயர்"
    }
  };

  function getLang() {
    var m = document.cookie.match(/ygc-lang=([^;]+)/);
    if (m) return m[1];
    var u = new URLSearchParams(window.location.search);
    return u.get('lang') || 'en';
  }

  function translatePage() {
    var lang = getLang();
    if (lang === 'en' || !TRANSLATIONS[lang]) return;
    var dict = TRANSLATIONS[lang];
    var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
    var node;
    while (node = walker.nextNode()) {
      var txt = node.nodeValue.trim();
      if (txt && dict[txt]) {
        node.nodeValue = node.nodeValue.replace(txt, dict[txt]);
      }
    }
    // Translate placeholders and titles
    document.querySelectorAll('[placeholder]').forEach(function(el) {
      var p = el.getAttribute('placeholder').trim();
      if (dict[p]) el.setAttribute('placeholder', dict[p]);
    });
    document.querySelectorAll('[title]').forEach(function(el) {
      var t = el.getAttribute('title').trim();
      if (dict[t]) el.setAttribute('title', dict[t]);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', translatePage);
  } else {
    translatePage();
  }
})();

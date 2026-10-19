package com.recruiting.platform.privacy.consent.app;

import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.privacy.domain.PrivacyNotice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Writes the KVKK information notice + consent request e-mail in Turkish or English (S-01, NFR-01). */
@Component
class ConsentMailComposer {

    /** Subject and plain-text body of one e-mail. */
    record Mail(String subject, String body) {
    }

    private final String baseUrl;
    private final String controller;

    ConsentMailComposer(@Value("${app.base-url:http://localhost:8080}") String baseUrl,
            @Value("${app.privacy.controller-name:Executive Search Co.}") String controller) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.controller = controller;
    }

    /** The one-time link the person opens (no login). */
    String link(String token) {
        return baseUrl + "/consent/" + token;
    }

    Mail compose(Language language, String firstName, String token, PrivacyNotice notice, int validDays) {
        String link = link(token);
        if (language == Language.EN) {
            return new Mail("Information notice and consent request - " + controller,
                    "Dear " + firstName + ",\n\n"
                    + controller + " keeps your professional details so we can contact you about suitable roles. "
                    + "Below is our information notice on how we process your personal data (KVKK Art. 10).\n\n"
                    + notice.getNoticeText() + "\n\n"
                    + "To let us share your profile with our clients, we also need your separate explicit consent. "
                    + "You can give, refuse or withdraw it on this page, which stays valid for " + validDays
                    + " days:\n" + link + "\n\n"
                    + "If you do nothing, we keep using your data only as described in the notice.\n\n" + controller);
        }
        return new Mail("Aydınlatma metni ve açık rıza talebi - " + controller,
                "Sayın " + firstName + ",\n\n"
                + controller + ", uygun pozisyonlar hakkında sizinle iletişime geçebilmek için mesleki bilgilerinizi "
                + "saklamaktadır. Kişisel verilerinizi nasıl işlediğimize ilişkin aydınlatma metnimiz aşağıdadır "
                + "(KVKK m. 10).\n\n"
                + notice.getNoticeText() + "\n\n"
                + "Profilinizi müşterilerimizle paylaşabilmemiz için ayrıca açık rızanıza ihtiyaç duyuyoruz. "
                + "Rızanızı aşağıdaki sayfadan verebilir, reddedebilir veya sonradan geri alabilirsiniz; "
                + "bağlantı " + validDays + " gün geçerlidir:\n" + link + "\n\n"
                + "Hiçbir işlem yapmazsanız verileriniz yalnızca aydınlatma metninde belirtildiği şekilde "
                + "kullanılır.\n\n" + controller);
    }
}

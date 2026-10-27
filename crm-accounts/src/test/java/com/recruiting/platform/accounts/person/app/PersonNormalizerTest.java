package com.recruiting.platform.accounts.person.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PersonNormalizerTest {

    @Test
    void phonesGetCountryCodeWithTurkeyAsDefault() {
        assertThat(PersonNormalizer.phone("0555 123 45 67")).isEqualTo("+905551234567");
        assertThat(PersonNormalizer.phone("555 123 45 67")).isEqualTo("+905551234567");
        assertThat(PersonNormalizer.phone("+90 (555) 123-45-67")).isEqualTo("+905551234567");
        assertThat(PersonNormalizer.phone("0090 555 123 45 67")).isEqualTo("+905551234567");
        assertThat(PersonNormalizer.phone("905551234567")).isEqualTo("+905551234567");
        assertThat(PersonNormalizer.phone("+44 20 7946 0958")).isEqualTo("+442079460958");
        assertThat(PersonNormalizer.phone("12345")).isNull();
        assertThat(PersonNormalizer.phone("+1234567890123456789")).isNull();
        assertThat(PersonNormalizer.phone(null)).isNull();
    }

    @Test
    void linkedinUrlsAreReducedToTheProfileForm() {
        String expected = "https://www.linkedin.com/in/fictional-person";
        assertThat(PersonNormalizer.linkedin("https://tr.linkedin.com/in/Fictional-Person/?trk=x")).isEqualTo(expected);
        assertThat(PersonNormalizer.linkedin("linkedin.com/in/fictional-person/")).isEqualTo(expected);
        assertThat(PersonNormalizer.linkedin("http://www.linkedin.com/in/FICTIONAL-PERSON#top")).isEqualTo(expected);
        assertThat(PersonNormalizer.linkedin("https://www.linkedin.com/company/acme")).isNull();
        assertThat(PersonNormalizer.linkedin("https://example.test/in/someone")).isNull();
        assertThat(PersonNormalizer.linkedin("not a url")).isNull();
        assertThat(PersonNormalizer.linkedin(" ")).isNull();
    }
}

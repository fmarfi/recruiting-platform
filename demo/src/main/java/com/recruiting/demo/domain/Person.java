package com.recruiting.demo.domain;

import com.recruiting.demo.domain.Types.ConsentStatus;
import com.recruiting.demo.domain.Types.PersonType;
import java.time.LocalDate;
import java.util.List;

/**
 * One person the firm knows: executive, client contact, referrer or candidate
 * (requirement A-12). Contacts of an account are people linked through {@link Contact}.
 *
 * @param id          unique id, e.g. {@code per-1}
 * @param name        full name
 * @param title       current job title
 * @param company     current employer
 * @param type        how the firm mainly relates to the person
 * @param email       e-mail address (fictional in the demo)
 * @param city        city of residence
 * @param howWeKnow   how the firm got to know the person
 * @param owner       consultant who owns the relationship
 * @param lastContact date of the last logged contact
 * @param consent     KVKK consent status (S-01)
 * @param career      company history, most recent first
 */
public record Person(String id, String name, String title, String company, PersonType type,
                     String email, String city, String howWeKnow, String owner,
                     LocalDate lastContact, ConsentStatus consent, List<String> career) {

    /**
     * Returns a copy with a new last contact date.
     *
     * @param date the date of the latest contact
     * @return the updated person
     */
    public Person withLastContact(LocalDate date) {
        return new Person(id, name, title, company, type, email, city, howWeKnow, owner,
                date, consent, career);
    }
}

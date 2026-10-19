package com.recruiting.platform.accounts.referencedata.web.dto;

import com.recruiting.platform.contracts.ActivityType;
import com.recruiting.platform.contracts.ConsentChannel;
import com.recruiting.platform.contracts.DataSubjectRequestType;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.LinkType;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.Role;
import java.util.List;

/** Values of the plain enums. */
public record Vocabularies(List<PersonType> personType, List<ActivityType> activityType, List<LinkType> linkType,
        List<Role> role, List<ConsentChannel> consentChannel, List<DataSubjectRequestType> dataSubjectRequestType,
        List<Language> language) {
}

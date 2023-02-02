package com.diemlife.services;

import com.diemlife.models.LinkedAccount;
import com.diemlife.models.User;
import providers.MyUsernamePasswordAuthProvider;
import security.PasswordHasher;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBefore;

@Singleton
public class FormSecurityService {

    private final PasswordHasher passwordHasher;
    private final MyUsernamePasswordAuthProvider userPaswAuthProvider;

    @Inject
    public FormSecurityService(final PasswordHasher passwordHasher,
                               final MyUsernamePasswordAuthProvider userPaswAuthProvider) {
        this.passwordHasher = passwordHasher;
        this.userPaswAuthProvider = userPaswAuthProvider;
    }

    public boolean formPasswordMatches(final User user, final String password) {
        if (isBlank(password)) {
            return false;
        }
        final LinkedAccount credentials = user.getLinkedAccounts().stream()
                .filter(linkedAccount -> userPaswAuthProvider.getKey().equals(linkedAccount.getProviderKey()))
                .findFirst()
                .orElse(null);
        return credentials != null && passwordsMatch(password, credentials.getProviderUserId());
    }

    private boolean passwordsMatch(final String clearTextCandidate, final String checkHash) {
        if (isBlank(clearTextCandidate) || isBlank(checkHash)) {
            return false;
        }
        final String storedSha256hex = substringBefore(checkHash, "#");
        final String storedSalt = substringAfterLast(checkHash, "#");

        return passwordHasher.checkPassword(storedSha256hex, storedSalt, clearTextCandidate);
    }

}

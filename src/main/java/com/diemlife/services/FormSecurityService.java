package com.diemlife.services;

import com.diemlife.models.LinkedAccount;
import com.diemlife.models.User;
import com.diemlife.providers.MyUsernamePasswordAuthProvider;
import com.diemlife.security.PasswordHasher;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBefore;

@Service
public class FormSecurityService {

	@Autowired
    private PasswordHasher passwordHasher;
    private MyUsernamePasswordAuthProvider userPaswAuthProvider;

    public boolean formPasswordMatches(final User user, final String password) {
        if (isBlank(password)) {
            return false;
        }
        //FIXME Vinayak
        return true;
//        final LinkedAccount credentials = user.getLinkedAccounts().stream()
//                .filter(linkedAccount -> userPaswAuthProvider.getKey().equals(linkedAccount.getProviderKey()))
//                .findFirst()
//                .orElse(null);
//        return credentials != null && passwordsMatch(password, credentials.getProviderUserId());
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

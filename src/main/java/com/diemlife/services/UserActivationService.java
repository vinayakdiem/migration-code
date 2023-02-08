package com.diemlife.services;

import com.typesafe.config.Config;
import com.diemlife.dao.UserActivationPinCodeDAO;
import com.diemlife.models.User;
import com.diemlife.models.UserActivationPinCode;
import play.Logger;
import com.diemlife.utils.DAOProvider;
import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import com.diemlife.models.Quests;
import java.sql.SQLException;
import play.db.Database;
import com.diemlife.dao.QuestsDAO;
import static com.diemlife.constants.QuestMode.PACE_YOURSELF;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class UserActivationService {

    private static final List<Integer> DIGITS = IntStream.rangeClosed(0, 9).boxed().collect(toList());

    @Autowired
    private Config configuration;
    
    @Autowired
    private DAOProvider daoProvider;
    
    @Autowired
    private QuestService questService;

    @Autowired
    private Database db;

    public boolean isValidPinCode(final User user, final String code) {
        return jpaApi.withTransaction(em -> {
            final UserActivationPinCodeDAO dao = daoProvider.userActivationPinCodeDAO(em);
            final UserActivationPinCode activationCode = dao.getUserActivationPinCode(user, code);
            return isValid(activationCode);
        });
    }

    public void consumePinCode(final User user, final String code) {
        jpaApi.withTransaction(em -> {
            final UserActivationPinCodeDAO dao = daoProvider.userActivationPinCodeDAO(em);
            final UserActivationPinCode activationCode = dao.getUserActivationPinCode(user, code);
            if (isValid(activationCode)) {
                return dao.consume(activationCode);
            } else {
                return null;
            }
        });
    }

    public UserActivationPinCode generateCode(final User user) {
        return jpaApi.withTransaction(em -> {
            final UserActivationPinCodeDAO dao = daoProvider.userActivationPinCodeDAO(em);
            final String pinCode = generatePinCode(dao, user);
            if (isBlank(pinCode)) {
                Logger.error(format("Unable to generate the activation code for user [%s]", user.getEmail()));
                return null;
            }
            final UserActivationPinCode activationCode = new UserActivationPinCode();
            activationCode.pinCode = pinCode;
            activationCode.user = user;
            activationCode.createdOn = Calendar.getInstance().getTime();
            return dao.save(activationCode, UserActivationPinCode.class);
        });
    }

    public void populateStartQuest(User user) {
        // TODO: this can be moved up to a higher level later

        try (Connection c = db.getConnection()) {
            final Quests quest;
            //We are finding by ID here - so if anything changes this will break.
            if (configuration.getString("play.env").equalsIgnoreCase("LOCAL") ||
                    configuration.getString("play.env").equalsIgnoreCase("DEV")) {
                quest = QuestsDAO.findById(115);
            } else {
                quest = QuestsDAO.findById(222);
            }

            questService.startQuest(c, quest, quest.getUser(), user, PACE_YOURSELF, null, null);
        } catch (SQLException e) {
            Logger.error("populateStartQuest - error", e);
        }
    }

    private boolean isValid(final UserActivationPinCode activationCode) {
        final int ttlSeconds = configuration.getInt("application.activation.pin.ttl");
        if (activationCode == null) {
            return false;
        }
        final Calendar checkPoint = Calendar.getInstance();
        checkPoint.add(Calendar.SECOND, -1 * ttlSeconds);
        return checkPoint.getTime().before(activationCode.createdOn);
    }

    private String generatePinCode(final UserActivationPinCodeDAO dao, final User user) {
        final int retries = configuration.getInt("application.activation.pin.retries");
        final int codeLength = configuration.getInt("application.activation.pin.length");
        for (int retry = 0; retry < retries; retry++) {
            final String code = randomDigits(codeLength);
            final UserActivationPinCode existingCode = dao.getUserActivationPinCode(user, code);
            if (existingCode == null) {
                return code;
            }
        }
        return null;
    }

    private static String randomDigits(final int length) {
        if (DIGITS.size() < length) {
            throw new IllegalArgumentException();
        }
        final List<Integer> digits = new ArrayList<>(DIGITS);
        Collections.shuffle(digits);
        final StringBuilder builder = new StringBuilder();
        for (int index = 0; index < length; index++) {
            builder.append(digits.get(index));
        }
        return builder.toString();
    }

}

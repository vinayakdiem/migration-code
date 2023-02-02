package com.diemlife.providers;

import com.typesafe.config.Config;
import com.diemlife.constants.Interests;
import com.diemlife.dao.UserHome;
import com.diemlife.models.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import play.db.jpa.JPAApi;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import com.diemlife.services.OutgoingEmailService;
import com.diemlife.services.StripeAccountCreator;
import com.diemlife.services.StripeConnectService;
import com.diemlife.services.UserActivationService;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Consumer;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNumeric;

@Singleton
public class RedisAuthProvider implements StripeAccountCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisAuthProvider.class);

    private final JPAApi jpaApi;
    private final JedisPool jedisPool;
    private final StripeConnectService stripeConnectService;
    private final UserActivationService userActivationService;

    @Inject
    public RedisAuthProvider(final JPAApi jpaApi,
                             final Config config,
                             final OutgoingEmailService emailService,
                             final StripeConnectService stripeConnectService,
                             final UserActivationService userActivationService) {
        this.jpaApi = jpaApi;
        this.jedisPool = setupRedis(config);
        this.stripeConnectService = stripeConnectService;
        this.userActivationService = userActivationService;

        try (final Jedis poolInit = jedisPool.getResource()) {
            poolInit.sync();
        }

        subscribeToAccountCreation(jpaApi, emailService, config);
    }

    @Override
    public StripeConnectService stripeConnectService() {
        return stripeConnectService;
    }

    @Override
    public JPAApi jpaApi() {
        return jpaApi;
    }

    private JedisPool setupRedis(final Config config) {
        final String host = config.getString("application.redis.host");
        final int port = config.getInt("application.redis.port");
        final int database = config.getInt("application.redis.database");
        final String client = StringUtils.trimToNull(config.getString("application.redis.client"));
        final String password = StringUtils.trimToNull(config.getString("application.redis.password"));
        final long connectTimeout = Duration.ofSeconds(config.getLong("application.redis.timeout.connect")).toMillis();
        final long soTimeout = Duration.ofSeconds(config.getInt("application.redis.timeout.so")).toMillis();
        final int poolMin = config.getInt("application.redis.pool.min");
        final int poolMax = config.getInt("application.redis.pool.max");
        final long poolTimeout = Duration.ofSeconds(config.getInt("application.redis.pool.timeout")).toMillis();

        final JedisPoolConfig poolConfig = buildPoolConfig(poolMin, poolMax, poolTimeout);

        return new JedisPool(poolConfig, host, port, (int) connectTimeout, (int) soTimeout, password, database, client);
    }

    private void subscribeToAccountCreation(final JPAApi jpaApi, final OutgoingEmailService emailService, final Config config) {
        listenChannel(config.getString("application.redis.accountChannel"), userId -> {
            LOGGER.info("Received account creation notification for user ID " + userId);

            if (isNumeric(userId)) {
                jpaApi.withTransaction(em -> {
                    final User user = UserHome.findById(Integer.parseInt(userId), em);
                    if (user == null) {
                        LOGGER.warn("Account channel listener could not find user with ID " + userId);
                    } else {
                        createStripeCustomer(user);

                        emailService.sendAccountCreatedEmail(user);

                        // private turned public to start social users on start your diemlife
                        userActivationService.populateStartQuest(user);

                        // add favorties to user
                        addInterestsToUser(user);

                        LOGGER.info(format("Welcome email sent to '%s'", user.getEmail()));
                    }
                    return em;
                });
            }
        });
    }

    public void addInterestsToUser(final User user) {
        Arrays.stream(Interests.values()).forEach(interest -> {
            addUserFavorites(user, interest.getValue().toUpperCase());
        });
    }

    private void addUserFavorites(User newUser, String favorite) {
        EntityManager em = this.jpaApi.em();
        UserHome userDao = new UserHome();

        if (newUser != null && favorite != null) {
            userDao.addUserFavoritesToUser(newUser, favorite, em);
        }
    }

    private JedisPoolConfig buildPoolConfig(final int min, final int max, final long timeout) {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();

        poolConfig.setMaxTotal(max);
        poolConfig.setMaxIdle(max);
        poolConfig.setMinIdle(min);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(timeout).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(timeout).toMillis());
        poolConfig.setBlockWhenExhausted(true);

        return poolConfig;
    }

    public String get(final String key) {
        try (final Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }

    public void listenChannel(final String channelName, final Consumer<String> consumer) {
        final JedisPubSub jedisPubSub = new JedisPubSub() {
            @Override
            public void onSubscribe(final String channel, final int subscribedChannels) {
                LOGGER.info(format("Now listening to Redis channel '%s'", channel));
            }

            @Override
            public void onUnsubscribe(final String channel, final int subscribedChannels) {
                LOGGER.info(format("Stopped listening to Redis channel '%s'", channel));
            }

            @Override
            public void onMessage(final String channel, final String message) {
                LOGGER.debug(format("Received message from channel '%s': %s", channel, message));
                consumer.accept(message);
            }
        };
        new SimpleAsyncTaskExecutor("act-pub-sub-").execute(() -> {
            LOGGER.info(format("Subscribing to Redis channel '%s'", channelName));

            try (final Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(jedisPubSub, channelName);
            }
        });
    }

    public void remove(final String token) {
        try (final Jedis jedis = jedisPool.getResource()) {
            jedis.del(token);
        }
    }

}

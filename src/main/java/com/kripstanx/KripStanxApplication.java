package com.kripstanx;

import com.kripstanx.config.ApplicationProperties;
import com.kripstanx.config.ApplicationPropertiesListener;
import com.kripstanx.config.DefaultProfileUtil;
import com.kripstanx.constants.ProfileConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties({ ApplicationProperties.class })
public class KripStanxApplication {

	private static final Logger log = LoggerFactory.getLogger(KripStanxApplication.class);

	private final Environment env;

	public KripStanxApplication(Environment env) {
		this.env = env;
	}

	@Autowired
	ApplicationProperties applicationProperties;

	/**
	 * Initializes KripStanx App
	 */
	@PostConstruct
	public void initApplication() {
		Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
		if (activeProfiles.contains(ProfileConstants.SPRING_PROFILE_DEVELOPMENT) && activeProfiles.contains(
				ProfileConstants.SPRING_PROFILE_PRODUCTION)) {
			log.error("You have misconfigured your application! It should not run " +
					"with both the 'dev' and 'prod' profiles at the same time.");
		}

		log.info("Default user count ({})", applicationProperties.getDefaultUsers().size());
		insertOrUpdateDefaultUsers();
	}

	private void insertOrUpdateDefaultUsers() {
		log.debug("Inserting / Updating default users...");
//		applicationProperties.getDefaultUsers().stream().forEach(propUser -> {
//			if (!userRepository.findOneByLogin(propUser.getLogin()).isPresent()) {
//				propUser.setActivated(true);
//				propUser.setPasswordExpirationDate(Instant.now().plus(Duration.ofDays(365)));
//				propUser.setLangKey("en");
//				userRepository.save(propUser);
//				userService.deleteUser("evict caches");
//				log.debug("Inserting default user ({})", propUser.getName());
//			}
//		});
	}

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication app = new SpringApplication(KripStanxApplication.class);

		app.addListeners(new ApplicationPropertiesListener());
		DefaultProfileUtil.addDefaultProfile(app);
		Environment env = app.run(args).getEnvironment();
		String protocol = "http";
		if (env.getProperty("server.ssl.key-store") != null) {
			protocol = "https";
		}
		String hostAddress = "localhost";
		try {
			hostAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			log.warn("The host name could not be determined, using `localhost` as fallback");
		}
		log.info("\n----------------------------------------------------------\n\t" +
						"Application '{}' is running! Access URLs:\n\t" +
						"Local: \t\t{}://localhost:{}\n\t" +
						"External: \t{}://{}:{}\n\t" +
						"Profile(s): \t{}\n----------------------------------------------------------",
				env.getProperty("spring.application.name"),
				protocol,
				env.getProperty("server.port"),
				protocol,
				hostAddress,
				env.getProperty("server.port"),
				env.getActiveProfiles());
	}

}

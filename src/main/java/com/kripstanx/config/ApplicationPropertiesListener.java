package com.kripstanx.config;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

@Component
public class ApplicationPropertiesListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private final Logger log = LoggerFactory.getLogger(ApplicationPropertiesListener.class);

    private String region;
    private String secretName;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent) {
        ConfigurableEnvironment environment = applicationEnvironmentPreparedEvent.getEnvironment();
        region = environment.getProperty("aws.region");
        secretName = environment.getProperty("aws.secret-manager.secret-name");

        if (Objects.nonNull(region) && Objects.nonNull(secretName)) {
            loadPropertiesFromAws(environment);
        }
    }

    private void loadPropertiesFromAws(ConfigurableEnvironment environment) {
        log.info("Loading properties from Aws");
        Optional<String> maybeSecretString = getSecretStringFromAWSSecretsManager();
        maybeSecretString.ifPresent(secretString -> {
            JsonNode jsonNode = parseStringToJson(secretString);

            checkMandatoryPropertiesAvailability(jsonNode);

            Properties props = new Properties();
            for (Iterator<Entry<String, JsonNode>> i = jsonNode.fields(); i.hasNext(); ) {
                Entry<String, JsonNode> next = i.next();
                props.put(next.getKey(), next.getValue().textValue());
            }

            environment.getPropertySources().addFirst(new PropertiesPropertySource("aws.secret-manager", props));
            log.info("Aws properties loaded");
        });
    }

    private Optional<String> getSecretStringFromAWSSecretsManager() {
        AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
                                                                 .withRegion(Regions.fromName(region))
                                                                 .build();
        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
            .withSecretId(secretName);

        GetSecretValueResult getSecretValueResult = client.getSecretValue(getSecretValueRequest);

        return Optional.ofNullable(getSecretValueResult.getSecretString());
    }

    private JsonNode parseStringToJson(String from) {
        try {
            return new ObjectMapper().readTree(from);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkMandatoryPropertiesAvailability(JsonNode jsonNode) {
        Stream.of("spring.datasource.username",
                  "spring.datasource.password",
                  "jhipster.security.authentication.jwt.secret").forEach(propertyName -> {
            if (!jsonNode.hasNonNull(propertyName)) {
                throw new RuntimeException(String.format("Missing property from the AWS Secret Manager. (%s)",
                                                         propertyName));
            }
        });

        boolean missingTrustAndKeyStoreParameters =
            !(jsonNode.hasNonNull("application.email.soa.key-store-password")
                || jsonNode.hasNonNull("application.email.soa.trust-store-password"));
        if (missingTrustAndKeyStoreParameters) {
            throw new RuntimeException(String.format(
                "Missing property from the AWS Secret Manager. (application.email.soa.key-store-password or application.email.soa.trust-store-password is mandatory)"));
        }
    }
}

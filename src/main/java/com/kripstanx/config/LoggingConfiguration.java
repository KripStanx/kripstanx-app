package com.kripstanx.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.boolex.OnMarkerEvaluator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.EvaluatorFilter;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterReply;
import tech.jhipster.config.JHipsterProperties;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfiguration {

    private final Logger log = LoggerFactory.getLogger(LoggingConfiguration.class);

    private static final String ASYNC_METRICS_APPENDER_NAME = "ASYNC_MetricsAppender";

    private LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    public LoggingConfiguration(JHipsterProperties jHipsterProperties) {
        setMetricsMarkerLogbackFilter(context);
    }

    // Configure a log filter to remove "metrics" logs from all appenders except the "LOGSTASH" appender
    private void setMetricsMarkerLogbackFilter(LoggerContext context) {
        log.info("Filtering metrics logs from all appenders");
        EvaluatorFilter<ILoggingEvent> logEverythingExceptMetricsFilter = createMarkerFilter(context, "metrics", FilterReply.DENY);
        logEverythingExceptMetricsFilter.setOnMatch(FilterReply.DENY);
        EvaluatorFilter<ILoggingEvent> logOnlyMetricsFilter = createMarkerFilter(context, "metrics", FilterReply.ACCEPT);
        logOnlyMetricsFilter.setOnMismatch(FilterReply.DENY);

        for (ch.qos.logback.classic.Logger logger : context.getLoggerList()) {
            for (Iterator<Appender<ILoggingEvent>> it = logger.iteratorForAppenders(); it.hasNext(); ) {
                Appender<ILoggingEvent> appender = it.next();
                if (appender.getName().equals(ASYNC_METRICS_APPENDER_NAME)) {
                    log.debug("Enable metrics logs for the {} appender", appender.getName());
                    appender.addFilter(logOnlyMetricsFilter);
                } else {
                    log.debug("Disable metrics logs for the {} appender", appender.getName());
                    appender.addFilter(logEverythingExceptMetricsFilter);
                }
                appender.setContext(context);
                appender.start();
            }
        }
    }

    private EvaluatorFilter<ILoggingEvent> createMarkerFilter(LoggerContext context,
                                                              String markerName,
                                                              FilterReply onMatchRule) {
        OnMarkerEvaluator onMarkerMetricsEvaluator = new OnMarkerEvaluator();
        onMarkerMetricsEvaluator.setContext(context);
        onMarkerMetricsEvaluator.addMarker(markerName);
        onMarkerMetricsEvaluator.start();
        EvaluatorFilter<ILoggingEvent> metricsFilter = new EvaluatorFilter<>();
        metricsFilter.setContext(context);
        metricsFilter.setEvaluator(onMarkerMetricsEvaluator);
        metricsFilter.start();
        return metricsFilter;
    }

    /**
     * Logback configuration is achieved by configuration file and API. When configuration file change is detected, the
     * configuration is reset. This listener ensures that the programmatic configuration is also re-applied after
     * reset.
     */
    class LogbackLoggerContextListener extends ContextAwareBase implements LoggerContextListener {

        @Override
        public boolean isResetResistant() {
            return true;
        }

        @Override
        public void onStart(LoggerContext context) {
            // Nothing to do.
        }

        @Override
        public void onReset(LoggerContext context) {
            // Nothing to do.
        }

        @Override
        public void onStop(LoggerContext context) {
            // Nothing to do.
        }

        @Override
        public void onLevelChange(ch.qos.logback.classic.Logger logger, Level level) {
            // Nothing to do.
        }
    }

}

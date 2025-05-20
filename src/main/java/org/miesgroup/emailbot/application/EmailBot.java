package org.miesgroup.emailbot.application;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.miesgroup.emailbot.service.email.EmailManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusMain
public class EmailBot implements QuarkusApplication {

    @Inject
    EmailManager emailManager;

    private static final Logger LOG = LoggerFactory.getLogger(EmailBot.class);

    @Override
    public int run(String... args) {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        LOG.info("✅ Applicazione avviata. In attesa delle attività schedulate...");
        emailManager.DailyEmailAlert();
        Quarkus.waitForExit();
        return 0;
    }

    public static void main(String[] args) {
        Quarkus.run(EmailBot.class, args);
    }
}


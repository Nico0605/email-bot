package org.miesgroup.emailbot.application;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import static io.quarkus.arc.impl.UncaughtExceptions.LOGGER;

@QuarkusMain
public class WebScraper implements QuarkusApplication {

    @Override
    public int run(String... args) {
        LOGGER.info("✅ Applicazione avviata. In attesa delle attività schedulate...");
        Quarkus.waitForExit();
        return 0;
    }

    public static void main(String[] args) {
        Quarkus.run(WebScraper.class, args);
    }
}

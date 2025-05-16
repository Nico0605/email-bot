package org.miesgroup.emailbot.application;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.miesgroup.emailbot.service.email.EmailManager;

@QuarkusMain
public class WebScraper implements QuarkusApplication {
    @Inject
    EmailManager emailManager;

    @Override
    public int run(String... args) {
        System.out.println("✅ Applicazione avviata. In attesa delle attività schedulate...");
        Quarkus.waitForExit();
        return 0;
    }

    public static void main(String[] args) {
        Quarkus.run(WebScraper.class, args);
    }
}

package org.kiwi.console;

import org.kiwi.console.util.ConsoleConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@EnableTransactionManagement
@EnableScheduling
@EnableWebSocket
@EnableAsync
public class ConsoleApplication {

    public static void main(String[] args) {
        parseArgs(args);
        SpringApplication.run(ConsoleApplication.class);
    }

    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-config")) {
                if (i >= args.length - 1) {
                    System.err.println("Invalid options");
                    System.exit(1);
                }
                ConsoleConfig.CONFIG_PATH = args[++i];
            }
        }
    }


}

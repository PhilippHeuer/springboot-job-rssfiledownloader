package job;

import job.service.CLIArguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
@Slf4j
public class ConsoleApplication implements CommandLineRunner {

    @Autowired
    private Environment env;

    @Autowired
    private CLIArguments cliArguments;

    /**
     * App Entrypoint
     *
     * @param args Args
     */
    public static void main(String[] args) {
        SpringApplication.run(ConsoleApplication.class, args);
    }

    /**
     * Executed Run Method
     *
     * @param args Args
     */
    @Override
    public void run(String... args) {
        // CLI
        // - Parse Arguments
        cliArguments.parseArguments(args);
        // - Call Implementation
        // - call your own code here and use the parsed arguments
    }
}

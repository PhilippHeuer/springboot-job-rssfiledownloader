package job.service;

import lombok.extern.slf4j.Slf4j;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.springframework.stereotype.Service;

import static java.lang.System.exit;

@Service
@Slf4j
public class CLIArguments {

    @Option(name="-example", usage="Example CLI Parameter")
    String configAction = "sample";

    public void parseArguments(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            log.error(e.getMessage());
            parser.printUsage(System.err);
            exit(1);
        }
    }

}

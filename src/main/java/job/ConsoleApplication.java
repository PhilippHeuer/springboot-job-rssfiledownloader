package job;

import com.apptastic.rssreader.Item;
import com.apptastic.rssreader.RssReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import job.domain.RSSConfig;
import job.service.RSSDownloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.io.File;
import java.util.stream.Stream;

@SpringBootApplication
@Slf4j
public class ConsoleApplication implements CommandLineRunner {

    @Autowired
    private Environment env;

    @Autowired
    private RSSDownloadService rssDownloadService;

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
        rssDownloadService.downloadNewFiles();
    }
}

package job.service;

import com.apptastic.rssreader.Item;
import com.apptastic.rssreader.RssReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import job.domain.RSSConfig;
import job.domain.RSSFeedRule;
import job.domain.RSSStateFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class RSSDownloadService {

    @Autowired
    private Environment env;

    // ObjectMapper
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public RSSDownloadService() {
        // register modules
        mapper.findAndRegisterModules();
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }

    /**
     * Download new Files
     */
    public void downloadNewFiles() {
        try {
            String configPath = env.getProperty("path.config");
            log.info("Using config directory: {}", configPath);

            String torrentFilePath = env.getProperty("path.data");
            log.info("Using data directory: {}", torrentFilePath);

            // parse config
            RSSConfig config = mapper.readValue(new File(configPath + File.separator + "config.yaml"), RSSConfig.class);

            // parse state
            final RSSStateFile currentState;
            if (Files.exists(new File(torrentFilePath + File.separator + "state.yaml").toPath())) {
                currentState = mapper.readValue(new File(torrentFilePath + File.separator + "state.yaml"), RSSStateFile.class);
            } else {
                currentState = new RSSStateFile();
            }

            // for each feed
            config.getFeeds().forEach(feed -> {
                // DateFormat df = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

                // get last modified file
                LocalDateTime feedLastChecked = currentState.getLastCheckedAtForFeed(feed.getName());
                log.info("Fetching all new torrent files after: " + feedLastChecked);

                try {
                    // parse feed
                    RssReader reader = new RssReader();
                    List<Item> rssFeed = reader.read(feed.getUrl()).collect(Collectors.toList());

                    // for each item
                    for(Item item : rssFeed) {
                        // skip if it's not a new torrent
                        LocalDateTime currentTorrentDate =  LocalDateTime.parse(item.getPubDate().get(), formatter);
                        if (currentTorrentDate.compareTo(feedLastChecked) <= 0) {
                            log.debug(currentTorrentDate + ": " + item.getTitle().get() + " -> Skipped since it was already loaded!");
                            continue;
                        }

                        // regex filter rules
                        Integer matchesFilter = 0;
                        for (RSSFeedRule rule : feed.getRules()) {
                            if (rule.getType().equalsIgnoreCase("regex")) {
                                log.debug("Checking regex rule [{}] for match in [{}]", rule.getValue(), item.getTitle().get());

                                if (item.getTitle().get().matches(rule.getValue())) {
                                    matchesFilter++;
                                }
                            }
                        }

                        // exclude filters
                        Integer matchesExclude = 0;
                        for (RSSFeedRule rule : feed.getExclude()) {
                            if (rule.getType().equalsIgnoreCase("regex")) {
                                log.debug("Checking regex rule [{}] for match in [{}]", rule.getValue(), item.getTitle().get());

                                if (item.getTitle().get().matches(rule.getValue())) {
                                    matchesExclude++;
                                }
                            }
                        }

                        // download torrent file
                        if (matchesFilter > 0 && matchesExclude == 0) {
                            log.info(currentTorrentDate + ": " + item.getTitle().get() + " -> " + item.getLink().get());
                            FileUtils.copyURLToFile(new URL(item.getLink().get()), new File(torrentFilePath + File.separator + item.getTitle().get() + ".torrent"), 5000, 5000);
                        } else if (matchesExclude > 0) {
                            log.debug(currentTorrentDate + ": " + item.getTitle().get() + " -> Skipped because it matches a exclusion filter rule!");
                        } else {
                            log.debug(currentTorrentDate + ": " + item.getTitle().get() + " -> Skipped since no filter matches!");
                        }
                    }

                    // store latest entry for this feed
                    LocalDateTime mostRecentItem = rssFeed
                        .stream()
                        .map(i -> LocalDateTime.parse(i.getPubDate().get(), formatter))
                        .sorted(Comparator.reverseOrder())
                        .findFirst()
                        .get();
                    currentState.getLastCheckedFeedAt().put(feed.getName(), mostRecentItem);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            // save last state
            mapper.writeValue(new File(torrentFilePath + File.separator + "state.yaml"), currentState);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

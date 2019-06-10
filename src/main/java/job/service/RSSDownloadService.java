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
import java.io.FileFilter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class RSSDownloadService {

    @Autowired
    private Environment env;

    // ObjectMapper
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

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
            RSSStateFile currentState = new RSSStateFile(0L);
            if (Files.exists(new File(torrentFilePath + File.separator + "state.yaml").toPath())) {
                currentState = mapper.readValue(new File(torrentFilePath + File.separator + "state.yaml"), RSSStateFile.class);
            }

            // get last modified file
            Date latestTorrentFileDate = new Date(currentState.getLastCheckedAt());
            log.info("Fetching all new torrent files after: " + latestTorrentFileDate.toString());

            // for each feed
            config.getFeeds().forEach(feed -> {
                try {
                    // parse feed
                    RssReader reader = new RssReader();
                    Stream<Item> rssFeed = reader.read(feed.getUrl());

                    // for each item
                    for(Item item : rssFeed.collect(Collectors.toList())) {
                        // skip if it's not a new torrent
                        DateFormat df = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                        Date currentTorrentDate =  df.parse(item.getPubDate().get());

                        if (currentTorrentDate.before(latestTorrentFileDate)) {
                            log.debug(currentTorrentDate + ": " + item.getTitle().get() + " -> Skipped since it was already loaded!");
                            continue;
                        }

                        // regex filter rules
                        Boolean matchesFilter = false;
                        for (RSSFeedRule rule : feed.getRules()) {
                            if (rule.getType().equalsIgnoreCase("regex")) {
                                log.debug("Checking regex rule [{}] for match in [{}]", rule.getValue(), item.getTitle().get());

                                if (item.getTitle().get().matches(rule.getValue())) {
                                    matchesFilter = true;
                                }
                            }
                        }

                        // download torrent file
                        if (matchesFilter) {
                            log.info(currentTorrentDate + ": " + item.getTitle().get() + " -> " + item.getLink().get());
                            FileUtils.copyURLToFile(new URL(item.getLink().get()), new File(torrentFilePath + File.separator + item.getTitle().get() + ".torrent"), 5000, 5000);
                        } else {
                            log.debug(currentTorrentDate + ": " + item.getTitle().get() + " -> Skipped since no filter matches!");
                        }

                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            // save last state
            currentState.setLastCheckedAt(new Date().getTime());
            mapper.writeValue(new File(torrentFilePath + File.separator + "state.yaml"), currentState);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

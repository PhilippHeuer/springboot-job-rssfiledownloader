package job.service;

import com.apptastic.rssreader.Item;
import com.apptastic.rssreader.RssReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import job.domain.RSSConfig;
import job.domain.RSSFeedRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
            log.debug(config.toString());

            // get last modified file
            Long lastTorrentFileModified = lastFileModified(torrentFilePath) == null ? 0l : lastFileModified(torrentFilePath).lastModified();
            Date latestTorrentFileDate = new Date(lastTorrentFileModified);
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
                            log.debug(currentTorrentDate + ": " + item.getTitle().get() + " -> Skipped.");
                        }

                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File lastFileModified(String dir) {
        File fl = new File(dir);
        File[] files = fl.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        return choice;
    }

}

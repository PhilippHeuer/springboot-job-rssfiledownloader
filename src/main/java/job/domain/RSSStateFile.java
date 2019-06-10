package job.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RSSStateFile {

    private Map<String, LocalDateTime> lastCheckedFeedAt = new HashMap<>();

    public LocalDateTime getLastCheckedAtForFeed(String feed) {
        if (lastCheckedFeedAt.containsKey(feed)) {
            return lastCheckedFeedAt.get(feed);
        } else {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
        }
    }

}

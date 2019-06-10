package job.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RSSFeed {

    private String name;

    private String url;

    private List<RSSFeedRule> rules;

    private List<RSSFeedRule> exclude;

}

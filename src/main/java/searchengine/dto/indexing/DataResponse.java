package searchengine.dto.indexing;

import lombok.Data;

@Data
public class DataResponse {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;
}

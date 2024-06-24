package searchengine.dto.indexing;

import lombok.Data;

@Data
public class IndexingResponse {
    private boolean result;
    private String error;
    private int count;
    private DataResponse[] data;
}

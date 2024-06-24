package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface SearchService {
    IndexingResponse search(String query, String site);
}

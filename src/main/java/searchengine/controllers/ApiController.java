package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> indexing(){
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {return ResponseEntity.ok(indexingService.stopIndexing()); }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> updatePage(@RequestParam(value = "url") String url)
    {
        return ResponseEntity.ok(indexingService.updateIndex(url));
    }

    @GetMapping("/search")
    public ResponseEntity<IndexingResponse> search(@RequestParam(value = "query") String query,
                                                   @RequestParam(value = "site", required = false) String site)
    {
        return ResponseEntity.ok(searchService.search(query, site));
    }
}

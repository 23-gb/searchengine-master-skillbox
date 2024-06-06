package searchengine.dto.indexing;

import lombok.Data;

@Data
public class IndexingResponse {
    private boolean result;
    private String error;

    public IndexingResponse() {
        result = true;
        error = null;
    }

    public IndexingResponse(String error) {
        result = false;
        this.error = error;
    }
}

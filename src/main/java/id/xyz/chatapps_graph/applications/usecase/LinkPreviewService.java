package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.entity.LinkPreview;
import java.util.concurrent.CompletableFuture;

public interface LinkPreviewService {

  CompletableFuture<LinkPreview> fetchPreviewAsync(String url);
}

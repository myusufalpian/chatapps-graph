package id.xyz.chatapps_graph.framework.controller;

import id.xyz.chatapps_graph.applications.usecase.ExportService;
import id.xyz.chatapps_graph.framework.dto.BaseResponse;
import id.xyz.chatapps_graph.framework.dto.ExportJobResponse;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
public class ExportController extends BaseApiController {
  private final ExportService exportService;

  @PostMapping
  public ResponseEntity<BaseResponse<ExportJobResponse>> request(@RequestAttribute("X-User-Id") Long userId) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(new BaseResponse<>(exportService.request(userId), new id.xyz.chatapps_graph.framework.dto.BaseMetadata("Export requested")));
  }

  @GetMapping("/{exportUuid}")
  public ResponseEntity<BaseResponse<ExportJobResponse>> status(@RequestAttribute("X-User-Id") Long userId,
      @PathVariable UUID exportUuid) {
    return success(exportService.status(userId, exportUuid), "Export status");
  }

  @GetMapping("/{exportUuid}/download")
  public ResponseEntity<Void> download(@RequestAttribute("X-User-Id") Long userId,
      @PathVariable UUID exportUuid) {
    return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION,
        exportService.downloadUrl(userId, exportUuid)).build();
  }
}

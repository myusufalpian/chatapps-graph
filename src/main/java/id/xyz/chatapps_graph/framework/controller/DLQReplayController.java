package id.xyz.chatapps_graph.framework.controller;

import id.xyz.chatapps_graph.applications.usecase.DLQReplayService;
import id.xyz.chatapps_graph.framework.dto.BaseResponse;
import id.xyz.chatapps_graph.framework.dto.BaseMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/internal/v1/dlq")
@RequiredArgsConstructor
public class DLQReplayController extends BaseApiController {

  private final DLQReplayService replayService;

  @PostMapping("/{taskId}/replay")
  @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
  public ResponseEntity<BaseResponse<Void>> replay(
      @RequestAttribute("X-User-Id") Long operatorId,
      @PathVariable String taskId,
      @RequestParam(value = "force", defaultValue = "false") boolean force,
      @RequestParam("reason") String reason) {
    
    replayService.replay(operatorId, taskId, force, reason);
    return success(null, "Task replayed successfully");
  }
}

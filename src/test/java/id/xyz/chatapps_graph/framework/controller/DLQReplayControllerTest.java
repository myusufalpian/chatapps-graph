package id.xyz.chatapps_graph.framework.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import id.xyz.chatapps_graph.applications.usecase.DLQReplayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DLQReplayControllerTest {

  private MockMvc mockMvc;

  @Mock private DLQReplayService replayService;

  @InjectMocks private DLQReplayController controller;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  @DisplayName("POST /internal/v1/dlq/{taskId}/replay: should trigger manual DLQ replay in standalone MockMvc")
  void testReplayEndpointSuccess() throws Exception {
    String taskId = "some-task-uuid";
    doNothing().when(replayService).replay(100L, taskId, false, "Fixing error");

    mockMvc.perform(post("/internal/v1/dlq/{taskId}/replay", taskId)
            .requestAttr("X-User-Id", 100L)
            .param("reason", "Fixing error")
            .param("force", "false")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metadata.message").value("Task replayed successfully"));

    verify(replayService, times(1)).replay(100L, taskId, false, "Fixing error");
  }
}

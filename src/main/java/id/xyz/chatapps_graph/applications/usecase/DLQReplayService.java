package id.xyz.chatapps_graph.applications.usecase;

public interface DLQReplayService {
  void replay(Long operatorId, String taskId, boolean force, String reason);
}

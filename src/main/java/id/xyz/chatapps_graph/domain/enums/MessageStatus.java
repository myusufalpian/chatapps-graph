package id.xyz.chatapps_graph.domain.enums;

public enum MessageStatus {
  ACTIVE(0),
  DELETED(1);

  private final int value;

  MessageStatus(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}

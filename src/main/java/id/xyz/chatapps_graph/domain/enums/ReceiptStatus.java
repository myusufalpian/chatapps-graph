package id.xyz.chatapps_graph.domain.enums;

public enum ReceiptStatus {
  SENT(0),
  DELIVERED(1),
  READ(2);

  private final int value;

  ReceiptStatus(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}

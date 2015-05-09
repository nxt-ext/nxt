package nxt.peer;

final class Errors {

    final static String BLACKLISTED = "Your peer is blacklisted";
    final static String END_OF_FILE = "Unexpected token END OF FILE at position 0.";
    final static String UNKNOWN_PEER = "Your peer address cannot be resolved";
    final static String UNSUPPORTED_REQUEST_TYPE = "Unsupported request type!";
    final static String UNSUPPORTED_PROTOCOL = "Unsupported protocol!";
    final static String INVALID_ANNOUNCED_ADDRESS = "Invalid announced address";
    final static String SEQUENCE_ERROR = "Peer request received before 'getInfo' request";
    final static String MAX_INBOUND_CONNECTIONS = "Maximum number of inbound connections exceeded";

    private Errors() {} // never
}

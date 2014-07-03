package nxt.http;

public enum APITag {

    AE("Asset Exchange"), DGS("Digital Goods Store"), ALIASES("Aliases"), VS("Voting System"), ACCOUNTS("Accounts"),
    BLOCKS("Blockchain queries"), TRANSACTIONS("Transaction handling"), CREATE_TRANSACTION("Create new transactions"),
    INFO("Server status and info"), TOKENS("Hallmarks and tokens"), MESSAGES("Plain and encrypted messages"),
    FORGING("Control forging");

    private final String displayName;

    private APITag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}

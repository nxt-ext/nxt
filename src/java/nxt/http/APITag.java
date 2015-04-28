package nxt.http;

public enum APITag {

    ACCOUNTS("Accounts"), ALIASES("Aliases"), AE("Asset Exchange"), CREATE_TRANSACTION("Create Transaction"),
    BLOCKS("Blocks"), DGS("Digital Goods Store"), FORGING("Forging"), INFO("Server Info"), MESSAGES("Messages"),
	TRANSACTIONS("Transactions"), PHASING("Phasing"), TOKENS("Tokens"), VS("Voting System"),
    MS("Monetary System"), SEARCH("Search"), ACCOUNT_CONTROL("Account Control"), NETWORK("Networking"), DATA("Tagged Data"), UTILS("Utils"), DEBUG("Debug");

    private final String displayName;

    APITag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}

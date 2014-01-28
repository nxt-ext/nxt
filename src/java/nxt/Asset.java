package nxt;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Asset {

    private static final ConcurrentMap<Long, Asset> assets = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Asset> assetNameToAssetMappings = new ConcurrentHashMap<>();

    public static final Collection<Asset> allAssets = Collections.unmodifiableCollection(assets.values());

    public static Asset getAsset(Long id) {
        return assets.get(id);
    }

    public static Asset getAsset(String name) {
        return assetNameToAssetMappings.get(name);
    }

    static void addAsset(Long assetId, Long senderAccountId, String name, String description, int quantity) {
        Asset asset = new Asset(assetId, senderAccountId, name, description, quantity);
        Asset.assets.put(assetId, asset);
        Asset.assetNameToAssetMappings.put(name.toLowerCase(), asset);
    }

    static void clear() {
        assets.clear();
        assetNameToAssetMappings.clear();
    }

    public final Long assetId;
    public final Long accountId;
    public final String name;
    public final String description;
    public final int quantity;

    private Asset(Long assetId, Long accountId, String name, String description, int quantity) {
        this.assetId = assetId;
        this.accountId = accountId;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
    }

}

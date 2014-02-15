package nxt;

import nxt.util.Convert;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Asset {

    private static final ConcurrentMap<Long, Asset> assets = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Asset> assetNameToAssetMappings = new ConcurrentHashMap<>();
    private static final Collection<Asset> allAssets = Collections.unmodifiableCollection(assets.values());

    public static Collection<Asset> getAllAssets() {
        return allAssets;
    }

    public static Asset getAsset(Long id) {
        return assets.get(id);
    }

    public static Asset getAsset(String name) {
        return assetNameToAssetMappings.get(name);
    }

    static void addAsset(Long assetId, Long senderAccountId, String name, String description, int quantity) {
        Asset asset = new Asset(assetId, senderAccountId, name, description, quantity);
        if (Asset.assets.putIfAbsent(assetId, asset) != null) {
            throw new IllegalStateException("Asset with id " + Convert.convert(assetId) + " already exists");
        }
        if (Asset.assetNameToAssetMappings.putIfAbsent(name.toLowerCase(), asset) != null) {
            throw new IllegalStateException("Asset with name " + name.toLowerCase() + " already exists");
        }
    }

    static void removeAsset(Long assetId) {
        Asset asset = Asset.assets.remove(assetId);
        Asset.assetNameToAssetMappings.remove(asset.getName());
    }

    static void clear() {
        Asset.assets.clear();
        Asset.assetNameToAssetMappings.clear();
    }

    private final Long assetId;
    private final Long accountId;
    private final String name;
    private final String description;
    private final int quantity;

    private Asset(Long assetId, Long accountId, String name, String description, int quantity) {
        this.assetId = assetId;
        this.accountId = accountId;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
    }

    public Long getId() {
        return assetId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getQuantity() {
        return quantity;
    }

}

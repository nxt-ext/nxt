package nxt;

import nxt.util.Convert;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VerifyTrace {

    private static final List<String> balanceHeaders = Arrays.asList("balance", "unconfirmed balance");
    private static final List<String> deltaHeaders = Arrays.asList("transaction amount", "transaction fee",
            "generation fee", "trade cost");
    private static final List<String> assetQuantityHeaders = Arrays.asList("asset balance", "unconfirmed asset balance");
    private static final List<String> deltaAssetQuantityHeaders = Arrays.asList("asset quantity", "trade quantity");

    private static String[] headers;

    private static boolean isBalance(int columnIndex) {
        return balanceHeaders.contains(headers[columnIndex]);
    }

    private static boolean isDelta(int columnIndex) {
        return deltaHeaders.contains(headers[columnIndex]);
    }

    private static boolean isAssetQuantity(int columnIndex) {
        return assetQuantityHeaders.contains(headers[columnIndex]);
    }

    private static boolean isDeltaAssetQuantity(int columnIndex) {
        return deltaAssetQuantityHeaders.contains(headers[columnIndex]);
    }

    public static void main(String[] args) {
        String fileName = args.length == 1 ? args[0] : "nxt.trace";
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line = reader.readLine();
            headers = line.split("\t");

            Map<String,Map<String,Long>> totals = new HashMap<>();
            Map<String,Map<String,Map<String,Long>>> accountAssetTotals = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                String[] values = line.split("\t");
                String accountId = values[0];
                Map<String,Long> accountTotals = totals.get(accountId);
                if (accountTotals == null) {
                    accountTotals = new HashMap<>();
                    totals.put(accountId, accountTotals);
                }
                Map<String,Map<String,Long>> accountAssetMap = accountAssetTotals.get(accountId);
                if (accountAssetMap == null) {
                    accountAssetMap = new HashMap<>();
                    accountAssetTotals.put(accountId, accountAssetMap);
                }
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    if (value == null || "".equals(value.trim())) {
                        continue;
                    }
                    if (isBalance(i)) {
                        accountTotals.put(headers[i], Long.parseLong(value));
                    } else if (isDelta(i)) {
                        long previousValue = Convert.nullToZero(accountTotals.get(headers[i]));
                        accountTotals.put(headers[i], previousValue + Long.parseLong(value));
                    } else if (isAssetQuantity(i)) {
                        String assetId = values[1];
                        Map<String,Long> assetTotals = accountAssetMap.get(assetId);
                        if (assetTotals == null) {
                            assetTotals = new HashMap<>();
                            accountAssetMap.put(assetId, assetTotals);
                        }
                        assetTotals.put(headers[i], Long.parseLong(value));
                    } else if (isDeltaAssetQuantity(i)) {
                        String assetId = values[1];
                        Map<String,Long> assetTotals = accountAssetMap.get(assetId);
                        if (assetTotals == null) {
                            assetTotals = new HashMap<>();
                            accountAssetMap.put(assetId, assetTotals);
                        }
                        long previousValue = Convert.nullToZero(assetTotals.get(headers[i]));
                        assetTotals.put(headers[i], previousValue + Long.parseLong(value));
                    }
                }
            }

            Set<String> failed = new HashSet<>();
            for (Map.Entry<String,Map<String,Long>> mapEntry : totals.entrySet()) {
                String accountId = mapEntry.getKey();
                Map<String,Long> accountValues = mapEntry.getValue();
                System.out.println("account: " + accountId);
                for (String balanceHeader : balanceHeaders) {
                    System.out.println(balanceHeader + ": " + accountValues.get(balanceHeader));
                }
                System.out.println("totals:");
                long totalDelta = 0;
                for (String header : deltaHeaders) {
                    long delta = Convert.nullToZero(accountValues.get(header));
                    totalDelta += delta;
                    System.out.println(header + ": " + delta);
                }
                System.out.println("total confirmed balance change: " + totalDelta);
                long balance = accountValues.get("balance");
                if (balance != totalDelta) {
                    System.out.println("ERROR: balance doesn't match total change!!!");
                    failed.add(accountId);
                }
                Map<String,Map<String,Long>> accountAssetMap = accountAssetTotals.get(accountId);
                for (Map.Entry<String,Map<String,Long>> assetMapEntry : accountAssetMap.entrySet()) {
                    String assetId = assetMapEntry.getKey();
                    Map<String,Long> assetValues = assetMapEntry.getValue();
                    System.out.println("asset: " + assetId);
                    for (Map.Entry<String,Long> assetValueEntry : assetValues.entrySet()) {
                        System.out.println(assetValueEntry.getKey() + ": " + assetValueEntry.getValue());
                    }
                    long totalAssetDelta = 0;
                    for (String header : deltaAssetQuantityHeaders) {
                        long delta = Convert.nullToZero(assetValues.get(header));
                        totalAssetDelta += delta;
                    }
                    System.out.println("total confirmed asset quantity change: " + totalAssetDelta);
                    long assetBalance= assetValues.get("asset balance");
                    if (assetBalance != totalAssetDelta) {
                        System.out.println("ERROR: asset balance doesn't match total asset quantity change!!!");
                        failed.add(accountId);
                    }
                }
                System.out.println();
            }
            if (failed.size() > 0) {
                System.out.println("ERROR: " + failed.size() + " accounts have incorrect balances");
                System.out.println(Arrays.toString(failed.toArray(new String[failed.size()])));
            } else {
                System.out.println("SUCCESS: all " + totals.size() + " account balances and asset balances match the transaction and trade totals!");
            }

        } catch (IOException e) {
            System.out.println(e.toString());
            throw new RuntimeException(e);
        }
    }
}

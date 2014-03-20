package nxt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VerifyTrace {

    private static final String accountIdHeader = "account";
    private static final String timestampHeader = "timestamp";
    private static final List<String> balanceHeaders = Arrays.asList("balance", "unconfirmed balance");
    private static final List<String> deltaHeaders = Arrays.asList("transaction amount", "transaction fee",
            "generation fee", "asset cost");

    private static String[] headers;

    private static int accountIdColumn;
    private static int timestampColumn;

    private static boolean isBalance(int columnIndex) {
        return balanceHeaders.contains(headers[columnIndex]);
    }

    private static boolean isDelta(int columnIndex) {
        return deltaHeaders.contains(headers[columnIndex]);
    }

    public static void main(String[] args) {
        String fileName = args.length == 1 ? args[0] : "nxt.trace";
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line = reader.readLine();
            headers = line.split("\t");
            Map<String,Map<String,Long>> totals = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                String[] values = line.split("\t");
                String accountId = values[accountIdColumn];
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    if (value == null || "".equals(value.trim())) {
                        continue;
                    }
                    Map<String,Long> accountTotals = totals.get(accountId);
                    if (accountTotals == null) {
                        accountTotals = new HashMap<>();
                        totals.put(accountId, accountTotals);
                    }
                    if (isBalance(i)) {
                        accountTotals.put(headers[i], Long.parseLong(value));
                    } else if (isDelta(i)) {
                        Long previousValue = accountTotals.get(headers[i]);
                        if (previousValue == null) {
                            previousValue = 0L;
                        }
                        accountTotals.put(headers[i], previousValue + Long.parseLong(value));
                    }
                }
            }

            for (Map.Entry<String,Map<String,Long>> mapEntry : totals.entrySet()) {
                System.out.println("account: " + mapEntry.getKey());
                for (String balanceHeader : balanceHeaders) {
                    System.out.println(balanceHeader + ": " + mapEntry.getValue().get(balanceHeader));
                }
                System.out.println("totals:");
                long totalDelta = 0;
                for (String deltaHeader : deltaHeaders) {
                    Long delta = mapEntry.getValue().get(deltaHeader);
                    if (delta == null) {
                        delta = 0L;
                    }
                    totalDelta += delta;
                    System.out.println(deltaHeader + ": " + delta);
                }
                System.out.println("total change: " + totalDelta);
                boolean fail = false;
                for (String balanceHeader : balanceHeaders) {
                    long balance = mapEntry.getValue().get(balanceHeader);
                    if (balance != totalDelta) {
                        fail = true;
                        System.out.println("ERROR: " + balanceHeader + " doesn't match total change!!!");
                    }
                }
                if (!fail) {
                    System.out.println("OK");
                }
                System.out.println();
            }

        } catch (IOException e) {
            System.out.println(e.toString());
            throw new RuntimeException(e);
        }
    }
}

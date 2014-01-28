package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.Attachment;
import nxt.Blockchain;
import nxt.Genesis;
import nxt.Nxt;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class IssueAsset extends HttpRequestHandler {

    static final IssueAsset instance = new IssueAsset();

    private IssueAsset() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String secretPhrase = req.getParameter("secretPhrase");
        String name = req.getParameter("name");
        String description = req.getParameter("description");
        String quantityValue = req.getParameter("quantity");
        String feeValue = req.getParameter("fee");
        if (secretPhrase == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"secretPhrase\" not specified");

        } else if (name == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"name\" not specified");

        } else if (quantityValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"quantity\" not specified");

        } else if (feeValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"fee\" not specified");

        } else {

            name = name.trim();
            if (name.length() < 3 || name.length() > 10) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"name\" (length must be in [3..10] range)");

            } else {

                String normalizedName = name.toLowerCase();
                int i;
                for (i = 0; i < normalizedName.length(); i++) {

                    if (Convert.alphabet.indexOf(normalizedName.charAt(i)) < 0) {

                        break;

                    }

                }
                if (i != normalizedName.length()) {

                    response.put("errorCode", 4);
                    response.put("errorDescription", "Incorrect \"name\" (must contain only digits and latin letters)");

                } else if (Asset.getAsset(normalizedName) != null) {

                    response.put("errorCode", 8);
                    response.put("errorDescription", "\"" + name + "\" is already used");

                } else {

                    if (description != null && description.length() > 1000) {

                        response.put("errorCode", 4);
                        response.put("errorDescription", "Incorrect \"description\" (length must be not longer than 1000 characters)");

                    } else {

                        try {

                            int quantity = Integer.parseInt(quantityValue);
                            if (quantity <= 0 || quantity > Nxt.MAX_ASSET_QUANTITY) {

                                response.put("errorCode", 4);
                                response.put("errorDescription", "Incorrect \"quantity\" (must be in [1..1'000'000'000] range)");

                            } else {

                                try {

                                    int fee = Integer.parseInt(feeValue);
                                    if (fee < Nxt.ASSET_ISSUANCE_FEE) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"fee\" (must be not less than 1'000)");

                                    } else {

                                        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
                                        Account account = Account.getAccount(publicKey);
                                        if (account == null) {

                                            response.put("errorCode", 6);
                                            response.put("errorDescription", "Not enough funds");

                                        } else {

                                            if (fee * 100L > account.getUnconfirmedBalance()) {

                                                response.put("errorCode", 6);
                                                response.put("errorDescription", "Not enough funds");

                                            } else {

                                                int timestamp = Convert.getEpochTime();

                                                Attachment attachment = new Attachment.ColoredCoinsAssetIssuance(name, description, quantity);
                                                Transaction transaction = Transaction.newTransaction(timestamp, (short)1440, publicKey,
                                                        Genesis.CREATOR_ID, 0, fee, null, attachment);
                                                transaction.sign(secretPhrase);

                                                Blockchain.broadcast(transaction);

                                                response.put("transaction", transaction.getStringId());

                                            }

                                        }

                                    }

                                } catch (Exception e) {

                                    response.put("errorCode", 4);
                                    response.put("errorDescription", "Incorrect \"fee\"" + e.toString());

                                }

                            }

                        } catch (Exception e) {

                            response.put("errorCode", 4);
                            response.put("errorDescription", "Incorrect \"quantity\"");

                        }

                    }

                }

            }

        }
        return response;
    }

}

package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class TransferAsset extends HttpRequestHandler {

    static final TransferAsset instance = new TransferAsset();

    private TransferAsset() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String secretPhrase = req.getParameter("secretPhrase");
        String recipientValue = req.getParameter("recipient");
        String assetValue = req.getParameter("asset");
        String quantityValue = req.getParameter("quantity");
        String feeValue = req.getParameter("fee");
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionValue = req.getParameter("referencedTransaction");
        if (secretPhrase == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"secretPhrase\" not specified");

        } else if (recipientValue == null || "0".equals(recipientValue)) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"recipient\" not specified");

        } else if (assetValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"asset\" not specified");

        } else if (quantityValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"quantity\" not specified");

        } else if (feeValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"fee\" not specified");

        } else if (deadlineValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"deadline\" not specified");

        } else {

            try {

                Long recipient = Convert.parseUnsignedLong(recipientValue);

                try {

                    Long asset = Convert.parseUnsignedLong(assetValue);

                    try {

                        int quantity = Integer.parseInt(quantityValue);
                        if (quantity <= 0 || quantity >= Nxt.MAX_ASSET_QUANTITY) {

                            throw new Exception();

                        }

                        try {

                            int fee = Integer.parseInt(feeValue);
                            if (fee <= 0 || fee >= Nxt.MAX_BALANCE) {

                                throw new Exception();

                            }

                            try {

                                short deadline = Short.parseShort(deadlineValue);
                                if (deadline < 1) {

                                    throw new Exception();

                                }

                                Long referencedTransaction = referencedTransactionValue == null ? null : Convert.parseUnsignedLong(referencedTransactionValue);

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

                                        Integer assetBalance = account.getUnconfirmedAssetBalance(asset);
                                        if (assetBalance == null || quantity > assetBalance) {

                                            response.put("errorCode", 6);
                                            response.put("errorDescription", "Not enough funds");

                                        } else {

                                            int timestamp = Convert.getEpochTime();

                                            Attachment attachment = new Attachment.ColoredCoinsAssetTransfer(asset, quantity);
                                            Transaction transaction = Transaction.newTransaction(timestamp, deadline, publicKey,
                                                    recipient, 0, fee, referencedTransaction, attachment);
                                            transaction.sign(secretPhrase);

                                            Blockchain.broadcast(transaction);

                                            response.put("transaction", transaction.getStringId());

                                        }

                                    }

                                }

                            } catch (Exception e) {

                                response.put("errorCode", 4);
                                response.put("errorDescription", "Incorrect \"deadline\"" + e.toString());

                            }

                        } catch (Exception e) {

                            response.put("errorCode", 4);
                            response.put("errorDescription", "Incorrect \"fee\"");

                        }

                    } catch (Exception e) {

                        response.put("errorCode", 4);
                        response.put("errorDescription", "Incorrect \"quantity\"");

                    }

                } catch (Exception e) {

                    response.put("errorCode", 4);
                    response.put("errorDescription", "Incorrect \"asset\"");

                }

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"recipient\"");

            }

        }
        return response;
    }

}

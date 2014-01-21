package nxt.http;


import nxt.Account;
import nxt.Alias;
import nxt.Attachment;
import nxt.Genesis;
import nxt.Nxt;
import nxt.Peer;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class AssignAlias extends HttpRequestHandler {

    static final AssignAlias instance = new AssignAlias();

    private AssignAlias() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {
        String secretPhrase = req.getParameter("secretPhrase");
        String alias = req.getParameter("alias");
        String uri = req.getParameter("uri");
        String feeValue = req.getParameter("fee");
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionValue = req.getParameter("referencedTransaction");

        JSONObject response = new JSONObject();

        if (secretPhrase == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"secretPhrase\" not specified");

        } else if (alias == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"alias\" not specified");

        } else if (uri == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"uri\" not specified");

        } else if (feeValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"fee\" not specified");

        } else if (deadlineValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"deadline\" not specified");

        } else {

            alias = alias.trim();
            if (alias.length() == 0 || alias.length() > 100) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"alias\" (length must be in [1..100] range)");

            } else {

                String normalizedAlias = alias.toLowerCase();
                int i;
                for (i = 0; i < normalizedAlias.length(); i++) {

                    if (Convert.alphabet.indexOf(normalizedAlias.charAt(i)) < 0) {

                        break;

                    }

                }
                if (i != normalizedAlias.length()) {

                    response.put("errorCode", 4);
                    response.put("errorDescription", "Incorrect \"alias\" (must contain only digits and latin letters)");

                } else {

                    uri = uri.trim();
                    if (uri.length() > 1000) {

                        response.put("errorCode", 4);
                        response.put("errorDescription", "Incorrect \"uri\" (length must be not longer than 1000 characters)");

                    } else {

                        try {

                            int fee = Integer.parseInt(feeValue);
                            if (fee <= 0 || fee >= Nxt.MAX_BALANCE) {

                                throw new Exception();

                            }

                            try {

                                short deadline = Short.parseShort(deadlineValue);
                                if (deadline < 1) {

                                    throw new Exception();
                                    //TODO: better error checking
                                    // cfb: This is a part of Legacy API, it isn't worth rewriting

                                }

                                long referencedTransaction = referencedTransactionValue == null ? 0 : Convert.parseUnsignedLong(referencedTransactionValue);

                                byte[] publicKey = Crypto.getPublicKey(secretPhrase);
                                long accountId = Account.getId(publicKey);
                                Account account = Nxt.accounts.get(accountId);
                                if (account == null) {

                                    response.put("errorCode", 6);
                                    response.put("errorDescription", "Not enough funds");

                                } else {

                                    if (fee * 100L > account.getUnconfirmedBalance()) {

                                        response.put("errorCode", 6);
                                        response.put("errorDescription", "Not enough funds");

                                    } else {

                                        Alias aliasData = Nxt.aliases.get(normalizedAlias);
                                        if (aliasData != null && aliasData.account != account) {

                                            response.put("errorCode", 8);
                                            response.put("errorDescription", "\"" + alias + "\" is already used");

                                        } else {

                                            int timestamp = Convert.getEpochTime();

                                            Transaction transaction = new Transaction(Transaction.TYPE_MESSAGING,
                                                    Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT, timestamp, deadline,
                                                    publicKey, Genesis.CREATOR_ID, 0, fee, referencedTransaction);
                                            transaction.attachment = new Attachment.MessagingAliasAssignment(alias, uri);
                                            transaction.sign(secretPhrase);

                                            JSONObject peerRequest = new JSONObject();
                                            peerRequest.put("requestType", "processTransactions");
                                            JSONArray transactionsData = new JSONArray();
                                            transactionsData.add(transaction.getJSONObject());
                                            peerRequest.put("transactions", transactionsData);

                                            Peer.sendToSomePeers(peerRequest);

                                            response.put("transaction", transaction.getStringId());

                                            Nxt.nonBroadcastedTransactions.put(transaction.id, transaction);

                                        }

                                    }

                                }

                            } catch (Exception e) {

                                response.put("errorCode", 4);
                                response.put("errorDescription", "Incorrect \"deadline\"");

                            }

                        } catch (Exception e) {

                            response.put("errorCode", 4);
                            response.put("errorDescription", "Incorrect \"fee\"");

                        }

                    }

                }

            }

        }
        return response;
    }

}

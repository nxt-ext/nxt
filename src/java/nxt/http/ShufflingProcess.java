package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.Shuffling;
import nxt.ShufflingParticipant;
import nxt.crypto.EncryptedData;
import nxt.db.DbIterator;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;

public final class ShufflingProcess extends CreateTransaction {

    static final ShufflingProcess instance = new ShufflingProcess();

    private ShufflingProcess() {
        super(new APITag[] {APITag.SHUFFLING, APITag.CREATE_TRANSACTION},
                "shuffling", "secretPhrase", "recipient", "data");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Shuffling shuffling = ParameterParser.getShuffling(req);
        if (!shuffling.isProcessing()) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 11);
            response.put("errorDescription", "shuffling is not ready for processing, state " + shuffling.getState());
            return JSON.prepare(response);
        }
        Account senderAccount = ParameterParser.getSenderAccount(req);
        if (shuffling.getAssigneeAccountId() != senderAccount.getId()) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 12);
            response.put("errorDescription", String.format("Account %s cannot process shuffling since shuffling assignee is %s",
                    Convert.rsAccount(senderAccount.getId()), Convert.rsAccount(shuffling.getAssigneeAccountId())));
            return JSON.prepare(response);
        }

        String secretPhrase = req.getParameter("secretPhrase");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }
        long recipientId = ParameterParser.getRecipientId(req);

        DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(shuffling.getId());
        Map<Long, Long> mapping = new HashMap<>();
        Map<Long, Long> reverseMapping = new HashMap<>();
        Map<Long, ShufflingParticipant> participantLookup = new HashMap<>();
        for (ShufflingParticipant participant : participants) {
            mapping.put(participant.getAccountId(), participant.getNextAccountId());
            reverseMapping.put(participant.getNextAccountId(), participant.getAccountId());
            participantLookup.put(participant.getAccountId(), participant);
        }
        byte[] data = participantLookup.get(senderAccount.getId()).getData();
        JSONObject tokens;
        JSONArray tokenArray;
        if (data.length == 0) {
            tokens = new JSONObject();
            tokenArray = new JSONArray();
            tokens.put("tokens", tokenArray);
        } else {
            tokens = (JSONObject) JSONValue.parse(Convert.toString(data));
            tokenArray = (JSONArray) tokens.get("tokens");
        }
        JSONArray shuffledTokensArray = new JSONArray();
        for (Object token : tokenArray) {
            String tokenData = (String)token;
            byte[] tokenBytes = Convert.toBytes(tokenData);
            EncryptedData encryptedData = EncryptedData.readEncryptedData(ByteBuffer.wrap(tokenBytes), 0, tokenBytes.length);
            Account previous = Account.getAccount(reverseMapping.get(senderAccount.getId()));
            data = previous.decryptFrom(encryptedData, secretPhrase);
            shuffledTokensArray.add(data);
        }
        JSONObject shuffledTokens = new JSONObject();
        shuffledTokens.put("tokens", shuffledTokensArray);

        Long id = senderAccount.getId(); // TODO null vs 0 might be a problem
        while (mapping.get(id) != null) {
            id = mapping.get(id);
        }
        if (id == senderAccount.getId()) {
            // TODO handle last participant everything is decrypted
        }
        EncryptedData encryptedData;
        JSONObject token = new JSONObject();
        token.put("recipient", recipientId);
        while (id != senderAccount.getId()) {
            encryptedData = Account.getAccount(id).encryptTo(Convert.toBytes(token.toJSONString()), secretPhrase);
            token = new JSONObject();
            token.put("recipient", Convert.toHexString(encryptedData.getData()));
            id = reverseMapping.get(id);
        }
        shuffledTokensArray.add(token);
        Collections.shuffle(shuffledTokensArray);
        byte[] tokenBytes = Convert.toBytes(shuffledTokens.toJSONString());


//        Attachment attachment = new Attachment.MonetarySystemShufflingDistribution(shuffling.getId());
//
//        return createTransaction(req, senderAccount, attachment);
        return null;
    }
}

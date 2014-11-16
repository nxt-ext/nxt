package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import nxt.Shuffling;
import nxt.ShufflingParticipant;
import nxt.crypto.EncryptedData;
import nxt.db.DbIterator;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Logger;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;

public final class ShufflingProcess extends CreateTransaction {

    static final ShufflingProcess instance = new ShufflingProcess();

    private ShufflingProcess() {
        super(new APITag[]{APITag.SHUFFLING, APITag.CREATE_TRANSACTION},
                "shuffling", "secretPhrase", "recipient");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Shuffling shuffling = ParameterParser.getShuffling(req);
        if (!shuffling.isProcessingAllowed()) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 11);
            response.put("errorDescription", "Shuffling is not ready for processing, stage " + shuffling.getStage());
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

        // Read the participant list for the shuffling
        DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(shuffling.getId());
        Map<Long, Long> mapping = new HashMap<>();
        Map<Long, Long> reverseMapping = new HashMap<>();
        Map<Long, ShufflingParticipant> participantLookup = new HashMap<>();
        for (ShufflingParticipant participant : participants) {
            mapping.put(participant.getAccountId(), participant.getNextAccountId());
            reverseMapping.put(participant.getNextAccountId(), participant.getAccountId());
            participantLookup.put(participant.getAccountId(), participant);
        }
        if (participantLookup.get(senderAccount.getId()) == null) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 13);
            response.put("errorDescription", String.format("Account %s is not a participant of shuffling %d",
                    Convert.rsAccount(senderAccount.getId()), shuffling.getId()));
            return JSON.prepare(response);
        }

        // Read the encrypted participant data for the sender account token (the first sender won't have any data)
        byte[] dataBytes = participantLookup.get(senderAccount.getId()).getData();
        List<EncryptedData> inputDataList = EncryptedData.getUnmarshaledDataList(dataBytes);

        // decrypt the tokens bundled in the current data
        List<EncryptedData> outputDataList = new ArrayList<>();
        for (EncryptedData encryptedData : inputDataList) {
            // Since we cannot tell which participant public key was used to encrypted which token
            // we'll have to try them all until one succeed starting with the shuffling issuer which has to be a participant
            byte[] decryptedBytes = null;
            long id = shuffling.getIssuerId();
            while (mapping.get(id) != 0) {
                Account publicKeyAccount = Account.getAccount(id);
                if (Logger.isDebugEnabled()) {
                    Logger.logDebugMessage(String.format("decryptFrom using public key of %s sender %s data %s nonce %s",
                            Convert.rsAccount(publicKeyAccount.getId()), Convert.rsAccount(senderAccount.getId()),
                            Arrays.toString(encryptedData.getData()), Arrays.toString(encryptedData.getNonce())));
                }
                try {
                    decryptedBytes = publicKeyAccount.decryptFrom(encryptedData, secretPhrase);
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof InvalidCipherTextException) {
                        // That's fine, this participant did not encrypt the token try the next one
                        Logger.logDebugMessage("failed " + e.getMessage());
                        id = mapping.get(id);
                        continue;
                    } else {
                        throw e;
                    }
                }
                if (Logger.isDebugEnabled()) {
                    Logger.logDebugMessage(String.format("decryptFrom bytes %s", Arrays.toString(decryptedBytes)));
                }
                break;
            }
            if (decryptedBytes == null) {
                // This should never happen, as long as the data is intact one of the participants will be able to decrypt it
                JSONObject response = new JSONObject();
                response.put("errorCode", 15);
                response.put("errorDescription", String.format("Cannot decrypt token for account %s of shuffling %d",
                        Convert.rsAccount(senderAccount.getId()), shuffling.getId()));
                return JSON.prepare(response);
            }
            outputDataList.add(EncryptedData.unmarshalData(new DataInputStream(new ByteArrayInputStream(decryptedBytes))));
        }

        // We always encrypt the recipient using the public key of the last participant
        // Therefore we need to find the last participant
        long id = senderAccount.getId();
        while (mapping.get(id) != 0) {
            id = mapping.get(id);
        }

        // Calculate the token for the current sender by iteratively encrypting it using the public key of all the participants
        // which did not perform shuffle processing yet
        EncryptedData encryptedData = new EncryptedData(Convert.toBytes(Convert.toUnsignedLong(recipientId)), new byte[]{});
        byte[] bytesToEncrypt = EncryptedData.marshalData(encryptedData);
        // If we are that last participant to process then we do not encrypt our recipient
        while (id != senderAccount.getId() && id != 0) {
            Account account = Account.getAccount(id);
            if (Logger.isDebugEnabled()) {
                Logger.logDebugMessage(String.format("encryptTo %s by %s bytes %s",
                        Convert.rsAccount(account.getId()), Convert.rsAccount(senderAccount.getId()), Arrays.toString(bytesToEncrypt)));
            }
            encryptedData = account.encryptTo(bytesToEncrypt, secretPhrase);
            bytesToEncrypt = EncryptedData.marshalData(encryptedData);
            if (Logger.isDebugEnabled()) {
                Logger.logDebugMessage(String.format("encryptTo data %s nonce %s",
                        Arrays.toString(encryptedData.getData()), Arrays.toString(encryptedData.getNonce())));
            }
            id = reverseMapping.get(id);
        }
        outputDataList.add(encryptedData);

        // Shuffle the tokens and save the shuffled tokens as the participant data
        Collections.shuffle(outputDataList, EncryptedData.getSecureRandom());
        ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(bytesStream);
        for (EncryptedData outputData : outputDataList) {
            EncryptedData.marshalData(dataOutputStream, outputData);
        }
        byte[] data = bytesStream.toByteArray();
        Attachment attachment = new Attachment.MonetarySystemShufflingProcessing(shuffling.getId(), data);
        return createTransaction(req, senderAccount, attachment);
    }

}

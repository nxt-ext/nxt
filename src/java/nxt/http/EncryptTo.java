package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_RECIPIENT;

public final class EncryptTo extends APIServlet.APIRequestHandler {

    static final EncryptTo instance = new EncryptTo();

    private EncryptTo() {
        super("recipient", "note", "secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Long recipientId = ParameterParser.getRecipientId(req);
        Account recipientAccount = Account.getAccount(recipientId);
        if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
            return INCORRECT_RECIPIENT;
        }
        String secretPhrase = ParameterParser.getSecretPhrase(req);
        byte[] note = ParameterParser.getNote(req);
        EncryptedData encryptedNote = recipientAccount.encryptTo(note, secretPhrase);

        JSONObject response = new JSONObject();
        response.put("encryptedNote", Convert.toHexString(encryptedNote.getData()));
        response.put("encryptedNoteNonce", Convert.toHexString(encryptedNote.getNonce()));
        return response;

    }

}

var hash = {
	init: SHA256_init,
	update: SHA256_write,
	getBytes: SHA256_finalize
};

var nxtCrypto = function(curve25519, hash, converters) {
	function simpleHash(message) {
		hash.init();
		hash.update(message);
		return hash.getBytes();
	}

	function areByteArraysEqual(bytes1, bytes2) {
		if (bytes1.length !== bytes2.length)
			return false;

		for (var i = 0; i < bytes1.length; ++i) {
			if (bytes1[i] !== bytes2[i])
				return false;
		}

		return true;
	}

	function getPublicKey(secretPhrase, isAccountNumber) {
		if (isAccountNumber) {
			var accountNumber = secretPhrase;
			//synchronous!
			NRS.sendRequest("getAccountPublicKey", {
				"account": accountNumber
			}, function(response) {
				if (!response.publicKey) {
					throw new Exception("Account does not have a public key.");
				} else {
					return response.publicKey;
				}
			}, false);
		} else {
			var secretPhraseBytes = converters.hexStringToByteArray(secretPhrase);
			var digest = simpleHash(secretPhraseBytes);
			return converters.byteArrayToHexString(curve25519.keygen(digest).p);
		}
	}

	function getPrivateKey(secretPhrase) {
		SHA256_init();
		SHA256_write(converters.stringToByteArray(secretPhrase));
		return converters.shortArrayToHexString(curve25519_clamp(converters.byteArrayToShortArray(SHA256_finalize())));
	}

	function curve25519_clamp(curve) {
		curve[0] &= 0xFFF8;
		curve[15] &= 0x7FFF;
		curve[15] |= 0x4000;
		return curve;
	}

	function getAccountId(secretPhrase) {
		var publicKey = getPublicKey(converters.stringToHexString(secretPhrase));

		/*	
		if (NRS.accountInfo && NRS.accountInfo.publicKey && publicKey != NRS.accountInfo.publicKey) {
			return -1;
		}
		*/
		var hex = converters.hexStringToByteArray(publicKey);

		hash.init();
		hash.update(hex);

		var account = hash.getBytes();

		account = converters.byteArrayToHexString(account);

		var slice = (converters.hexStringToByteArray(account)).slice(0, 8);

		return byteArrayToBigInteger(slice).toString();
	}

	function byteArrayToBigInteger(byteArray, startIndex) {
		var value = new BigInteger("0", 10);
		var temp1, temp2;
		for (var i = byteArray.length - 1; i >= 0; i--) {
			temp1 = value.multiply(new BigInteger("256", 10));
			temp2 = temp1.add(new BigInteger(byteArray[i].toString(10), 10));
			value = temp2;
		}

		return value;
	}

	function encryptData(plaintext, myPrivateKey, theirPublicKey) {
		try {
			var compressedPlaintext = pako.deflate(new Uint8Array(converters.stringToByteArray(plaintext)));

			var nonce = new Uint8Array(32);
			window.crypto.getRandomValues(nonce);

			var data = nxtCrypto.aesEncrypt(compressedPlainText, myPrivateKey, theirPublicKey, nonce);

			return {
				"nonce": nonce,
				"data": data
			};
		} catch (e) {
			//
		}
	}

	//var myPrivateKey = converters.hexStringToByteArray(nxtCrypto.getPrivateKey(myPassword));
	//var theirPublicKey = converters.hexStringToByteArray(nxtCrypto.getPublicKey(theirAccount, true));

	function decryptData(data, nonce, myPrivateKey, theirPublicKey) {
		try {

			var compressedPlaintext = nxtCrypto.aesDecrypt(data, myPrivateKey, theirPublicKey, nonce);

			var binData = new Uint8Array(compressedPlaintext);

			var data = pako.inflate(binData);

			return data;

			/*
			// Decode base64 (convert ascii to binary)
			var strData = atob(compressedPlaintext);

			// Convert binary string to character-number array
			var charData = strData.split('').map(function(x) {
				return x.charCodeAt(0);
			});

			// Convert gunzipped byteArray back to ascii string:
			var strData = String.fromCharCode.apply(null, new Uint16Array(data));

			return strData;
			*/
		} catch (e) {
			//..
		}
	}

	/**
	 * Encrypt a message given a private key and a public key.
	 * @param1: plaintext         Array of bytes: message that needs to be encrypted
	 * @param2: myPrivateKey      Array of bytes: private key of the sender of the message
	 * @param3: theirPublicKey    Array of bytes: public key of the receiver of the message
	 * @param4: nonce             Array of bytes: the nonce
	 *
	 * @return:                   Array of bytes:
	 *                               First 16 bytes is the initialization vector.
	 *                               Rest is the encrypted text.
	 */
	function aesEncrypt(plaintext, myPrivateKey, theirPublicKey, nonce) {
		// CryptoJS likes WordArray parameters
		var text = converters.byteArrayToWordArray(plaintext);
		var sharedKey = crypto.getSharedKey(myPrivateKey, theirPublicKey);
		for (var i = 0; i < 32; i++) {
        	sharedKey[i] ^= nonce[i];
    	}
		var key = CryptoJS.SHA256(converters.byteArrayToWordArray(sharedKey));
		var tmp = new Uint8Array(16);
		window.crypto.getRandomValues(tmp);
		var iv = converters.byteArrayToWordArray(tmp);
		var encrypted = CryptoJS.AES.encrypt(text, key, {
			iv: iv
		});
		var ivOut = converters.wordArrayToByteArray(encrypted.iv);
		var ciphertextOut = converters.wordArrayToByteArray(encrypted.ciphertext);

		return ivOut.concat(ciphertextOut);
	}

	/**
	 * Decrypt a message given a private key and a public key.
	 * @param1: ivCiphertext      Array of bytes: 
	 *                               First 16 bytes is the initialization vector.
	 *                               Rest is the encrypted text.
	 * @param2: myPrivateKey      Array of bytes: private key of the sender of the message
	 * @param3: theirPublicKey    Array of bytes: public key of the receiver of the message
	 * @param4: nonce             Array of bytes: the nonce
	 * 
	 * @return:                   Array of bytes: decrypted text.
	 */
	function aesDecrypt(ivCiphertext, myPrivateKey, theirPublicKey, nonce) {
		if (ivCiphertext.length < 16 || ivCiphertext.length % 16 != 0) {
			throw {
				name: "invalid ciphertext"
			};
		}
		var iv = converters.byteArrayToWordArray(ivCiphertext.slice(0, 16));
		var ciphertext = converters.byteArrayToWordArray(ivCiphertext.slice(16));
		var sharedKey = crypto.getSharedKey(myPrivateKey, theirPublicKey);
		for (var i = 0; i < 32; i++) {
        	sharedKey[i] ^= nonce[i];
    	}
		var key = CryptoJS.SHA256(converters.byteArrayToWordArray(sharedKey));
		var encrypted = CryptoJS.lib.CipherParams.create({
			ciphertext: ciphertext,
			iv: iv,
			key: key
		});
		var decrypted = CryptoJS.AES.decrypt(encrypted, key, {
			iv: iv
		});
		var plaintext = converters.wordArrayToByteArray(decrypted);

		return plaintext;
	}

	function getSharedKey(key1, key2) {
		return converters.shortArrayToByteArray(curve25519(converters.byteArrayToShortArray(key1), converters.byteArrayToShortArray(key2), null));
	}

	function sign(message, secretPhrase) {
		var messageBytes = converters.hexStringToByteArray(message);
		var secretPhraseBytes = converters.hexStringToByteArray(secretPhrase);

		var digest = simpleHash(secretPhraseBytes);
		var s = curve25519.keygen(digest).s;

		var m = simpleHash(messageBytes);

		hash.init();
		hash.update(m);
		hash.update(s);
		var x = hash.getBytes();

		var y = curve25519.keygen(x).p;

		hash.init();
		hash.update(m);
		hash.update(y);
		var h = hash.getBytes();

		var v = curve25519.sign(h, x, s);

		return converters.byteArrayToHexString(v.concat(h));
	}

	function verify(signature, message, publicKey) {
		var signatureBytes = converters.hexStringToByteArray(signature);
		var messageBytes = converters.hexStringToByteArray(message);
		var publicKeyBytes = converters.hexStringToByteArray(publicKey);
		var v = signatureBytes.slice(0, 32);
		var h = signatureBytes.slice(32);
		var y = curve25519.verify(v, h, publicKeyBytes);

		var m = simpleHash(messageBytes);

		hash.init();
		hash.update(m);
		hash.update(y);
		var h2 = hash.getBytes();

		return areByteArraysEqual(h, h2);
	}

	return {
		getPublicKey: getPublicKey,
		getPrivateKey: getPrivateKey,
		encryptData: encryptData,
		decryptData: decryptData,
		getAccountId: getAccountId,
		sign: sign,
		verify: verify
	};

}(curve25519, hash, converters);
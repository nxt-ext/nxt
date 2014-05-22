var EncryptedData = {
	data: null,
	nonce: null,

	encryptedData: function(data, nonce) {
		this.data = data;
		this.nonce = nonce;
	},

	encrypt: function(plaintext, myPrivateKey, theirPublicKey) {
		try {
			var compressedPlaintext = pako.deflate(converters.stringToByteArray(plaintext));

			this.nonce = new Uint8Array(16);
			window.crypto.getRandomValues(this.nonce);

			this.data = crypto.aesEncrypt(compressedPlainText, myPrivateKey, theirPublicKey, this.nonce);
		} catch (e) {
			//
		}
	},

	decrypt: function(myPrivateKey, theirPublicKey) {
		try {
			var compressedPlaintext = crypto.aesDecrypt(this.data, myPrivateKey, theirPublicKey, this.nonce);

			// Decode base64 (convert ascii to binary)
			var strData = atob(compressedPlaintext);

			// Convert binary string to character-number array
			var charData = strData.split('').map(function(x) {
				return x.charCodeAt(0);
			});

			// Turn number array into byte-array
			var binData = new Uint8Array(charData);

			// Pako magic
			var data = pako.inflate(binData);

			// Convert gunzipped byteArray back to ascii string:
			var strData = String.fromCharCode.apply(null, new Uint16Array(data));

			return strData;
		} catch (e) {
			//..
		}
	},

	getData: function() {
		return this.data;
	},

	getNonce: function() {
		return this.nonce;
	}
}
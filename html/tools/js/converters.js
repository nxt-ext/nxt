var converters = function () {
    var charToNibble= {};
    var nibbleToChar = [];
    var i;
    for (i = 0; i <= 9; ++i) {
        var char = i.toString();
        charToNibble[char] = i;
        nibbleToChar.push(char)
    }

    for (i = 10; i <= 15; ++i) {
        var lowerChar = String.fromCharCode('a'.charCodeAt(0) + i - 10);
        var upperChar = String.fromCharCode('A'.charCodeAt(0) + i - 10);

        charToNibble[lowerChar] = i;
        charToNibble[upperChar] = i;
        nibbleToChar.push(lowerChar);
    }

    return {
        byteArrayToHexString: function (bytes) {
            var str = '';
            for (var i = 0; i < bytes.length; ++i)
                str += nibbleToChar[bytes[i] >> 4] + nibbleToChar[bytes[i] & 0x0F];

            return str;
        },
        stringToByteArray: function (str) {
            var bytes = new Array(str.length);
            for (var i = 0; i < str.length; ++i)
                bytes[i] = str.charCodeAt(i);

            return bytes;
        },
        hexStringToByteArray: function (str) {
            var bytes = [];
            var i = 0;
            if (0 !== str.length % 2) {
                bytes.push(charToNibble[str.charAt(0)]);
                ++i;
            }

            for (; i < str.length - 1; i += 2)
                bytes.push((charToNibble[str.charAt(i)] << 4) + charToNibble[str.charAt(i + 1)]);

            return bytes;
        },
        stringToHexString: function (str) {
            return this.byteArrayToHexString(this.stringToByteArray(str));
        },
        checkBytesToIntInput: function(bytes, numBytes, opt_startIndex) {
			var startIndex = opt_startIndex || 0;
			if (startIndex < 0) {
				throw new Error('Start index should not be negative');
			}
			
			if (bytes.length < startIndex + numBytes) {
				throw new Error('Need at least ' + (numBytes) +' bytes to convert to an integer');
			}
			return startIndex;
		},
		byteArrayToSignedShort: function(bytes, opt_startIndex) {
			var index = this.checkBytesToIntInput(bytes, 2, opt_startIndex);
			value = bytes[index];
			value += bytes[index + 1] << 8;
			return value;
		},
		byteArrayToSignedInt32: function(bytes, opt_startIndex) {
			var index = this.checkBytesToIntInput(bytes, 4, opt_startIndex);
			value = bytes[index];
			value += bytes[index + 1] << 8;
			value += bytes[index + 2] << 16;
			value += bytes[index + 3] << 24;
			return value;
		},
		byteArrayToBigInteger: function(bytes, opt_startIndex) {
			var index = this.checkBytesToIntInput(bytes, 8, opt_startIndex);
		
		    var value = new BigInteger("0",10);
		    
		    var temp1,temp2;
		    
		    for (var i=7; i>=0; i--) {
		        temp1 = value.multiply(new BigInteger("256",10));
				temp2 = temp1.add(new BigInteger(bytes[opt_startIndex+i].toString(10),10));
				value = temp2;
		    }
		
		    return value;
		},
		byteArrayToString: function(bytes, opt_startIndex, length) {	
			var index = this.checkBytesToIntInput(bytes, parseInt(length, 10), parseInt(opt_startIndex, 10));
			
			var bytes = bytes.slice(opt_startIndex, opt_startIndex+length);
			
			return decodeURIComponent(escape(String.fromCharCode.apply(null, bytes)));
		}
    }
}();

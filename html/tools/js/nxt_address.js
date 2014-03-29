/*
    NXT address class, simple version (check only).

    Version: 1.0, license: Public Domain, coder: NxtChg (admin@nxtchg.com).
*/

function NxtAddress()
{
	var codeword = [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0];
	var syndrome = [0,0,0,0,0];

	var gexp = [ 1, 2, 4, 8, 16, 5, 10, 20, 13, 26, 17, 7, 14, 28, 29, 31, 27, 19, 3, 6, 12, 24, 21, 15, 30, 25, 23, 11, 22, 9, 18, 1 ];
	var glog = [ 0, 0, 1, 18, 2, 5, 19, 11, 3, 29, 6, 27, 20, 8, 12, 23, 4, 10, 30, 17, 7, 22, 28, 26, 21, 25, 9, 16, 13, 14, 24, 15 ];

	var cwmap = [ 3, 2, 1, 0, 7, 6, 5, 4, 13, 14, 15, 16, 12, 8, 9, 10, 11 ];

	var alphabet = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
	//var alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ345679';

	function gmult(a, b)
	{
		if(a == 0 || b == 0) return 0;
	
		var idx = (glog[a] + glog[b]) % 31;
	
		return gexp[idx];
	}//__________________________

	function encode()
	{
		var p = [0,0,0,0];
		
		for(var i = 12; i >= 0; i--)
		{
			var fb = codeword[i] ^ p[3];
	
			p[3] = p[2] ^ gmult(30, fb);
			p[2] = p[1] ^ gmult( 6, fb);
			p[1] = p[0] ^ gmult( 9, fb);
			p[0] =        gmult(17, fb);
		}
	
		codeword[13] = p[0]; codeword[14] = p[1];
		codeword[15] = p[2]; codeword[16] = p[3];
	}//__________________________

	function reset()
	{
		for(var i = 0; i < 17; i++) codeword[i] = 0;
		
		codeword[0] = 1;
	}

	this.ok = function()
	{
		var sum = 0;
	
		for(var i = 1; i < 5; i++)
		{
			for(var j = 0, t = 0; j < 31; j++)
			{
	            if(j > 12 && j < 27) continue;
	
				pos = j; if(j > 26) pos -= 14;
	
				t ^= gmult(codeword[pos], gexp[(i*j)%31]);
			}
	
			sum |= t; syndrome[i] = t;
		}
	
		return (sum == 0);
	}//__________________________

	function from_acc(acc)
	{
		var inp = [], out = [], pos = 0, len = acc.length;
	
		if(len == 20 && acc.charAt(0) != '1') return false;

		for(var i = 0; i < len; i++)
		{
			inp[i] = acc.charCodeAt(i) - '0'.charCodeAt(0);
		}
	
		do // base 10 to base 32 conversion
		{
			var divide = 0, newlen = 0;
	
			for(i = 0; i < len; i++)
			{
				divide = divide * 10 + inp[i];
	
				if(divide >= 32)
				{
					inp[newlen++] = divide >> 5; divide &= 31;
				}
				else if(newlen > 0)
				{
					inp[newlen++] = 0;
				}
			}
	
			len = newlen; out[pos++] = divide;
		}
		while(newlen);
	
		for(i = 0; i < 13; i++) // copy to codeword in reverse, pad with 0's
		{
			codeword[i] = (--pos >= 0 ? out[i] : 0);
		}
	
		encode();
	
		return true;
	}//__________________________

	this.toString = function(prefix)
	{
		var out = (prefix ? 'NXT-' : '');
	
		for(var i = 0; i < 17; i++)
		{
			out += alphabet[codeword[cwmap[i]]];
	
			if((i & 3) == 3 && i < 13) out += '-';
		}
			
		return out;
	}//__________________________

	this.account_id = function()
	{
		var out = '', inp = [], len = 13;
	
		for(var i = 0; i < 13; i++)
		{
			inp[i] = codeword[12-i];
		}
	
		do // base 32 to base 10 conversion
		{
			var divide = 0, newlen = 0;
	
			for(i = 0; i < len; i++)
			{
				divide = divide * 32 + inp[i];
	
				if(divide >= 10)
				{
					inp[newlen++] = Math.floor(divide / 10); divide %= 10;
				}
				else if(newlen > 0)
				{
					inp[newlen++] = 0;
				}
			}
	
			len = newlen; out += String.fromCharCode(divide + '0'.charCodeAt(0));
		}
		while(newlen);
	
		return out.split("").reverse().join("");
	}//__________________________

	this.set = function(adr, allow_accounts)
	{
		if(typeof allow_accounts === 'undefined') allow_accounts = true;
	
		var len = 0; reset();
		
		adr = String(adr);
	
		adr = adr.replace(/(^\s+)|(\s+$)/g, '').toUpperCase();
	
		if(adr.indexOf('NXT-') == 0) adr = adr.substr(4);
	
		if(adr.match(/^\d{1,20}$/g)) // account id
		{
			if(allow_accounts) return from_acc(adr);
		}
		else // address
		{
			for(var i = 0; i < adr.length; i++)
			{
				var pos = alphabet.indexOf(adr[i]);
				
				if(pos >= 0)
				{
					if(len > 16) return false;
		
					codeword[cwmap[len++]] = pos;
				}
			}
		}

		return (len == 17 ? this.ok() : false);
	}
}

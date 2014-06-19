//
//  Copyright (c) 2014, Neil Alexander T.
//  All rights reserved.
//
//  Redistribution and use in source and binary forms, with
//  or without modification, are permitted provided that the following
//  conditions are met:
//
//  - Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
//  - Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
//  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
//  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
//  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
//  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
//  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
//  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
//  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
//  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
//  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
//  POSSIBILITY OF SUCH DAMAGE.
//

package com.frozenriver.sigmavpn;

public class SigmaVPNProtoTAI64 extends SigmaVPNProto
{	
	public class TAI
	{
		public long x;
	}
	
	public class TAIa
	{
		public TAI sec;
		public long nano;
		public long atto;
	}
	
	TAIa cdTAIp, cdTAIe;
	
	public SigmaVPNProtoTAI64(com.neilalexander.jnacl.NaCl crypto)
	{
		super(crypto);
		
		cdTAIp = new TAIa();
		cdTAIp.sec = new TAI();
		
		cdTAIe = new TAIa();
		cdTAIe.sec = new TAI();
		
		System.out.println("nacltai initialised");
	}
	
	public byte[] encode(byte[] input, int length)
	{
		byte[] nonce = new byte[24];
		nonce[7] = (byte) (crypto.getRole() ? 1 : 0);
		
		TAIa_now(cdTAIp);
		TAIa_pack(nonce, 8, cdTAIp);
		
		byte[] enc = crypto.encrypt(input, length, nonce);
		
		for (int i = 0; i < 16; i ++)
			enc[i] = nonce[8 + i];
		
		return enc;
	}
	
	public byte[] decode(byte[] input, int length)
	{
		byte[] nonce = new byte[24];
		nonce[7] = (byte) (crypto.getRole() ? 0 : 1);
		
		for (int i = 0; i < 16; i ++)
		{
			nonce[8 + i] = input[i];
			input[i] = 0;
		}
		
		return crypto.decrypt(input, length, nonce);
	}
	
	public void TAIa_now(TAIa t)
	{
		long millis = System.currentTimeMillis();
		t.sec.x = 4611686018427387914L + (long) millis / 1000;
		t.nano = (millis % 1000) * 1000 * 1000;
		t.atto ++;
	}

	public void TAI_pack(byte[] s, int soffset, TAI t)
	{
		long x = t.x;
		s[7 + soffset] = (byte) (x & 255); x >>>= 8;
		s[6 + soffset] = (byte) (x & 255); x >>>= 8;
		s[5 + soffset] = (byte) (x & 255); x >>>= 8;
		s[4 + soffset] = (byte) (x & 255); x >>>= 8;
		s[3 + soffset] = (byte) (x & 255); x >>>= 8;
		s[2 + soffset] = (byte) (x & 255); x >>>= 8;
		s[1 + soffset] = (byte) (x & 255); x >>>= 8;
		s[0 + soffset] = (byte) x;
	}
	
	public void TAI_unpack(byte[] s, int soffset, TAI t)
	{
		long x;
		
		x = s[0 + soffset];
		x <<= 8; x += s[1 + soffset];
		x <<= 8; x += s[2 + soffset];
		x <<= 8; x += s[3 + soffset];
		x <<= 8; x += s[4 + soffset];
		x <<= 8; x += s[5 + soffset];
		x <<= 8; x += s[6 + soffset];
		x <<= 8; x += s[7 + soffset];
		
		t.x = x;
	}
	
	public void TAIa_pack(byte[] s, int soffset, TAIa t)
	{
		long x;
		
		TAI_pack(s, soffset, t.sec);
		
		soffset += 8;
		x = t.atto;
		
		s[7 + soffset] = (byte)(x & 255); x >>>= 8;
		s[6 + soffset] = (byte)(x & 255); x >>>= 8;
		s[5 + soffset] = (byte)(x & 255); x >>>= 8;
		s[4 + soffset] = (byte) x;
		
		x = t.nano;
		
		s[3 + soffset] = (byte)(x & 255); x >>>= 8;
		s[2 + soffset] = (byte)(x & 255); x >>>= 8;
		s[1 + soffset] = (byte)(x & 255); x >>>= 8;
		s[0 + soffset] = (byte) x;
	}
	
	public void TAIa_unpack(byte[] s, int soffset, TAIa t) 
	{
		int x;
		
		TAI_unpack(s, soffset, t.sec);
		soffset += 8;
		
		x = s[4 + soffset];
		x <<= 8; x += s[5 + soffset];
		x <<= 8; x += s[6 + soffset];
		x <<= 8; x += s[7 + soffset];
		
		t.atto = x;
		
		x = s[0 + soffset];
		x <<= 8; x += s[1 + soffset];
		x <<= 8; x += s[2 + soffset];
		x <<= 8; x += s[3 + soffset];
		
		t.nano = x;
	}
}
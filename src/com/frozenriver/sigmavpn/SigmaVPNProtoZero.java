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

public class SigmaVPNProtoZero extends SigmaVPNProto
{		
	public SigmaVPNProtoZero(com.neilalexander.jnacl.NaCl crypto)
	{
		super(crypto);
		
		System.out.println("nacl0 initialised");
	}
	
	public byte[] encode(byte[] input, int length)
	{
		byte[] enc = crypto.encrypt(input, length, new byte[24]);
		byte[] buf = new byte[enc.length - 16];
		
		for (int i = 0; i < enc.length - 16; i ++)
			buf[i] = enc[16 + i];

		return buf;
	}
	
	public byte[] decode(byte[] input, int length)
	{
		byte[] dec = new byte[length + 16];
		
		for (int i = 0; i < length; i ++)
			dec[16 + i] = input[i];
		
		return crypto.decrypt(dec, dec.length, new byte[24]);
	}
}
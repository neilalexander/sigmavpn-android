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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class SigmaVPNServiceSend implements Runnable
{
	private DatagramChannel tunnel;
	private FileInputStream fileStream;
	private SigmaVPNProto proto;
	private int pollfreq;
	
	public SigmaVPNServiceSend(DatagramChannel tunnel, FileInputStream fileStream, SigmaVPNProto proto, int pollfreq)
	{
		this.tunnel = tunnel;
		this.fileStream = fileStream;
		this.proto = proto;
		this.pollfreq = pollfreq;
	}
	
	public void run()
	{
		int result = 0;
		int packetcount = 1;
		
		do
		{
			try
			{
				if (result <= 0)
				{
					if (packetcount > 1)
						packetcount --;
					
					Thread.sleep(pollfreq / packetcount);
				}
				else
					packetcount ++;
				
				result = process();
			}
			catch (IOException e)
			{
				System.out.println("IOException from send thread");
				return;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			};
		}
		while (result != -1);
	}
	
	public int process() throws Exception
	{
		byte[] packet = new byte[1516];
		int length = fileStream.read(packet);
        
		if (length <= 0)
			return length;
		
		byte[] enc = proto.encode(packet, length);
	
		ByteBuffer buf = ByteBuffer.wrap(enc, 0, enc.length);
		tunnel.write(buf);
		
		return length;
	}
}

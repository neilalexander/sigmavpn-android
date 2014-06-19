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

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class SigmaVPNServiceRecv implements Runnable
{
	private DatagramChannel tunnel;
	private FileOutputStream fileStream;
	private SigmaVPNProto proto;
	
	public SigmaVPNServiceRecv(DatagramChannel tunnel, FileOutputStream fileStream, SigmaVPNProto proto)
	{
		this.tunnel = tunnel;
		this.fileStream = fileStream;
		this.proto = proto;
	}
	
	public void run()
	{
		int result = 0;
		
		do
		{
			try
			{
				if (result <= 0)
					Thread.sleep(1000);
				
				result = process();
			}
			catch (IOException e)
			{
				System.out.println("IOException from recv thread");
				e.printStackTrace();
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
		ByteBuffer packet = ByteBuffer.allocate(1516);
		int length = tunnel.read(packet);
		
		byte[] dec = proto.decode(packet.array(), length);
		
		fileStream.write(dec, 0, dec.length);
		packet.clear();
		
		return length;
	}
}

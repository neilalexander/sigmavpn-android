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

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

import com.frozenriver.sigmavpn.R;
import com.neilalexander.jnacl.*;

public class SigmaVPNService extends VpnService implements Handler.Callback, Runnable
{
    private String _RemoteAddress, _RemotePort, _TunnelAddress, _PrivateKey, _PublicKey, _Protocol, _DNSServers, _StaticRoutes;
    private int _TunnelPrefixLen, _MTU, _LocalPort, _SendPollFreq;
    private boolean _DefineLocalPort;
    private PendingIntent ConfigureIntent;

    private Handler mHandler;
    private Thread mThread;

    private ParcelFileDescriptor mInterface;
    private String mParameters;
    
    private NaCl crypto;
    
    Thread sendThread, recvThread;

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (mHandler == null)
            mHandler = new Handler(this);

        if (mThread != null)
            mThread.interrupt();

        String prefix = getPackageName();
        
        try
        {
        	_RemoteAddress = intent.getStringExtra(prefix + ".REMOTEADDRESS");
        	_RemotePort = intent.getStringExtra(prefix + ".REMOTEPORT");
        	_PrivateKey = intent.getStringExtra(prefix + ".PRIVATEKEY");
        	_PublicKey = intent.getStringExtra(prefix + ".PUBLICKEY");
        	_TunnelAddress = intent.getStringExtra(prefix + ".TUNNELREALADDRESS");
        	_TunnelPrefixLen = Integer.parseInt(intent.getStringExtra(prefix + ".TUNNELPREFIXLEN"));
        	_DNSServers = intent.getStringExtra(prefix + ".DNSSERVERS");
        	_StaticRoutes = intent.getStringExtra(prefix + ".STATICROUTES");
        	_Protocol = intent.getStringExtra(prefix + ".PROTOCOL");
        	_DefineLocalPort = intent.getBooleanExtra(prefix + ".DEFINELOCALPORT", false);
        	_LocalPort = intent.getIntExtra(prefix + ".LOCALPORT", 1234);
        	_MTU = intent.getIntExtra(prefix + ".MTU", 1400);
        	_SendPollFreq = intent.getIntExtra(prefix + ".SENDPOLLFREQ", 1000);
        }
        catch (Exception e)
        {
        	mHandler.sendEmptyMessage(R.string.disconnected);
        	this.stopSelf();
        }
        
        mThread = new Thread(this, "SigmaVPNThread");
        mThread.start();
        
        return START_STICKY;
    }

    public void onDestroy()
    {
        if (mThread != null)
            mThread.interrupt();
    }

    public boolean handleMessage(Message message)
    {
        if (message != null) 
            Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        
        return true;
    }

    public synchronized void run()
    {    	
        try
        {
        	crypto = new NaCl(_PrivateKey, _PublicKey);
        	
           InetSocketAddress server = new InetSocketAddress(_RemoteAddress, Integer.parseInt(_RemotePort));

            for (int attempt = 0; attempt < 10; ++attempt)
            {
                if (run(server))
                    attempt = 0;

                Thread.sleep(3000);
            }
        }
        catch (Exception e)
        {
        	System.out.println(e.getClass().getName() + " reported error: " + e.getMessage());
        }
        finally
        {
            try
            {
                mInterface.close();
            }
            catch (Exception e) {};
            
            mInterface = null;
            mParameters = null;

            mHandler.sendEmptyMessage(R.string.disconnected);
            this.stopSelf();
        }
    }

    private boolean run(InetSocketAddress server) throws Exception
    {
        DatagramChannel tunnel = DatagramChannel.open();
        
        if (!protect(tunnel.socket()))
        {
        	return false;
        }
        
        try
        {
        	if (_DefineLocalPort)
        	{
        		System.out.println("Binding " + _LocalPort + "...");
        		
        		DatagramSocket sock = tunnel.socket();
        		sock.setReuseAddress(true);
        		sock.bind(new InetSocketAddress(_LocalPort));
        	}
        }
        catch (Exception e)
        {
        	System.out.println("Unable to set local socket binding manually");
        }
      
        tunnel.connect(server);
        tunnel.configureBlocking(true);

        String config = "m," + _MTU + " a," + _TunnelAddress + "," + _TunnelPrefixLen + " ";
        
        try
        {
        	if (!_DNSServers.trim().equals(""))
        	{
        		String[] dnsservers = _DNSServers.split(",");

        		for (String dnsserver: dnsservers)
        		{
        			System.out.println("Installing new DNS server: " + dnsserver.trim());
        			config = config.concat("d," + dnsserver.trim() + " ");
        		}
        	}

        	if (!_StaticRoutes.trim().equals(""))
        	{
        		String[] staticroutes = _StaticRoutes.split(",");

        		for (String staticroute: staticroutes)
        		{
        			System.out.println("Installing new static route: " + staticroute.trim());
        			String[] parts = staticroute.trim().split("/");
        			config = config.concat("r," + parts[0] + "," + parts[1] + " ");
        		}
        	}
        }
        catch (Exception e) { }
        
        configure(config);
        
        FileInputStream sendQ = new FileInputStream(mInterface.getFileDescriptor());
        FileOutputStream recvQ = new FileOutputStream(mInterface.getFileDescriptor());
        
        SigmaVPNProto proto;
        
        if (_Protocol.equals("nacl0"))
        	proto = new SigmaVPNProtoZero(crypto);
        else if (_Protocol.equals("nacltai"))
        	proto = new SigmaVPNProtoTAI64(crypto);
        else
        	proto = new SigmaVPNProto(crypto);
    	
        SigmaVPNServiceSend sendTask = new SigmaVPNServiceSend(tunnel, sendQ, proto, _SendPollFreq);
        SigmaVPNServiceRecv recvTask = new SigmaVPNServiceRecv(tunnel, recvQ, proto);
        
        sendThread = new Thread(sendTask);
        recvThread = new Thread(recvTask);
        
        sendThread.start();
        recvThread.start();
        
        sendThread.join();
       // recvThread.join();
        
        tunnel.disconnect();
        tunnel.close();
        
        return true;
    }

    private void configure(String parameters) throws Exception
    {
        if (mInterface != null && parameters.equals(mParameters))
            return;

        Builder builder = new Builder();
        
        for (String parameter : parameters.split(" "))
        {
            String[] fields = parameter.split(",");
            
            try
            {
                switch (fields[0].charAt(0))
                {
                    case 'm':
                        builder.setMtu(Short.parseShort(fields[1]));
                        break;
                    case 'a':
                        builder.addAddress(fields[1], Integer.parseInt(fields[2]));
                        break;
                    case 'r':
                        builder.addRoute(fields[1], Integer.parseInt(fields[2]));
                        break;
                    case 'd':
                        builder.addDnsServer(fields[1]);
                        break;
                    case 's':
                        builder.addSearchDomain(fields[1]);
                        break;
                }
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("Bad parameter: " + parameter);
            }
        }

        try
        {
            mInterface.close();
        }
        catch (Exception e) { };

        mInterface = builder.setSession(_RemoteAddress)
                .setConfigureIntent(ConfigureIntent)
                .establish();
        mParameters = parameters;
    }
}

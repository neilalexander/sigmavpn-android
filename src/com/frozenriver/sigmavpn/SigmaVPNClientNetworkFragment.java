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

import java.net.InetAddress;

import com.frozenriver.sigmavpn.R;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SigmaVPNClientNetworkFragment extends Fragment implements View.OnFocusChangeListener, TextView.OnEditorActionListener
{
	private TextView entryTunnelAddress, entryStaticRoutes, entryDNSServers;
	private SharedPreferences settings;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		setHasOptionsMenu(false);
		
		settings = this.getActivity().getPreferences(Context.MODE_PRIVATE);
		View v = inflater.inflate(R.layout.network, container, false);
		
		entryTunnelAddress = (TextView) v.findViewById(R.id.tunneladdress);
		entryTunnelAddress.setText(settings.getString("TunnelAddress", ""));
		entryTunnelAddress.setOnFocusChangeListener(this);
		entryTunnelAddress.setOnEditorActionListener(this);
		
		entryStaticRoutes = (TextView) v.findViewById(R.id.staticroutes);
		entryStaticRoutes.setText(settings.getString("StaticRoutes", ""));
		entryStaticRoutes.setOnFocusChangeListener(this);
		entryStaticRoutes.setOnEditorActionListener(this);
		
		entryDNSServers = (TextView) v.findViewById(R.id.dnsservers);
		entryDNSServers.setText(settings.getString("DNSServers", ""));
		entryDNSServers.setOnFocusChangeListener(this);
		entryDNSServers.setOnEditorActionListener(this);
		
		return v;
	}

	public boolean validate()
	{
		boolean error = false;

		try
		{
			String[] tokens = entryTunnelAddress.getText().toString().split("/");

			if (Integer.parseInt(tokens[1]) <= 0 ||
					Integer.parseInt(tokens[1]) > 128)
				throw new Exception();

			if (tokens.length != 2)
				throw new Exception();

			InetAddress.getByName(tokens[0]);
		}
		catch (Exception e)
		{
			entryTunnelAddress.setError("IPv4 or IPv6 address in CIDR notation (i.e. 192.168.0.2/24) is required"); 
			error = true;
		}

		return !error;
	}

	public void onFocusChange(View v, boolean hasFocus)
	{
		try
		{
			if (!validate())
			{
			}
		}
		catch (Exception e)
		{
		}
		
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("TunnelAddress", entryTunnelAddress.getText().toString().trim());
		
		try
		{
			String[] tokens = entryTunnelAddress.getText().toString().trim().split("/");
			editor.putString("TunnelRealAddress", tokens[0]);
			editor.putString("TunnelPrefixLen", tokens[1]);
		}
		catch (Exception e)
		{
			entryTunnelAddress.setError("IPv4 or IPv6 address in CIDR notation (i.e. 192.168.0.2/24) is required"); 
		}
		
		editor.putString("DNSServers", entryDNSServers.getText().toString().trim());
		editor.putString("StaticRoutes", entryStaticRoutes.getText().toString().trim());
		editor.commit();
	}
	
	public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2)
	{
		onFocusChange((View) arg0, false);
		return false;
	}
}

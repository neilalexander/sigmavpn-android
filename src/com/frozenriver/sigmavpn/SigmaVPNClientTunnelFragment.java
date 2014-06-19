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

import java.util.Random;

import com.neilalexander.jnacl.NaCl;
import com.neilalexander.jnacl.crypto.curve25519xsalsa20poly1305;
import com.frozenriver.sigmavpn.R;

import android.app.Fragment;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public class SigmaVPNClientTunnelFragment extends Fragment implements View.OnClickListener, View.OnFocusChangeListener, TextView.OnEditorActionListener
{
	private TextView entryRemoteAddress, entryRemotePort,
						entryPrivateKey, entryPublicKey, entryLocalPublicKey;
	private CheckBox entryUseTAI64;
	private SharedPreferences settings;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Random rand = new Random();
		byte[] pk = new byte[32], sk = new byte[32];
		rand.nextBytes(sk);
		curve25519xsalsa20poly1305.crypto_box_getpublickey(pk, sk);
		
		setHasOptionsMenu(true);
		
		settings = this.getActivity().getPreferences(Context.MODE_PRIVATE);
		View v = inflater.inflate(R.layout.tunnel, container, false);
		
		entryRemoteAddress = (TextView) v.findViewById(R.id.remoteaddress);
		entryRemoteAddress.setText(settings.getString("RemoteAddress", ""));
		entryRemoteAddress.setOnFocusChangeListener(this);
		entryRemoteAddress.setOnEditorActionListener(this);

		entryRemotePort = (TextView) v.findViewById(R.id.remoteport);
		entryRemotePort.setText(settings.getString("RemotePort", ""));
		entryRemotePort.setOnFocusChangeListener(this);
		entryRemotePort.setOnEditorActionListener(this);

		entryPrivateKey = (TextView) v.findViewById(R.id.privatekey);
		entryPrivateKey.setText(settings.getString("PrivateKey", NaCl.asHex(sk)));
		entryPrivateKey.setOnFocusChangeListener(this);
		entryPrivateKey.setOnEditorActionListener(this);

		entryLocalPublicKey = (TextView) v.findViewById(R.id.localpublickey);
		entryLocalPublicKey.setText(settings.getString("LocalPublicKey", NaCl.asHex(pk)));
		entryLocalPublicKey.setOnFocusChangeListener(this);
		entryLocalPublicKey.setOnEditorActionListener(this);

		entryPublicKey = (TextView) v.findViewById(R.id.publickey);
		entryPublicKey.setText(settings.getString("RemotePublicKey", ""));
		entryPublicKey.setOnFocusChangeListener(this);
		entryPublicKey.setOnEditorActionListener(this);
		
		entryUseTAI64 = (CheckBox) v.findViewById(R.id.enablenacltai);
		entryUseTAI64.setChecked(settings.getBoolean("UseTAI64", false));
		entryUseTAI64.setOnClickListener(this);

		v.findViewById(R.id.copypublickey).setOnClickListener(this);
		v.findViewById(R.id.genprivatekey).setOnClickListener(this);
		
		return v;
	}
	
	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
		inflater.inflate(R.menu.client, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	ClipboardManager clipboard = (ClipboardManager) this.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
    	
    	switch (item.getItemId())
    	{
    		case R.id.copypublickey:
    			clipboard.setText(((TextView) this.getActivity().findViewById(R.id.localpublickey)).getText().toString());

    			Toast.makeText(this.getActivity(), "Copied public key to clipboard", Toast.LENGTH_SHORT).show();
    			break;
    			
    		case R.id.copyprivatekey:
    			clipboard.setText(((TextView) this.getActivity().findViewById(R.id.privatekey)).getText().toString());

    			Toast.makeText(this.getActivity(), "Copied private key to clipboard", Toast.LENGTH_SHORT).show();
    			break;
    			
    		case R.id.copybothkeys:
    			clipboard.setText("Private: " + ((TextView) this.getActivity().findViewById(R.id.privatekey)).getText().toString() + "\n" +
    								"Public: " + ((TextView) this.getActivity().findViewById(R.id.localpublickey)).getText().toString());

    			Toast.makeText(this.getActivity(), "Copied both keys to clipboard", Toast.LENGTH_SHORT).show();
    			break;
    	}
    	
    	return true;
    }

	public boolean validate()
	{
		boolean error = false;

		if (entryRemoteAddress.getText().length() == 0)
		{
			entryRemoteAddress.setError("Remote address required");
			error = true;
		}

		try
		{
			if (Integer.parseInt(entryRemotePort.getText().toString()) < 1||
					Integer.parseInt(entryRemotePort.getText().toString()) > 65535)
			{
				entryRemotePort.setError("Port number between 1 and 65535 is required");
				error = true;
			}
		}
		catch (NumberFormatException e)
		{
			entryRemotePort.setError("Port number is required");
			error = true;
		}

		if (!entryPublicKey.getText().toString().matches("[0-9A-Fa-f]+"))
		{
			entryPublicKey.setError("Public key should be provided in hexadecimal format");
			error = true;
		}

		if (entryPublicKey.getText().length() != 64)
		{
			entryPublicKey.setError("32-bit key length is required");
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
		editor.putString("RemoteAddress", entryRemoteAddress.getText().toString().trim());
		editor.putString("RemotePort", entryRemotePort.getText().toString().trim());
		editor.putString("PrivateKey", entryPrivateKey.getText().toString().trim());
		editor.putString("LocalPublicKey", entryLocalPublicKey.getText().toString().trim());
		editor.putString("RemotePublicKey", entryPublicKey.getText().toString().trim());
		editor.putBoolean("UseTAI64", entryUseTAI64.isChecked());
		editor.commit();
	}

	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.enablenacltai:
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("UseTAI64", entryUseTAI64.isChecked());
			editor.commit();
			break;
		
		case R.id.copypublickey:
			ClipboardManager clipboard = (ClipboardManager) this.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(entryLocalPublicKey.getText().toString());

			Toast.makeText(this.getActivity(), "Copied public key to clipboard", Toast.LENGTH_SHORT).show();
			break;

		case R.id.genprivatekey:
			Random rand = new Random();
			byte[] pk = new byte[32], sk = new byte[32];
			rand.nextBytes(sk);
			curve25519xsalsa20poly1305.crypto_box_getpublickey(pk, sk);
			entryLocalPublicKey.setText(NaCl.asHex(pk));
			entryPrivateKey.setText(NaCl.asHex(sk));
			break;
		}
	}

	public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2)
	{
		onFocusChange((View) arg0, false);
		return false;
	}
}

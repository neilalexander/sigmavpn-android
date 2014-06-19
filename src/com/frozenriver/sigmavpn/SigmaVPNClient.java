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

import com.frozenriver.sigmavpn.R;

import android.app.ActionBar.Tab;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.SeekBar;

public class SigmaVPNClient extends Activity
{
	SharedPreferences settings;
	Dialog dialog;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	/*if ((System.currentTimeMillis() / 1000) > 1327060800)
    	{
    		this.finish();
    		return;
    	}*/
    	
    	ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        Tab statusTab = actionBar.newTab()
                .setText("Status")
                .setTabListener(new TabListener(new SigmaVPNClientStatusFragment()));
        actionBar.addTab(statusTab);
        
        Tab tunnelTab = actionBar.newTab()
                .setText("Tunnel")
                .setTabListener(new TabListener(new SigmaVPNClientTunnelFragment()));
        actionBar.addTab(tunnelTab);
        
        Tab networkTab = actionBar.newTab()
                .setText("Network")
                .setTabListener(new TabListener(new SigmaVPNClientNetworkFragment()));
        actionBar.addTab(networkTab);
        
        settings = this.getPreferences(Context.MODE_PRIVATE);
    	
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);
    }
    
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.allitems, menu);
		return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.advanced)
        {
        	showAdvancedOptions();
            return true;
        }
        
        return false;
    }
    
    public void showAdvancedOptions()
    {    	
    	dialog = new Dialog(this);
    	
    	dialog.setContentView(R.layout.advanced);
        dialog.setTitle("Advanced Settings");
        dialog.setOwnerActivity(this);
        dialog.setCancelable(true);
        dialog.findViewById(R.id.advancedsavebutton).setOnClickListener(new View.OnClickListener()
        {
        	public void onClick(View v)
			{
				saveAdvancedSettings();
				dialog.dismiss();
			}
        });
        
        CheckBox defineLocalSocket = (CheckBox) dialog.findViewById(R.id.definelocalsocket);
        defineLocalSocket.setChecked(settings.getBoolean("LocalPortSetManually", false));
        
        TextView localPortNumber = (TextView) dialog.findViewById(R.id.localportnumber);
        localPortNumber.setText(Integer.toString(settings.getInt("LocalPort", 1234)));
        
        TextView mtu = (TextView) dialog.findViewById(R.id.mtu);
        mtu.setText(Integer.toString(settings.getInt("MTU", 1400)));
        
        SeekBar sendPollFreq = (SeekBar) dialog.findViewById(R.id.sendpollfreq);
        sendPollFreq.setProgress(settings.getInt("SendPollFreq", 1000));
        sendPollFreq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {			
			public void onStopTrackingTouch(SeekBar arg0) {}
			public void onStartTrackingTouch(SeekBar arg0) {}
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2)
			{
				if (arg0.getProgress() < 100)
					arg0.setProgress(100);
				else
					arg0.setProgress(arg0.getProgress() - (arg0.getProgress() % 100));
				
				TextView sendPollFreqLabel = (TextView) dialog.findViewById(R.id.sendpollfreqlabel);
		        sendPollFreqLabel.setText(arg0.getProgress() + "ms");
			}
		});
        
        TextView sendPollFreqLabel = (TextView) dialog.findViewById(R.id.sendpollfreqlabel);
        sendPollFreqLabel.setText(sendPollFreq.getProgress() + "ms");
        
        dialog.show();    
    }
    
    public void saveAdvancedSettings()
    {
    	if (dialog == null)
    		return;
    	
    	SharedPreferences.Editor edit = settings.edit();
    	edit.putBoolean("LocalPortSetManually", ((CheckBox) dialog.findViewById(R.id.definelocalsocket)).isChecked());
    	
    	int localPortNumber = Integer.parseInt(((TextView) dialog.findViewById(R.id.localportnumber)).getText().toString());
    	if (localPortNumber > 0 && localPortNumber < 65535)
    		edit.putInt("LocalPort", localPortNumber);
    	
    	int mtu = Integer.parseInt(((TextView) dialog.findViewById(R.id.mtu)).getText().toString());
    	if (mtu >= 1280 && mtu <= 1500)
    		edit.putInt("MTU", mtu);
    	
    	edit.putInt("SendPollFreq", ((SeekBar) dialog.findViewById(R.id.sendpollfreq)).getProgress());
    	edit.commit();
    }
    
    @Override
    protected void onActivityResult(int request, int result, Intent data)
    {
        if (result == RESULT_OK)
        {
        	SharedPreferences settings = getPreferences(MODE_PRIVATE);
            String prefix = getPackageName();

            Intent intent = new Intent(this, SigmaVPNService.class)
                    .putExtra(prefix + ".REMOTEADDRESS", settings.getString("RemoteAddress", ""))
                    .putExtra(prefix + ".REMOTEPORT", settings.getString("RemotePort", ""))
                    .putExtra(prefix + ".TUNNELADDRESS", settings.getString("TunnelAddress", ""))
                    .putExtra(prefix + ".TUNNELREALADDRESS", settings.getString("TunnelRealAddress", ""))
                    .putExtra(prefix + ".TUNNELPREFIXLEN", settings.getString("TunnelPrefixLen", ""))
                    .putExtra(prefix + ".PRIVATEKEY", settings.getString("PrivateKey", ""))
                    .putExtra(prefix + ".PUBLICKEY", settings.getString("RemotePublicKey", ""))
                    .putExtra(prefix + ".LOCALPUBLICKEY", settings.getString("LocalPublicKey", ""))
                    .putExtra(prefix + ".DNSSERVERS", settings.getString("DNSServers", ""))
                    .putExtra(prefix + ".STATICROUTES", settings.getString("StaticRoutes", ""))
                    .putExtra(prefix + ".PROTOCOL", settings.getBoolean("UseTAI64", false) ? "nacltai" : "nacl0")
                    .putExtra(prefix + ".MTU", settings.getInt("MTU", 1400))
                    .putExtra(prefix + ".SENDPOLLFREQ", settings.getInt("SendPollFreq", 1000))
                    .putExtra(prefix + ".DEFINELOCALPORT", settings.getBoolean("LocalPortSetManually", false))
                    .putExtra(prefix + ".LOCALPORT", settings.getInt("LocalPort", 1234));
            
            startService(intent);
        }
    }
    
    private boolean checkVPNServiceActive()
    {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if ("com.frozenriver.sigmavpn.SigmaVPNService".equals(service.service.getClassName()))
                return true;
        }
        return false;
    }
    
    public void startVPNService()
    {
    	if (!checkVPNServiceActive())
    	{
    		Intent intent = VpnService.prepare(this);

    		if (intent != null)
    			startActivityForResult(intent, 0);
    		else
    			onActivityResult(0, RESULT_OK, null);
    	}
    }

    private class TabListener implements ActionBar.TabListener
    {
        private Fragment mFragment;

        public TabListener(Fragment fragment)
        {
            mFragment = fragment;
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft)
        {
            ft.add(R.id.mainscrollview, mFragment);
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft)
        {
            ft.remove(mFragment);
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft)
        {
         
        }
    }
}
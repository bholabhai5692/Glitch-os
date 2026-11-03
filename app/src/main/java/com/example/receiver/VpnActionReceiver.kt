package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class VpnActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ROTATE_VPN = "com.example.vpn.ACTION_ROTATE_VPN"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_ROTATE_VPN) {
            Toast.makeText(
                context,
                "Automatic VPN switching is unavailable. Export a live profile to a compatible VPN app.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

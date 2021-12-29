package com.example.ipcserver.ui.messenger

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.ipcserver.IPCExample
import com.example.ipcserver.R
import com.example.ipcserver.databinding.FragmentMessengerBinding

class MessengerFragment : Fragment(), ServiceConnection,  View.OnClickListener {

    companion object {
        // Bundle keys
        const val PID = "pid"
        const val CONNECTION_COUNT = "connection_count"
        const val PACKAGE_NAME = "package_name"
        const val DATA = "data"
        const val MESSAGE = "message"
    }

    private var _binding: FragmentMessengerBinding? = null
    private val viewBinding get() = _binding!!

    // Is bound to the service of remote process
    private var isBound: Boolean = false

    // Messenger on the server
    private var serverMessenger: Messenger? = null

    // Messenger on the client
    private var clientMessenger: Messenger? = null

    // Handle messages from the remote service
    var handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            // Update UI with remote process info
            val bundle = msg.data
            viewBinding.linearLayoutClientInfo.visibility = View.VISIBLE
            viewBinding.btnConnect.text = getString(R.string.disconnect)
            viewBinding.txtServerPid.text = bundle.getInt(PID).toString()
            viewBinding.txtMessagePid.text = bundle.getString(MESSAGE).toString()
            viewBinding.txtServerConnectionCount.text = bundle.getInt(CONNECTION_COUNT).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMessengerBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewBinding.btnConnect.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if(isBound){
            doUnbindService()
        } else {
            doBindService()
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        serverMessenger = Messenger(service)
        // Ready to send messages to remote service
        sendMessageToServer()
    }

    override fun onServiceDisconnected(className: ComponentName) {
        clearUI()
        serverMessenger = null
    }

    private fun clearUI(){
        viewBinding.txtServerPid.text = ""
        viewBinding.txtServerConnectionCount.text = ""
        viewBinding.btnConnect.text = getString(R.string.connect)
        viewBinding.linearLayoutClientInfo.visibility = View.INVISIBLE
    }

    override fun onDestroy() {
        doUnbindService()
        super.onDestroy()
    }

    private fun doBindService() {
        clientMessenger = Messenger(handler)
        Intent("messengerexample").also { intent ->
            intent.`package` = IPCExample::class.java.`package`.name
            activity?.applicationContext?.bindService(intent, this, Context.BIND_AUTO_CREATE)
        }
        isBound = true
    }

    private fun doUnbindService() {
        if (isBound) {
            activity?.applicationContext?.unbindService(this)
            isBound = false
            viewBinding.btnConnect.text = getString(R.string.connect)
        }
    }

    private fun sendMessageToServer() {
        if (!isBound) return
        val message = Message.obtain(handler)
        val bundle = Bundle()
        bundle.putString(DATA, viewBinding.edtClientData.text.toString())
        bundle.putString(PACKAGE_NAME, context?.packageName)
        bundle.putInt(PID, Process.myPid())
        message.data = bundle
        message.replyTo = clientMessenger // we offer our Messenger object for communication to be two-way
        try {
            serverMessenger?.send(message)
        } catch (e: RemoteException) {
            e.printStackTrace()
        } finally {
            message.recycle()
        }
    }
}
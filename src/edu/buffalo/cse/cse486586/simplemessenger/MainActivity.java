package edu.buffalo.cse.cse486586.simplemessenger;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

	final private String SOCKET_TAG = "socket";
	final private String MESSAGE_TALKER = "talker";
	final private String MESSAGE_CONTENT = "content";
	
	private Button btnSend;
	private EditText editText;
	private ListView lstView;
	private List<Map<String, String>> chatList;
	
	private ServerSocket serversocket;
	final private String IP = "10.0.2.2";
	final private int LISTEN_PORT = 10000; 
	private int peerPort = -1; // server port 
	private boolean serverRunning = true;//whether the server is running
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		editText = (EditText)findViewById(R.id.editText1);
		lstView = (ListView)findViewById(R.id.listView1);
		btnSend = (Button)findViewById(R.id.button1);
		chatList = new ArrayList<Map<String, String>>();
		btnSend.setEnabled(false);

		//if enter key pressed, send the message
		editText.setOnKeyListener(new OnKeyListener(){
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if((event.getAction() == KeyEvent.ACTION_DOWN) &&
						(keyCode == KeyEvent.KEYCODE_ENTER)) {
					sendMessage();
					return true;
				}
				return false;
			}
		});
		
		initServer();
		initClient();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
		
	//if server is running then the menu option is disabled. vice versa
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {     
		super.onPrepareOptionsMenu(menu);
		if(serverRunning == false)
			menu.findItem(R.id.menu_reconnect).setEnabled(true);
		else
			menu.findItem(R.id.menu_reconnect).setEnabled(false);
		return true; 
	}
	
	//restart server on menu option
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        if(item.getItemId() == R.id.menu_reconnect){
            initServer();
        }
        return true;
    }

	//get avd port and what port number is of the other avd on vr
	private void initClient() {
		// TODO Auto-generated method stub
		peerPort = -1;
		TelephonyManager tel =
		        (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		Log.i(SOCKET_TAG, "My port:" + portStr);
		if(portStr.equals("5554")) {
			peerPort = 11112;
		}
		else if(portStr.equals("5556")) {
			peerPort = 11108;
		}
		if(peerPort == -1) {
			Log.e(SOCKET_TAG, "My port:" + portStr);
		}
	}

	//start server
	private void initServer() {
		try {
			serversocket = new ServerSocket(LISTEN_PORT);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e(SOCKET_TAG, "Server sock create failed." + e.toString());
		}
		serverRunning = true;
		ServerTask server = new ServerTask();
		server.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serversocket);
	}

	//on press send button
	public void onButton1(View view) {
		sendMessage();
	}
	
	private void sendMessage() {
		String msg = editText.getText().toString();
		if(msg.equals(""))
			return;
		updateListView("me", msg);
		(new ClientTask()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msg.concat("\n"));
		editText.setText("");
	}
	
	private class ClientTask extends AsyncTask<String, Integer, String>{
		final private int START = 0;
		final private int FINISH = 100;
		
		@Override
		protected String doInBackground(String... message){
			Socket socket = null;
			publishProgress(Integer.valueOf(START));
			try {
				socket = new Socket(IP, peerPort);
				OutputStreamWriter output = new OutputStreamWriter(socket.getOutputStream());
				output.write(message[0]);
				output.flush();
				output.close();
				socket.close();
				publishProgress(Integer.valueOf(FINISH));
			} catch (Exception e) {
				Log.e(SOCKET_TAG, "Client sock error." + e.toString());
				return "'" + message[0] + "'" + "sent failed.";
			}
			return null;
		}
		
	    @Override  
	    protected void onProgressUpdate(Integer... progress) {
	    	switch(progress[0].intValue()) {
	    	case START:
	    		btnSend.setEnabled(false);
	    		break;
	    	case FINISH:
	    		btnSend.setEnabled(true);
	    		break;
	    	}
	    }
        @Override  
        protected void onPostExecute(String result) {
        	if(result != null) {
        		updateListView("me", result);
        	}
        }
	}
	
	private class ServerTask extends AsyncTask<ServerSocket, String, String> {

		public boolean run = true;
		@Override
		protected String doInBackground(ServerSocket... ssock) {
			// TODO Auto-generated method stub
			Log.i(SOCKET_TAG, "Init server.");
			btnSend.setEnabled(true);
			try {
				while(run) {
					Socket s = ssock[0].accept();
					Log.i(SOCKET_TAG, "message received.");
				    BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
				    String message = input.readLine();
				    String peer = s.getInetAddress().getHostAddress();
				    Log.i(SOCKET_TAG, "msg:" + message);
				    Log.i(SOCKET_TAG, "ip:" + peer);
				    publishProgress(peer, message);
				    input.close();
				    s.close();  
				}  
				ssock[0].close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(SOCKET_TAG, e.toString());
				return "";
			}
			return null;
		}
		
        @Override  
        protected void onProgressUpdate(String... msg) {
        	updateListView(msg[0], msg[1]);
        	Log.i(SOCKET_TAG, "Set text.");
        }  
        
        @Override  
        protected void onPostExecute(String result) {
        	if(result != null) {
        		btnSend.setEnabled(false);
    			serverRunning = false;
        	}
        }
	}
	
	//display message on listview
	private void updateListView(String who, String content){
		Map<String, String> map = new HashMap<String, String>();
		map.put(MESSAGE_TALKER, who);
		map.put(MESSAGE_CONTENT, content);
		chatList.add(map);
		
//		SimpleAdapter adt = new SimpleAdapter(this, chatList, R.layout.chat_left,
//				new String[]{"talker","content"}, new int[]{R.id.talker,R.id.content});
		ChatAdapter adt = new ChatAdapter(this, chatList);
		lstView.setAdapter(adt);
		lstView.setSelection(chatList.size()-1);
	}
	
	
	//for display message separately at left and right sides, employing different layout
	private class ChatAdapter extends BaseAdapter {
		private Context context;
		private List<Map<String, String>> list;
		
		public ChatAdapter(Context context, List<Map<String, String>> list) {
			this.context = context;
			this.list = list;
		}
		
		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int arg0) {
			return list.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(this.context);
			View view = null;
			Map<String, String> showMsg = list.get(position);
			if(showMsg.get(MESSAGE_TALKER).equals("me")) {
				view = inflater.inflate(R.layout.chat_right, null);
			}
			else {
				view = inflater.inflate(R.layout.chat_left, null);
			}
			
			TextView txvTalker = (TextView) view.findViewById(R.id.talker);  
			txvTalker.setText(showMsg.get(MESSAGE_TALKER));
			
			TextView txvContent = (TextView) view.findViewById(R.id.content);  
			txvContent.setText(showMsg.get(MESSAGE_CONTENT));  
			
			return view;
		}
		
	}
	
}

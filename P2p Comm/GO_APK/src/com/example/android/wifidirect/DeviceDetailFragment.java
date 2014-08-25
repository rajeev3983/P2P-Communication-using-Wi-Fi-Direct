

package com.example.android.wifidirect;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.Enumeration;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.WifiManager.MulticastLock;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

/**
 * A fragment that manages a all peers and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener, GroupInfoListener {

	
	public static int PORT = 8988;
	public static int PORT2 = 7000;
	private static boolean server=false;
	private static boolean service=false;
	private static final String MESSAGE1="FirSt";
	private static final int MAX_CLIENTS=10;
	private static final clientThr[] threads=new clientThr[MAX_CLIENTS];
	protected static final int CHOOSE_FILE_RESULT_CODE = 20;
	private View mContentView = null;
	private View mView2 = null;
	private WifiP2pDevice device;
	private WifiP2pInfo info;
	private  WifiP2pGroup grp;
	private static 	String sss=" ";
	private Thread t1;
	private fileReceiver t2;
    private chatSender CHs;
    private chatReceiver CHr;
	ProgressDialog progressDialog = null;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       ;
		mContentView = inflater.inflate(R.layout.device_detail, container,false);
		mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				WifiP2pConfig config = new WifiP2pConfig();
				config.deviceAddress = device.deviceAddress;
			
				config.wps.setup = WpsInfo.PBC;
				
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
						"Connecting to :" + device.deviceAddress, true, true
						//                        new DialogInterface.OnCancelListener() {
						//
						//                            @Override
						//                            public void onCancel(DialogInterface dialog) {
						//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
						//                            }
						//                        }
				);
				((DeviceActionListener) getActivity()).connect(config);

			}
		});

		mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						((DeviceActionListener) getActivity()).disconnect();
					}
				});

		mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// Allow user to pick any file from file manager or other
						// registered apps
						try{
						Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
						intent.setType("*/*");
						startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);}
						catch(Exception e){
							
						}
					}
				});
		///
		/*
		 * starts the file receiver on this GO device so that any file sent by a peer shall be received by it
		 *                                                                                       -Anand
		 */
		mContentView.findViewById(R.id.btn_receiver).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				mContentView.findViewById(R.id.btn_receiver).setVisibility(View.GONE);
				mContentView.findViewById(R.id.btn_receiver_stop).setVisibility(View.VISIBLE);
				t2=new fileReceiver();
				t1=new Thread(t2);
				t1.start();
			}
		});
mContentView.findViewById(R.id.btn_receiver_stop).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mContentView.findViewById(R.id.btn_receiver_stop).setVisibility(View.GONE);
				mContentView.findViewById(R.id.btn_receiver).setVisibility(View.VISIBLE);
				try {
					t2.e1=99;
					//t1.stop();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
/*
 * Button to start Thread to test if broadcast of a UDp packet is possible to all connected 
 * clients .. But it did not work
 *                                                                               -Anand
 */
mContentView.findViewById(R.id.btn_start_broadcast).setOnClickListener(new View.OnClickListener() {
	
	@Override
	public void onClick(View v) {
		mContentView.findViewById(R.id.btn_start_broadcast).setVisibility(View.GONE);
		new Thread(new packetSender()).start();
		
	}
});
/*
 * Start Chat server and Chat receiver thread will accept only one conenction at a time
 * in order to chat to another client close this server by sending 'bye' 
 * and then start again
 * 
 */
mContentView.findViewById(R.id.btn_start_chat).setOnClickListener(new View.OnClickListener() {
	
	@Override
	public void onClick(View v) {
		
		
		mContentView.findViewById(R.id.btn_start_chat).setVisibility(View.GONE);
		mContentView.findViewById(R.id.send_chat).setVisibility(View.VISIBLE);
	
		CHs=null;CHr=null;
		CHs=new chatSender(info.groupOwnerAddress.getHostAddress());
		new Thread(CHs).start();
		
		CHr=new chatReceiver();
		new Thread(CHr).start();
		
	}
});
	/*
	 * Send message to peer
	 */
mContentView.findViewById(R.id.send_chat).setOnClickListener(new View.OnClickListener() {
	
	@Override
	public void onClick(View v) {
		
	EditText ett=(EditText)	mContentView.findViewById(R.id.editText1);
	String sch=ett.getText().toString();
	ett.setText("");
	synchronized(CHs)
	{CHs.stats1=true;
	CHs.send_text=sch;
	CHs.notify();}


		//synchronize with chat sender
	}
});


		return mContentView;
	}
	

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		  if(requestCode==CHOOSE_FILE_RESULT_CODE)
		  {

		   
		// User has picked a filee. Transfer it to all other peers using file sender
		
		Uri uri = data.getData();
		TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
		statusText.setText("Sending: " + uri);
		Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
		new Thread(new fileSender(uri,threads)).start();
		}
	}

	/*
	 * call back from broadcast receiver when first connection formed .. 
	 * also called whenever any data sent/received in the streams of sockets
	 * so boolean checks used to start threads when required
	 *                                                             -Anand
	 * (non-Javadoc)
	 * @see android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener#onConnectionInfoAvailable(android.net.wifi.p2p.WifiP2pInfo)
	 */
	
	@Override
	public void onConnectionInfoAvailable(final WifiP2pInfo info) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		this.info = info;
		

		

		
      if((info.groupFormed)&&(info.isGroupOwner)&&(service==false))
       {
    	  
    	  this.getView().setVisibility(View.VISIBLE);
    		
  		// The owner IP is now known.
  		TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
  		view.setText(getResources().getString(R.string.group_owner_text)
  				+ ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
  						: getResources().getString(R.string.no)));

  		// InetAddress from WifiP2pInfo struct.
  		view = (TextView) mContentView.findViewById(R.id.device_info);
  		view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());
    	if((server==false))
       {
    		new Thread(new serverq()).start();// starts thread that keeps listening for new socket connections 
      Log.d(WiFiDirectActivity.TAG,"server thread started");
      server=true;
      }
    	
      mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
		
      mContentView.findViewById(R.id.btn_receiver).setVisibility(View.VISIBLE);
      mContentView.findViewById(R.id.btn_start_broadcast).setVisibility(View.VISIBLE);
      mContentView.findViewById(R.id.btn_start_chat).setVisibility(View.VISIBLE);
	
		// hide the connect button
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
     service=true;
       }
      else{
    	  if(server==true)
    	  {Log.d(WiFiDirectActivity.TAG,"info called again means every time a device sends anything file/text any data recieved in streams of socket");}
      }
      
	}
	/*
	 * Class to test broadcast of Udp packets by sending them to broadcast address
	 * (Did not work)
	 *                                                               -Anand
	 *                                                               
	 */
	public class packetSender implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			WifiManager wifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
			MulticastLock lock = wifi.createMulticastLock("dk.aboaya.pingpong");
			lock.acquire();
			DatagramSocket socket;
			try {
				socket = new DatagramSocket(5555);
			
			String data="Hello I was broadcasted";
			socket.setBroadcast(true);
			Log.d(WiFiDirectActivity.TAG,"Broadcast address is "+((WiFiDirectActivity) getActivity()).getBroadcastAddress().getHostAddress());
			
			DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),
					InetAddress.getByName("192.168.49.255"), 5555);// unsure of the broadcast address -change if necessary
			int pq=30;
			while(pq!=0)
			{
				socket.send(packet);
			
			pq--;
			}
			
			lock.release();
			alterui(data);
			//socket.close();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		public void alterui(String gg){
			final String pp=gg;
			// in order to change uI from a different thread such functions required
			getActivity().runOnUiThread(new Runnable() {
	            public void run() {
	               TextView tt= (TextView) mContentView.findViewById(R.id.status_text);
	          tt.setText("Broadcast sent ->"+pp);
	          try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	          tt.setText("");
	          mContentView.findViewById(R.id.btn_start_broadcast).setVisibility(View.VISIBLE);
	               
	            }
	        });
		}
		
	}
	
	
	/*
	 *  A Thread than continuously listens to a server socket running on the Go
	 *  waiting for client file receivers to connect and when a client connect a
	 *  separate thread is started to handle that connection (clientThr object ) and is referenced by
	 *  a threads[] array so that other functions can also use this existing socket connection
	 *                                                                           -Anand 
	 */

	public class serverq implements Runnable{

		
		
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					ServerSocket serverSocket = new ServerSocket(PORT);
					Log.d(WiFiDirectActivity.TAG, "Server: Socket  opened");
				
					// File f;
					while(true)
					{
						Socket client = serverSocket.accept();
						Log.d(WiFiDirectActivity.TAG, "Server: connection done to"+client.getInetAddress().getHostAddress()+"max clients"+MAX_CLIENTS);
						
						int i=0;
						for(i=0;i<MAX_CLIENTS;i++)
						{
							if(threads[i]==null)
							{
								threads[i]=new  clientThr(client,threads);
								threads[i].start();
								
								Log.d(WiFiDirectActivity.TAG,"child "+i+" thread started");
								break;
							}
													
						}
						
						if(i==MAX_CLIENTS)
						{
							Log.d(WiFiDirectActivity.TAG,"Server client limit reached");
							break;
						}
				
						
			}
					serverSocket.close();
					server=false;
				}
				catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		
		
	}
	
	public void showDetails(WifiP2pDevice device) {
		this.device = device;
		
		this.getView().setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView.findViewById(R.id.device_address);
		view.setText(device.deviceAddress);
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(device.toString());

	}

	/**
	 * Clears the UI fields after a disconnect or direct mode disable operation.
	 * @throws InterruptedException 
	 */
	public void resetViews() throws InterruptedException {
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView.findViewById(R.id.device_address);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.status_text);
		view.setText(R.string.empty);
		service=false;
		for(int i=0;i<MAX_CLIENTS;i++){
			if(threads[i]!=null){
				//threads[i].wait();
				threads[i].e3=99;
				threads[i]=null;
			}
		}
		 mContentView.findViewById(R.id.btn_start_broadcast).setVisibility(View.GONE);
	      mContentView.findViewById(R.id.btn_start_chat).setVisibility(View.GONE);
		mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
		  mContentView.findViewById(R.id.btn_receiver).setVisibility(View.GONE);
			mContentView.findViewById(R.id.btn_receiver_stop).setVisibility(View.GONE);
		this.getView().setVisibility(View.GONE);
	}
	
	@Override
	public void onGroupInfoAvailable(WifiP2pGroup group) {
		
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		this.grp= group;
		
		Toast.makeText(getActivity(), "Group formed ssid->"+grp.getNetworkName()+"\n passphrase "+grp.getPassphrase(), Toast.LENGTH_LONG).show();
		
		
		
	}
	
	
/*
 * An object of this class is passed to each thread that handles a file connection
 * to any one client .
 * this thread waits till GO selects a file and when GO has selected a file one of 
 * it's child thread is informed which sends the file to the client it is responsible for
 *                                                                            
 * 
 */
	
	

	public  class clientThr  extends Thread{

		private String message = null;
		
		  private Socket client = null;
		  private  clientThr[] threads;
		  private int maxClientsCount;
		  private boolean stat=false;
		  private Uri file_uri;
		  private int e3=0;
		
		public clientThr(Socket client, clientThr[] threads1) {
		
			this.client = client;
		    this.threads = threads1;

		    this.maxClientsCount = threads1.length;
		  	
		 
		}
		
		 
		@Override
		public void run() {
		
			String line="";
			try {
				InputStream inputstream = client.getInputStream();
				BufferedReader bro=new BufferedReader(new InputStreamReader(inputstream));
				line=bro.readLine();
				
			
			String r1="Connected "+line+"  port -"+client.getPort();
			
			synchronized (sss) {
				sss="";
				sss=sss+"\n"+r1;
					}
					setext();
							
					Context context=getActivity().getApplicationContext();
					ContentResolver cr=context.getContentResolver();
					while(true){
						synchronized(this)
						{
							while(!(this.stat)){
								this.wait();
						}
							this.stat=false;
						}
						String str="";
						int ind=((file_uri.toString()).lastIndexOf('/'))+1;
						str=(file_uri.toString()).substring(ind, (file_uri.toString()).length());
						
						Log.d(WiFiDirectActivity.TAG,"got file name"+str);
						InputStream is=cr.openInputStream(file_uri);
						
						OutputStream os=this.client.getOutputStream();
						long size=findsize(is);
						is=cr.openInputStream(file_uri);
						DataInputStream dis1=new DataInputStream(new BufferedInputStream(is));
						DataOutputStream dos= new DataOutputStream(os);
						dos.writeUTF("sending");
						dos.writeUTF(str);
						dos.writeLong(size);
						if(copyFile(dis1,dos))
						{Log.d(WiFiDirectActivity.TAG, "file content written to output stream");
						changeUi("File sent to "+this.client.getInetAddress().getHostAddress());
						}
						else{Log.d(WiFiDirectActivity.TAG, "error in copying file content");}
						
						if(e3!=0){
							
							synchronized(threads){
							for(int i=0;i<maxClientsCount;i++)
							{if((threads[i]!=null)&&(threads[i]==this)){
								threads[i]=null;
								break;
							}
								
							}
							}
							break;
						}
												
					}
			
			
					bro.close();
					//ois.close();
					client.close();
					
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}
		// function to find the size of the selected file (did not know any direct function)
		public long findsize(InputStream in){
			byte buf[] = new byte[1024];
			int len;
			long size=0;
			try {
				while ((len = in.read(buf)) != -1) {
			
					size=size+len;
					
				}
				in.close();
			
		}catch (IOException e) {
			Log.d(WiFiDirectActivity.TAG, e.toString());
			return -1;
		}
			return size;
		}
		// copy file bytes from one stream to another
		public  boolean copyFile(DataInputStream inputStream, DataOutputStream out) {
			byte buf[] = new byte[1024];
			int len;
			long size=0;
			try {
				while ((len = inputStream.read(buf)) != -1) {
					out.write(buf, 0, len);
					size=size+len;
					out.flush(); 
				}
				
				
				
				Log.d(WiFiDirectActivity.TAG, "size written "+size);
			} catch (IOException e) {
				Log.d(WiFiDirectActivity.TAG, e.toString());
				return false;
			}
			return true;
		}
		
		public void changeUi(String s){
			final String gg=s;
			getActivity().runOnUiThread(new Runnable() {
	            public void run() {
	            	TextView tt4= (TextView) mContentView.findViewById(R.id.status_text);
	            
	          tt4.setText(gg);
	          
	          /*try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
		        //  tt4.setText("");
            	          	              
	            }
	        });
		}
		public void setext()
		
		{
			getActivity().runOnUiThread(new Runnable() {
	            public void run() {
	               TextView tt= (TextView) mContentView.findViewById(R.id.status_text);
	               synchronized (sss) {
	            	   tt.setText(sss);
				}
	               
	            }
	        });
			
		}
	}
	/*
	 * This thread is started every time a file is selected by the GO and it informs the server threads 
	 * responsible for client communication that data is available to be sent
	 *                                                                     -Anand
	 */
	public class fileSender implements Runnable{

		private Uri url;
		private  clientThr[] threads;
		private int maxClients;
		 
		public fileSender(Uri qri,clientThr[] thr)
		{
			this.url=qri;
			this.threads=thr;
			this.maxClients=thr.length;
			
		}
		@Override
		public void run() {
					
			int i;
			for(i=0;i<maxClients;i++)
			{
				if(threads[i]!=null){
				 
					Log.d(WiFiDirectActivity.TAG,i+ "socket "+threads[i].client.isConnected());
					synchronized(threads[i])
					{threads[i].stat=true;
					threads[i].file_uri=url;
					threads[i].notifyAll();}
				
				}
			}
			
		
		
	}
	}
	/*
	 * File Receiver thread started on button click
	 * the Server shall receive files only if this thread is running
	 *   While sending or receiving any file first filename is sent 
	 *   then its size and then the contents of the file are sent
	 *   So when receiving first two fields are read and the file contents are written
	 *                                                                 -Anand                                             
	 */
	public class fileReceiver implements Runnable{
		public int e1=0;
		@Override
		public void run() {
			try{
			ServerSocket receiverSocket = new ServerSocket(PORT2);
			Log.d(WiFiDirectActivity.TAG,"second receiver server started");
			Context context=getActivity().getApplicationContext();
			Socket xy=null;
			while(true){
				try{
			 xy=receiverSocket.accept();}
				catch(IOException e){
					e.printStackTrace();
					break;
				}
			Log.d(WiFiDirectActivity.TAG,"client accepted");
			DataInputStream dis=new DataInputStream((xy.getInputStream()));
        	String stats=dis.readUTF();
        	if((stats!=null)&&(stats.equals("sending"))){
        		
        		
        	String name=dis.readUTF();
        	long size=dis.readLong();
        	int bytesRead;
        	byte[] buffer = new byte[1024];
        	
        	 File f = new File(Environment.getExternalStorageDirectory() + "/"
						+ context.getPackageName() + "/p2preceived-MINE-"+System.currentTimeMillis()+name);

				File dirs = new File(f.getParent());
				if (!dirs.exists())
					dirs.mkdirs();
				f.createNewFile();
				OutputStream output=new FileOutputStream(f);
				DataOutputStream dos=new DataOutputStream(output);
        	 while (size > 0 && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1)     
             {     
                 dos.write(buffer, 0, bytesRead);     
                 size -= bytesRead;     
             }  
        	 if(size!=0)
         {Log.d(WiFiDirectActivity.TAG, "size left "+size);}
       
        	 else{
		
		//DeviceDetailFragment.copyFile(is, new FileOutputStream(f));
		Log.d(WiFiDirectActivity.TAG, "Client : file received");
		if((name.contains(".jpg"))||(name.contains(".gif"))||(name.contains(".png"))||(name.contains(".jpeg"))){
			
			  Intent in=new Intent();
			  in.setAction(android.content.Intent.ACTION_VIEW);
			  in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			  in.setDataAndType(Uri.parse("file://"+f.getAbsolutePath()), "image/*");
			  context.startActivity(in);
				}
		String tmp="File received";
		changeUi(tmp);
        	 }
		
          	
        }
        	else{
        		if(e1!=0)
            	{break;}
        	}
        	xy.close();
			}
		}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		public void changeUi(String s){
			final String gg=s;
			getActivity().runOnUiThread(new Runnable() {
	            public void run() {
	            	TextView tt4= (TextView) mContentView.findViewById(R.id.status_text);
	            
	          tt4.setText(gg);
	          
	         /* try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		          tt4.setText("");*/
            	          	              
	            }
	        });
		}
	}
/*
 * Class to handle thread dealing with chat sender and receiver
 * the sender accepts only one connection at a time (it can be altered to accept 
 * multiple connections if required) the sender and receiver communicate 
 * with each other to inform if exit word "bye" is received if so both threads close
 *                                                                    -Anand
 */

	
	public class chatSender implements Runnable{
		 protected String send_text;
		private boolean stats1=false;
		private String Go_Addr;
		private String ca;
		private boolean signal=false;
		public chatSender(String q){
			
			this.Go_Addr=q;
		}
			@Override
			public void run() {
				String stp="";
				OutputStream os;
				DataOutputStream dos=null;
				
				Socket chatter=null;
				ServerSocket cht=null;
				int llp=0;
				try {
					 cht=new ServerSocket(7577);
					chatter=cht.accept();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					llp=99;
				}
				
				if(llp!=99){
					synchronized(CHr){
						CHr.chatter=chatter;
						CHr.notify();
						
					}
					
					try {
						os = chatter.getOutputStream();
						dos=new DataOutputStream(os);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					ca=chatter.getInetAddress().getHostAddress();
					chatstart();
					
				}
				while(true){
					
					if(llp!=99){
					
						if(CHs.signal){
							stp="Disconnecting as client wants to disconnect";
							break;
						}
						synchronized(this)
						{
						while(!(this.stats1)){
							try {
								this.wait();
							} catch (InterruptedException e) {
								
								e.printStackTrace();
							}
					}
						this.stats1=false;
					}//synch
					
					
					try {
						
							dos.writeUTF(send_text);
							if(!(send_text.isEmpty()))
							{
							
							showtext("me ->"+send_text);
						}
						if(send_text.equals("bye")){
							stp="disconnecting";
							break;
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (NullPointerException e){
						Log.d(WiFiDirectActivity.TAG,"How the hell did this happen");
					}
					
					
					}
					else{
						// inform user that server didn't accept wasnt in chat mode
						stp="connection couldn't be established IOException";
						break;}
					
				}//while
				
				
				
					CHr.signal=true;
				
				try {
					cht.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//alter ui to show chat button again in order to start new session
				Log.d(WiFiDirectActivity.TAG,"Exiting chat sender");
				Aui(stp);
			}
			
			public void Aui(String s){
				final String pp=s;
				getActivity().runOnUiThread(new Runnable() {
		            public void run() {
		               TextView tt= (TextView) mContentView.findViewById(R.id.status_text);
		          tt.setText(pp);
		       /*   try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		          tt.setText("");*/
		          mContentView.findViewById(R.id.btn_start_chat).setVisibility(View.VISIBLE);
		          mContentView.findViewById(R.id.device_info).setVisibility(View.VISIBLE);	
		          mContentView.findViewById(R.id.group_owner).setVisibility(View.VISIBLE);
		          mContentView.findViewById(R.id.send_chat).setVisibility(View.GONE);  
		      	TextView tt4= (TextView) mContentView.findViewById(R.id.chat_text4);
		          TextView tt2= (TextView) mContentView.findViewById(R.id.chat_text3);
	            	TextView tt1= (TextView) mContentView.findViewById(R.id.chat_text2);
	               TextView tt3= (TextView) mContentView.findViewById(R.id.chat_text);
	               tt1.setText("");tt2.setText("");tt3.setText("");tt4.setText("");
		            }
		        });
			}
			
			public void showtext(String p){
				final String pp=p;
				getActivity().runOnUiThread(new Runnable() {
		            public void run() {
		            	
		            	
		            	TextView tt2= (TextView) mContentView.findViewById(R.id.chat_text3);
		            	TextView tt1= (TextView) mContentView.findViewById(R.id.chat_text2);
		               TextView tt= (TextView) mContentView.findViewById(R.id.chat_text);
		               
		           
		               tt2.setText(tt1.getText().toString());
		          tt1.setText(tt.getText().toString());
		               tt.setText(pp);
		          
		         
		              
		            }
		        });
			}
			public void chatstart(){
				
				getActivity().runOnUiThread(new Runnable() {
		            public void run() {
		            	mContentView.findViewById(R.id.device_info).setVisibility(View.GONE);	 
          	          mContentView.findViewById(R.id.group_owner).setVisibility(View.GONE);
		            	TextView tt4= (TextView) mContentView.findViewById(R.id.chat_text4);
		                    tt4.setText("Chat Server is running connected to "+ca+".. to exit type \'bye\'");
                	          
		            }
		        });
			}
			  
		  }
		  
		  public class chatReceiver implements Runnable{
		  private Socket chatter;
		  private boolean signal=false;
			@Override
			public void run() {
				
		
					
				
					synchronized(this)
					{
						
						while((this.chatter)==null){
							try {
								this.wait();
							} catch (InterruptedException e) {
								
								e.printStackTrace();
							}
					}
						
					}//synch
					
					try {
					
						DataInputStream dis;
					    dis = new DataInputStream((chatter.getInputStream()));
					
					    while(true){
					    	
								if(CHr.signal){
									break;
								}
							
					    	
		        	String chattex=dis.readUTF();
		        	if((chattex!=null)&&(!(chattex.isEmpty()))){
		        	 showtext(chattex.trim());
		        	}
		        	
		        		if((chattex.trim()).equals("bye")){
		        			break;
		        		}
		       	}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.d(WiFiDirectActivity.TAG,"input stream of chat socket exception");
					}
			   
					
					synchronized(CHs){
						CHs.signal=true;
						CHs.stats1=true;
						CHs.send_text="";
					CHs.notify();
					}
					
					Log.d(WiFiDirectActivity.TAG,"Exiting chat receiver");	
			}
			
			public void showtext(String p){
				final String pp=p;
				getActivity().runOnUiThread(new Runnable() {
		            public void run() {
		            	TextView tt2= (TextView) mContentView.findViewById(R.id.chat_text3);
		            	TextView tt1= (TextView) mContentView.findViewById(R.id.chat_text2);
		               TextView tt= (TextView) mContentView.findViewById(R.id.chat_text);
		               tt2.setText(tt1.getText().toString());
		          tt1.setText(tt.getText());
		               tt.setText("Client says->"+pp);
		          
		          //tt.setText("");
		              
		            }
		        });
			}
			  
		  }
	

}

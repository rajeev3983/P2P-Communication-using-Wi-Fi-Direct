

package com.example.android.wifidirect;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.net.wifi.WpsInfo;
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
import android.widget.EditText;
import android.widget.TextView;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    private Thread t1;
    private static boolean enc=false;
    private chatSender CHs;
    private chatReceiver CHr;
    ProgressDialog progressDialog = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                TextView vv=(TextView)mContentView.findViewById(R.id.status_text);
               vv.setText(device.deviceAddress);
                config.wps.setup = WpsInfo.PBC;
               // chose push button connect method to establish wifi direct connection with peer
              
                
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
                        /*connection established with GO i.e a socket connection made
                         start a thread on this client which will keep on listening to the socket 
                         to receive any file the Go might send
                         also make the button to send file to group owner visible
                         * 
                         */
                    	 mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
                    	 mContentView.findViewById(R.id.btn_start_sender).setVisibility(View.VISIBLE);
                    	 String lip=getDottedDecimalIP(getLocalIPAddress());
                    	 
                    	 new Thread(new fileReceiver(lip,info.groupOwnerAddress.getHostAddress(),8988)).start();
                    	 
                    	
                        
                         
                    }
                });
        
        mContentView.findViewById(R.id.btn_start_sender).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				 
				 Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                 intent.setType("*/*");
                 startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
				 
				
			}
		});

  mContentView.findViewById(R.id.btn_receive_broadcast).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mContentView.findViewById(R.id.btn_receive_broadcast).setVisibility(View.GONE);
				new Thread(new packetReceiver()).start();
			}
		});
  /*
   * Start Chat client(socket connection to server) also threads to handle
   *  sending and receiving threads of chat messages  
   * 
   */
  mContentView.findViewById(R.id.btn_start_chat).setOnClickListener(new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			
			mContentView.findViewById(R.id.btn_start_chat).setVisibility(View.GONE);
			mContentView.findViewById(R.id.send_chat).setVisibility(View.VISIBLE);
			
			CHs=new chatSender(info.groupOwnerAddress.getHostAddress());
			new Thread(CHs).start();
			
			CHr=new chatReceiver();
			new Thread(CHr).start();
			
		}
	});
  mContentView.findViewById(R.id.send_chat).setOnClickListener(new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
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
/*
 * called to inform that user has selected a file to send to GO
 * so inform the sender thread of presence of valid data
 * (non-Javadoc)
 * @see android.app.Fragment#onActivityResult(int, int, android.content.Intent)
 */
   @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using new thread
       
	   if(requestCode==CHOOSE_FILE_RESULT_CODE)
       {
      
    	   Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending file...");
        Log.d(WiFiDirectActivity.TAG, "Intent-----------  dta sent MAN" );
      t1=new Thread (new fileSender(uri,info.groupOwnerAddress.getHostAddress()));
      t1.start();
      }
    }
/*
 * callback from broadcast receiver when conenction formed
 * also whenever
 * (non-Javadoc)
 * @see android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener#onConnectionInfoAvailable(android.net.wifi.p2p.WifiP2pInfo)
 */
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        

      
        if (info.groupFormed && info.isGroupOwner) {
           
        } else if ((info.groupFormed)&&(enc==false)) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
        	this.getView().setVisibility(View.VISIBLE);

            // The owner IP is now known.
            TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
            view.setText(getResources().getString(R.string.group_owner_text)
                    + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                            : getResources().getString(R.string.no)));

            // InetAddress from WifiP2pInfo struct.
            view = (TextView) mContentView.findViewById(R.id.device_info);
            view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());
            mContentView.findViewById(R.id.btn_receive_broadcast).setVisibility(View.VISIBLE);
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            mContentView.findViewById(R.id.btn_start_sender).setVisibility(View.GONE);
           
            mContentView.findViewById(R.id.btn_start_chat).setVisibility(View.VISIBLE);
           enc=true;
        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    /**
     * Updates the UI with device data
     * 
     * @param device the device to be displayed
     */
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
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        enc=false;
        mContentView.findViewById(R.id.btn_receive_broadcast).setVisibility(View.GONE);
        mContentView.findViewById(R.id.btn_start_chat).setVisibility(View.GONE);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        mContentView.findViewById(R.id.btn_start_sender).setVisibility(View.GONE);
      
        this.getView().setVisibility(View.GONE);
    }
    /*
     * Functions to get Ip of this device from the network interfaces class
     */
    private byte[] getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        if (inetAddress instanceof Inet4Address) {
                            return inetAddress.getAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            // Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        } catch (NullPointerException ex) {
            // Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        }
        return null;
    }

    private String getDottedDecimalIP(byte[] ipAddr) {
        if (ipAddr != null) {
            String ipAddrStr = "";
            for (int i = 0; i < ipAddr.length; i++) {
                if (i > 0) {
                    ipAddrStr += ".";
                }
                ipAddrStr += ipAddr[i] & 0xFF;
            }
            return ipAddrStr;
        } else {
            return "null";
        }
    }
    //
    /*
     *This is the file receiver thread of client
     *it first gets the ip of server from the starting activity and then it forms
     *a socket connection with the server with a message(handshake) sending it's ip
     *server can store it for future use .
     *It then keeps on reading from the socket stream waiting for key text indicating 
     *start of a file content it stores it then again keeps listening to the socket
     *                                                               -Anand  
     */
    public class fileReceiver implements Runnable{

    	String data="";
    	String Address="";
    	int por;
    	private static final int SOCKET_TIMEOUT = 5000;
		public fileReceiver(String dat,String Add,int pop){
			this.data=dat;
			this.Address=Add;
			this.por=pop;
		}
    	@Override
		public void run() {
			// TODO Auto-generated method stub
			   Context context = getActivity().getApplicationContext();
		       
		           String host=Address;
		            Socket socket = new Socket();
		            int port = por;

		            try {
		                Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
		                socket.bind(null);
		                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
		                int e1=0;
		                Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
		                BufferedWriter wr=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		                wr.write(data);
		                wr.newLine();
		                Log.d(WiFiDirectActivity.TAG, "Client: Data sent");
		           
		                wr.flush();
		                //wr.close();
		               
		                
		                
		                while(true)
		                {
		                	DataInputStream dis=new DataInputStream((socket.getInputStream()));
		                	String stats=dis.readUTF();
		                	if((stats!=null)&&(stats.equals("sending"))){
		                		
		                		
		                	String name=dis.readUTF();
		                	long size=dis.readLong();
		                	int bytesRead;
		                	byte[] buffer = new byte[1024];
		                	
		                	 File f = new File(Environment.getExternalStorageDirectory() + "/"
		     						+ context.getPackageName() + "/p2pshared-MINE-"+System.currentTimeMillis()+name);

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
						
						
						Log.d(WiFiDirectActivity.TAG, "Client : file received");
						
						//start viewer only if received file is an image
						
						if((name.contains(".jpg"))||(name.contains(".gif"))||(name.contains(".png"))||(name.contains(".jpeg"))){
						
					  Intent in=new Intent();
					  in.setAction(android.content.Intent.ACTION_VIEW);
					  in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					  in.setDataAndType(Uri.parse("file://"+f.getAbsolutePath()), "image/*");
					  context.startActivity(in);
						}
						else{
							alUi("file received  "+name);
						}
						
						}
						
		                  	
		                }
		                	else{
		                		if(e1!=0)
		                    	{break;}
		                	}
		                }
		               
		            } catch (IOException e) {
		                Log.e(WiFiDirectActivity.TAG, e.getMessage());
		            } finally {
		                if (socket != null) {
		                    if (socket.isConnected()) {
		                        try {
		                            socket.close();
		                        } catch (IOException e) {
		                            // Give up
		                            e.printStackTrace();
		                        }
		                    }
		                }
		            }

		        
			
			
		}
    	
    	public void alUi(String s){
    		final String pp=s;
			getActivity().runOnUiThread(new Runnable() {
	            public void run() {
	               TextView tt= (TextView) mContentView.findViewById(R.id.status_text);
	          tt.setText(pp);
	         /* try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	          tt.setText("");*/
	         // mContentView.findViewById(R.id.btn_start_broadcast).setVisibility(View.VISIBLE);
	               
	            }
	        });
    	}
    	
    }
    /*
     * Packet receiver to receive Udp broadcasted packets 
     * (does not work)
     *                                                 -Anand
     */
    public class packetReceiver implements Runnable{

		@Override
		public void run() {
			WifiManager wifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
			MulticastLock lock = wifi.createMulticastLock("dk.aboaya.pingpong");
			String s="nothing received";
			lock.acquire();
			Log.d(WiFiDirectActivity.TAG,"LOCK ACQUIRED");
			DatagramSocket serverSocket=null;
			try {
				serverSocket = new DatagramSocket(5555);
				Log.d(WiFiDirectActivity.TAG,"Datagram socket connected");
			serverSocket.setSoTimeout(15000); //15 sec wait for the client to connect
			byte[] data = new byte[256]; 
			DatagramPacket packet = new DatagramPacket(data, data.length);
			while (true)
			{serverSocket.receive(packet);
			 s = new String(packet.getData());
			 if(s!=null)
			break;
			}
			lock.release();
			
			Log.d(WiFiDirectActivity.TAG,"GOT data "+s);
			//System.out.println(s);
			alterui(s.trim());
			serverSocket.close();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
				alterui(s.trim());
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				alterui(s.trim());
				
			}
			if((serverSocket!=null)&&(serverSocket.isConnected())){
				serverSocket.close();
			}
		}
    	
		public void alterui(String gg){
			final String pp=gg;
			getActivity().runOnUiThread(new Runnable() {
	            public void run() {
	               TextView tt= (TextView) mContentView.findViewById(R.id.status_text);
	          tt.setText("Broadcast received ->"+pp);
	          try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	          tt.setText("");
	          mContentView.findViewById(R.id.btn_receive_broadcast).setVisibility(View.VISIBLE);
	               
	            }
	        });
		}
    }
/*
 * first forms a connection with the file receiver thread 
 * (a different server socket of Go and then sends the file 
 * via streams and then breaks the socket .. 
 * it does not retain it like in case of sender of GO
 *                                                            
 */
  public class fileSender implements Runnable{
	  private Uri ur;
	  private String add="";
	  private final int port=7000;
	  public fileSender(Uri qri,String Adr)
		{
			this.ur=qri;
			this.add=Adr;
			
		
			
		}
	  

	@Override
	public void run() {
		try{
		Socket client=new Socket();
		client.connect((new InetSocketAddress(add, port)), 10000);
		Context context=getActivity().getApplicationContext();
		ContentResolver cr=context.getContentResolver();
		String str="";
		int ind=((ur.toString()).lastIndexOf('/'))+1;
		str=(ur.toString()).substring(ind, (ur.toString()).length());
		
		Log.d(WiFiDirectActivity.TAG,"got file name"+str);
		InputStream is=cr.openInputStream(ur);
		
		OutputStream os=client.getOutputStream();
		long size=findsize(is);
		is=cr.openInputStream(ur);
		DataInputStream dis=new DataInputStream(new BufferedInputStream(is));
		DataOutputStream dos= new DataOutputStream(os);
		dos.writeUTF("sending");
		dos.writeUTF(str);
		dos.writeLong(size);
		if(copyFile(dis,dos))
		{Log.d(WiFiDirectActivity.TAG, "file content written to output stream");}
		else{Log.d(WiFiDirectActivity.TAG, "error in copying file content");}	
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
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
	  
  }//fileSender class
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
private boolean signal=false;
public chatSender(String q){
	
	this.Go_Addr=q;
}
	@Override
	public void run() {
		String stp="";
		OutputStream os;
		DataOutputStream dos=null;
		Socket chatter=new Socket();
		int llp=0;
		try {
			chatter.connect(new InetSocketAddress(Go_Addr,7577),10000);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			llp=99;
		}
		
		if(llp!=99){
			
			try {
				os = chatter.getOutputStream();
				dos=new DataOutputStream(os);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			synchronized(CHr){
				CHr.chatter=chatter;
				CHr.notify();
				
			}
			
			
		}
		while(true){
			
			if(llp!=99){
			
				if(CHs.signal){
					stp="disconnecting as Grp owner wants to disconnect";
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
				showtext("me ->"+send_text);}
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
				stp="chat connection could not be established server wasn't running or busy";
				break;}
			
		}//while
		
		
		//alter ui to show chat button again in order to start new session
		
			CHr.signal=true;
			try {
				chatter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.d(WiFiDirectActivity.TAG,"Exiting chat sender");	
		Aui(stp);
	}
	
	public void Aui(String s){
		final String pp=s;
		getActivity().runOnUiThread(new Runnable() {
            public void run() {
               TextView tt= (TextView) mContentView.findViewById(R.id.status_text);
          tt.setText(pp);
         /* try {
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
        //  TextView tt2= (TextView) mContentView.findViewById(R.id.chat_text3);
        	TextView tt1= (TextView) mContentView.findViewById(R.id.chat_text2);
           TextView tt3= (TextView) mContentView.findViewById(R.id.chat_text);
           tt1.setText("");//tt2.setText("");
           tt3.setText("");
          
            }
        });
	}
	
	public void showtext(String p){
		final String pp=p;
		getActivity().runOnUiThread(new Runnable() {
            public void run() {
            	TextView tt1= (TextView) mContentView.findViewById(R.id.chat_text2);
               TextView tt= (TextView) mContentView.findViewById(R.id.chat_text);
          tt1.setText(tt.getText().toString());
               tt.setText(pp);
          
          //tt.setText("");
              
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
        	
        		if(chattex.equals("bye")){
        			break;
        		}
}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
            	TextView tt1= (TextView) mContentView.findViewById(R.id.chat_text2);
               TextView tt= (TextView) mContentView.findViewById(R.id.chat_text);
          tt1.setText(tt.getText());
               tt.setText("Owner says->"+pp);
          
          //tt.setText("");
              
            }
        });
	}
	  
  }
   



}

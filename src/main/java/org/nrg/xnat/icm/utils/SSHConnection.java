package org.nrg.xnat.icm.utils;

import java.util.*;

import java.lang.*;

import com.jcraft.jsch.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream.GetField;
import java.io.OutputStream;
import java.io.PrintWriter;


public class SSHConnection {

	

public SSHConnection(){


}

	public static String listImages[] = null;
	
	
 	// Les paramètres a distance 
	
    public  static String user = "";
    private  static String password = "";
    //private  static String host = "";
    private  static String host = "";
	
	
    
    /* la méthode qui permet de  recupere le contenu la liste des images singularity sur niolon*/
    public 	static  String[] recupererListImgCluster(String resultConsole){
    	
    	listImages = resultConsole.split("\\r?\\n");
        System.out.println("\n la taille de tableau lines est  : "+listImages.length +"\n");
         
    
    	return listImages;
    }
    
    
    
    
	public void doConnectionCluster() throws InterruptedException, JSchException, IOException {
		
		
		

        int port = 22;
        String remoteFile = "ls";
        
        ChannelExec channel = null;
        Session session = null;
        InputStream inputStream = null;
        OutputStream outputStream =null;
        ChannelShell channelShell = null;
        //String  commande= "niolon_interactive; cd /hpc/crise/rahmani.a/script; ls";
        
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            
            
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            System.out.println("Establishing Connection...");
            session.connect();
            System.out.println("Connection established.");
            
            // Execution de la commande
            
            channel = (ChannelExec) session.openChannel("exec");
            //channel.setCommand("ls");
            
            while (channel.isConnected()) {
                Thread.sleep(1000);
            }
            
            //channel.setCommand("cd /hpc/crise/rahmani.a/script ");
            channel.setCommand("ls /hpc/shared/apps/x86_64/softs/singularity_images");
            
            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            channel.setOutputStream(responseStream);
            channel.connect();
            
            System.out.println(" ************************************************ ");
            
            while (channel.isConnected()) {
                Thread.sleep(100);
            }
            
            
            String responseString = new String(responseStream.toByteArray());
            System.out.println(responseString);
            
            /* On appel la fonction de recupération de l'enssemble des images*/
            recupererListImgCluster(responseString);
            
			
			
			
			
			/*
            
            System.out.println("Crating SFTP Channel.");
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            System.out.println("SFTP Channel created.");

            InputStream inputStream = sftpChannel.get(remoteFile);
            
            try (Scanner scanner = new Scanner(new InputStreamReader(inputStream))) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    System.out.println(line);
                }
            }*/
           /* 
         } catch (JSchException e) {
            e.printStackTrace();
        }*/
        
        }finally {
            if (session != null) {
                session.disconnect();
                //outputStream.close();
				//inputStream.close();
				
            }
            if (channel != null) {
                channel.disconnect();
                //channelShell.disconnect();
            }
        }
	

	}
	
	
	
	
}

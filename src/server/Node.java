package server;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class Node {
	
    private String QUEUE_NODE_SEND;
    private String QUEUE_NODE_RECV;
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    private int index_node;
    
    private String messageRecv;
    private String messageSend;

    //Point haut de la matrice (en haut à gauche)
    private int x;
    private int y;

    //Longueur et largeur de la matrice
    private int width;
    private int height;

    //Channel permettant de communiquer avec l'extérieur
    private Channel channelClient;
    private String QUEUE_SERVEUR_RECV;
    private String EXCHANGE_NAME;

    //Random qui va nous permettre de décider ou va poper le prochain client
    private Random random;

    //unique identifier for each node
    private String identifiant;
    
    private int idIncr;

    // Client connected to this node
    private ConcurrentHashMap<String,Point> clientConnectedMap;
    
    // Client connected to the game
    private ConcurrentHashMap<String,Point> clientMap;

    // Skin for each client of the game
    private ConcurrentHashMap<String,String> clientSkin;

    public Node(int index_node, int index_queue_send, int index_queue_recv, int x, int y, int width, int height) throws IOException, TimeoutException {
    	
    	this.idIncr = 0;
        this.identifiant = String.valueOf(index_node);

        this.clientConnectedMap = new ConcurrentHashMap<>();
        this.clientMap = new ConcurrentHashMap<>();
        this.clientSkin= new ConcurrentHashMap<>();
        
        
        this.factory = new ConnectionFactory();
        this.factory.setHost("localhost");
        
        this.index_node = index_node;
        this.random = new Random();
        this.messageRecv = "";
        this.messageSend = "";
        
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        
        connection = null;
        try {
            connection = factory.newConnection();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        channel = connection.createChannel();

        channelClient = connection.createChannel();

        index_node = index_queue_recv;

        QUEUE_NODE_SEND = "channel" + index_queue_send;
        QUEUE_NODE_RECV = "channel" + index_queue_recv;
        EXCHANGE_NAME   = "instructions" + index_node;

        QUEUE_SERVEUR_RECV = "queue_client_recv"+index_node;

        // Queue to exchange with ring (so receive from the previous, send to the next)
        channel.queueDeclare(QUEUE_NODE_SEND, false, false, false, null);
        channel.queueDeclare(QUEUE_NODE_RECV, false, false, false, null);

        // Queue to receive message from client
        channelClient.queueDeclare(QUEUE_SERVEUR_RECV, false, false, false, null);

        // Queue to publish/subscribe with the client 
        // Queue to publish to all the clients
        channelClient.exchangeDeclare(EXCHANGE_NAME, "fanout");

    }

    public void start(){

    	// Thread to exchange with all the node the message from a client
        Thread threadRecv = new Thread(){
            public void run(){
                try {
                    Consumer consumer = new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                                throws IOException {
                   
                            messageRecv = new String(body, "UTF-8");
                            String[] splittedMessage = messageRecv.split(" ");

                            // If we haven't send the message, it process the message and send to the next 
                            
                        	if(messageRecv.contains("who")){
                                int posX = Integer.valueOf(splittedMessage[2]);
                                int posY = Integer.valueOf(splittedMessage[3]);
                                
                                // Test if the client is into this node
                        		if(posX <= x+width && posY <= y+height){
                        		    clientConnectedMap.put(splittedMessage[4], new Point(posX,posY));
                        			String message = "itsme "+index_node+" "+splittedMessage[1]+" "+splittedMessage[4];
                        			channel.basicPublish("", QUEUE_NODE_SEND, null, message.getBytes());
                        		}else{
                        			channel.basicPublish("", QUEUE_NODE_SEND, null, messageRecv.getBytes());
                        		}
                        	}
                        	else if(messageRecv.contains("itsme"))
                        	{
                        		// if the client need to come in this node, it specify the client
                        		if(Integer.valueOf(splittedMessage[2])==index_node){
                        			String message = "change "+splittedMessage[3]+" "+splittedMessage[1];
                        			channelClient.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
                        		}else{
                        			channel.basicPublish( "",QUEUE_NODE_SEND, null, messageRecv.getBytes());
                        		}
                        	}
                        	else{
                        		// It process the message if it's not this node that have send the message
                        		if(!splittedMessage[0].equals(String.valueOf(index_node))){
                                	String mess = splittedMessage[1];
                                	for(int i=2; i<splittedMessage.length; i++){
                            			mess += " " + splittedMessage[i];
                                	}
                                	
                                	
                                	// indexFrom is equal to 1 if it's a server
                                	processMessage(mess, splittedMessage[0]);
                        		}
                        	}
                            
                        }
                    };
                    channel.basicConsume(QUEUE_NODE_RECV, true, consumer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        threadRecv.start();

        // Thread allowing to communicate with the clients of this nodes
        Thread threadClient = new Thread(){
            public void run(){
                try {
                    Consumer consumerClient = new DefaultConsumer(channelClient) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                                throws IOException {
                            String messageRecvClient = new String(body, "UTF-8");
                            
                            // If value is null we receive from a client 
                            processMessage(messageRecvClient, null);
                        }
                    };
                    channelClient.basicConsume(QUEUE_SERVEUR_RECV, true, consumerClient);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        threadClient.start();
    }

    // inform the new client with the information of the clients already here
	public void informNewAboutPlayerAlreadyHere(String identifiant) throws IOException {
        Iterator<String> i = clientMap.keySet().iterator();
        while(i.hasNext()){
            String idP = (String)i.next();
            Point p = clientMap.get(idP);
            String message = "inform "+identifiant+" "+p.getX()+" "+p.getY()+" "+idP+" "+clientSkin.get(idP);
            channelClient.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
        }
    }

	// Send to the clients and the next node the message
    public void publishToClients(String message, String indexFrom){
        try {
            channelClient.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
            
            // The node add this own index if the client send to him or send the message with 
            // index of the other node
        	if(indexFrom!=null){
        		message = indexFrom + " " + message;
        	}else{
        		message = index_node + " " + message;
        	}
        
            channel.basicPublish("", QUEUE_NODE_SEND, null, message.getBytes());
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    public void processMessage(String messageToProcess, String indexFrom) throws IOException{
    	String[] splittedMessage = messageToProcess.split(" ");

        if(messageToProcess.contains("connexion"))
        {
            //On décide la position du client qui veut se connecter
            int pointX = x + random.nextInt(width);
            int pointY = y + random.nextInt(height);
            Point spawn= new Point(pointX,pointY);

            messageSend="connected "+String.valueOf(identifiant+idIncr)+" "+spawn.getX()+" "+spawn.getY()+" "+splittedMessage[1];

            publishToClients(messageSend, indexFrom);

            informNewAboutPlayerAlreadyHere(String.valueOf(identifiant+idIncr));

            clientConnectedMap.put(String.valueOf(identifiant+idIncr),spawn);
            clientSkin.put(String.valueOf(identifiant+idIncr), splittedMessage[1]);
            clientMap.put(String.valueOf(identifiant+idIncr),spawn);
            idIncr++;
        }
        else if(messageToProcess.contains("connected"))
        {
            clientMap.put(splittedMessage[1], new Point(Integer.valueOf(splittedMessage[2]), Integer.valueOf(splittedMessage[3])));
            clientSkin.put(splittedMessage[1], splittedMessage[4]);
            publishToClients(messageToProcess, indexFrom);
        }
        else if(messageToProcess.contains("move"))
        {
            //Il faut aussi prévenir ses clients qu'un client a bougé
        	int xC = Integer.valueOf(splittedMessage[2]); 
			int yC = Integer.valueOf(splittedMessage[3]);
            publishToClients(messageToProcess, indexFrom);
            if(clientConnectedMap.containsKey(splittedMessage[1])){
                clientConnectedMap.put(splittedMessage[1], new Point(xC, yC));

                if(xC > this.x + width || yC > this.y + height || yC < this.y || xC < this.x){
                    getIdNewMap(xC, yC, splittedMessage[1]);
                    clientConnectedMap.remove(splittedMessage[1]);
                }
            }
            
            Point newPos = new Point(xC, yC);
            
        	clientMap.put(splittedMessage[1], newPos);
        	
        	String clientSend = splittedMessage[1];
        	
        	if(getBeside(newPos) != null){
        		publishToClients("coucou " + clientSend, indexFrom);
            }
        }
        else if(messageToProcess.contains("leave"))
        {
            publishToClients("leave "+splittedMessage[1],indexFrom);
            clientConnectedMap.remove(splittedMessage[1]);
            clientMap.remove(splittedMessage[1]);
        }
        
        //channel.basicPublish(QUEUE_NODE_SEND, "", null, messageToProcess.getBytes());
        
    }
    	
    //Méthode demandant sur le ring quel est l'identifiant
    //Avec lequel doit communiquer le client qui a changé de matrice à présent.
    //xC et yC définisse la nouvelle position du client
    public void getIdNewMap(int xC, int yC, String idClient){
    	String message = "who "+index_node+" "+xC+" "+yC+" "+idClient;
    	try {
			channel.basicPublish("", QUEUE_NODE_SEND,null, message.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public String getBeside(Point character){
    	
    	for (ConcurrentHashMap.Entry<String,Point> e : clientMap.entrySet()){
    	    Point p = e.getValue();
    	    if((character.x == p.x+1 && character.y == p.y) || 
    	       (character.x == p.x-1 && character.y == p.y) ||
    	       (character.y == p.y+1 && character.x == p.x) ||
    	       (character.y == p.y-1 && character.x == p.x)){
    	    	return e.getKey();
    	    }
    	}
    	
    	return null;
    }
    
	@Override
	public String toString() {
		return "Node [index_node=" + index_node + ", x=" + x + ", y=" + y + ", width=" + width + ", height=" + height
				+ "]";
	}

}
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

    //TODO: GENERER DES IDENTIFIANTS UNIQUE SUR TOUS LES NOEUDS!
    private String identifiant;
    
    private int idIncr;

    private ConcurrentHashMap<String,Point> clientConnectedMap;
    
    private ConcurrentHashMap<String,Point> clientMap;

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

        channel.queueDeclare(QUEUE_NODE_SEND, false, false, false, null);
        channel.queueDeclare(QUEUE_NODE_RECV, false, false, false, null);


        //En revanche on continue de recevoir les messages de nos clients sur une queue
        channelClient.queueDeclare(QUEUE_SERVEUR_RECV, false, false, false, null);

        //Déclaration de notre moyen de publisher / consumer.
        //On utilisera ensuite cette commande pour publish dans les queues des consommateurs:
        channelClient.exchangeDeclare(EXCHANGE_NAME, "fanout");

    }

    public void start(){

        Thread threadRecv = new Thread(){
            public void run(){
                try {
                    Consumer consumer = new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                                throws IOException {
                   
                            messageRecv = new String(body, "UTF-8");
                            String[] splittedMessage = messageRecv.split(" ");

                            //Si ce n'est pas nous qui l'avons envoyé, on le traite et on envoi aux suivants
                            
                        	if(messageRecv.contains("who")){
                        		//Il est chez nous!
                                int posX = Integer.valueOf(splittedMessage[2]);
                                int posY = Integer.valueOf(splittedMessage[3]);
                        		if(posX<=x+width && posY<=y+height){
                        		    clientConnectedMap.put(splittedMessage[4], new Point(posX,posY));
                        			String message = "itsme "+index_node+" "+splittedMessage[1]+" "+splittedMessage[4];
                        			channel.basicPublish("",QUEUE_NODE_SEND ,null, message.getBytes());
                        		}else{
                        			channel.basicPublish("", QUEUE_NODE_SEND,null, messageRecv.getBytes());
                        		}
                        	}
                        	else if(messageRecv.contains("itsme"))
                        	{
                        		if(Integer.valueOf(splittedMessage[2])==index_node){
                        			String message = "change "+splittedMessage[3]+" "+splittedMessage[1];
                        			channelClient.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
                        		}else{
                        			channel.basicPublish( "",QUEUE_NODE_SEND, null, messageRecv.getBytes());
                        		}
                        	}
                        	else{
                        	    //On traite le message si ce n'est pas nous qui l'avons envoyé
                        		if(!splittedMessage[0].equals(String.valueOf(index_node))){
                                	String mess = splittedMessage[1];
                                	for(int i=2; i<splittedMessage.length; i++){
                            			mess += " " + splittedMessage[i];
                                	}
                                	
                                	//Si 1 c'est un message de la part d'un serv
                                	processMessage(mess,splittedMessage[0]);
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

        //Thread permettant de communiquer avec les clients du serveur
        Thread threadClient = new Thread(){
            public void run(){
                try {
                    Consumer consumerClient = new DefaultConsumer(channelClient) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                                throws IOException {
                            String messageRecvClient = new String(body, "UTF-8");
                            //Si 0 c'est un message des clients
                            //On met indexFrom à null lorsque l'on recoit un message de la part de nos clients
                            //IndexFrom n'est donc pas à null si l'on recoit le message d'un autre serveur
                            processMessage(messageRecvClient,null);
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

	@Override
	public String toString() {
		return "Node [index_node=" + index_node + ", x=" + x + ", y=" + y + ", width=" + width + ", height=" + height
				+ "]";
	}

	public Point splitPositionCharacter(String character){
		Point p;

		String position[] = character.split(" ");

		int x = Integer.parseInt(position[0]);
		int y = Integer.parseInt(position[1]);

		p = new Point(x, y);

		return p;
	}

	public void informNewAboutPlayerAlreadyHere(String identifiant) throws IOException {
        Iterator<String> i = clientMap.keySet().iterator();
        while(i.hasNext()){
            String idP = (String)i.next();
            Point p = clientMap.get(idP);
            String message = "inform "+identifiant+" "+p.getX()+" "+p.getY()+" "+idP+" "+clientSkin.get(idP);
            channelClient.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
        }
    }

    public void publishToClients(String message, String indexFrom){
        try {
            channelClient.basicPublish(EXCHANGE_NAME, "", null,
                    message.getBytes());
            
            //On ajoute ou non l'index du serveur qui a envoyé le message
        	if(indexFrom!=null){
        		message = indexFrom+" "+message;
        	}else{
        		message = index_node+" "+message;
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

                //TODO:Mauvaise condition
                if(xC > this.x + width || yC > this.y + height || yC < this.y || xC < this.x){
                    getIdNewMap(xC, yC, splittedMessage[1]);
                    clientConnectedMap.remove(splittedMessage[1]);
                }
            }
            
            Point newPos = new Point(xC, yC);
            
        	clientMap.put(splittedMessage[1], newPos);
        	
        	String clientSend = splittedMessage[1];
        	String clientRecv;
        	
        	if((clientRecv = getBeside(newPos)) != null){
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
    

}
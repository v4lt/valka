package client;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Communication{
    private String EXCHANGE_NAME;
    private String QUEUE_CLIENT_SEND;
    private String id;
    private Channel channel;
    private String url;

    //Objet permettant de notifier l'appli lorsque qu'on recoit des infos des autres joueurs du réseau
    private ICallBackForChangesOnMap callBack;

    public Communication(ICallBackForChangesOnMap callBack, int nodeToConnect,String url, Channel channel) throws IOException, TimeoutException {
        this.callBack = callBack;
        this.channel = channel;
        this.EXCHANGE_NAME = "instructions"+nodeToConnect;
        this.url = url;
        this.QUEUE_CLIENT_SEND = "queue_client_recv"+nodeToConnect;
        this.id = null;
    }

    public void communicate() throws IOException, TimeoutException {

        //Notre queue pour envoyer nos instructions à notre serv
        channel.queueDeclare(QUEUE_CLIENT_SEND, false, false, false, null);

        //Un "exchange" afin de recevoir les instructions du serveur suivant le schéma de producteur / consommateur
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        //On génére une queue, et on la bind
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");


        if(id==null){
            String message = "connexion "+url;
            channel.basicPublish("", QUEUE_CLIENT_SEND, null, message.getBytes("UTF-8"));
        }

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                String message = new String(body, "UTF-8");
                String[] splittedMessage = message.split(" ");
                
                if(message.contains("connected"))
                {
                    //On prévient app qu'il est à présent connecté
                    if(id==null){
                    	id=splittedMessage[1];
                        callBack.connected(splittedMessage[1],new Point(Integer.valueOf(splittedMessage[2]),
                                Integer.valueOf(splittedMessage[3])));
                    }else{
                        callBack.someoneJoin(splittedMessage[1], new Point(Integer.valueOf(splittedMessage[2]),
                                Integer.valueOf(splittedMessage[3])), splittedMessage[4]);
                    }

                }
                else if(message.contains("coucou"))
                {
                	callBack.sayHello(splittedMessage[1], true);
                }
                else if(message.contains("move"))//Quelqu'un a bougé
                {
                	callBack.sayHello(splittedMessage[1], false);
                    callBack.someoneMove(splittedMessage[1], new Point(Integer.valueOf(splittedMessage[2]),
                            Integer.valueOf(splittedMessage[3])));
                }
                else if(message.contains("inform"))
                {
                    callBack.printPlayersFirstConnexion(splittedMessage[1], new Point(Integer.valueOf(splittedMessage[2]),
                            Integer.valueOf(splittedMessage[3])), splittedMessage[4], splittedMessage[5]);
                }
                else if(message.contains("leave"))
                {
                    callBack.someoneLeave(splittedMessage[1]);
                }
                else if(message.contains("change"))
                {
                    //Si ce message nous est destiné
                	if(splittedMessage[1].equals(id))
                    {
                        QUEUE_CLIENT_SEND="queue_client_recv"+splittedMessage[2];
                        EXCHANGE_NAME = "instructions"+splittedMessage[2];
                        //Notre queue pour envoyer nos instructions à notre serv
                        channel.queueDeclare(QUEUE_CLIENT_SEND, false, false, false, null);

                        //Un "exchange" afin de recevoir les instructions du serveur suivant le schéma de producteur / consommateur
                        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
                        //On génére une queue, et on la bind
                        String queueName = channel.queueDeclare().getQueue();
                        channel.queueBind(queueName, EXCHANGE_NAME, "");
                    }
                }
            }
        };
        //On consomme sur une des queues générées
        channel.basicConsume(queueName, true, consumer);
    }

    //Méthode appelée par APP pour dire que l'on se déplace

    public void move(String id, Point p) {
        String message = "move "+id+" "+p.getX()+" "+p.getY();
        try {
            channel.basicPublish("", QUEUE_CLIENT_SEND, null, message.getBytes("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void leave(){
        String message = "leave "+this.id;
        try {
            channel.basicPublish("", QUEUE_CLIENT_SEND, null, message.getBytes("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

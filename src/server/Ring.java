package server;

import com.rabbitmq.client.*;

import java.io.IOException;

import java.util.ArrayList;

import client.Point;

public class Ring {
	
    public static void main(String[] argv) throws Exception {
    	ArrayList<Node> nodes = new ArrayList<>();

        int index_queue_send;
        int index_queue_recv;

        int nb_node = 4;

        ArrayList<Point> map = new ArrayList<>();
        
        map.add(new Point(0, 0));
        map.add(new Point(0, 8));
        map.add(new Point(8, 8));
        map.add(new Point(8, 0));
        
        int x;
        int y;
        
        for(int i=0; i<nb_node; i++){
            index_queue_send = ((i + 1) % nb_node);
            index_queue_recv = i;
            
            x = map.get(i).getX();
            y = map.get(i).getY();
            
            nodes.add(new Node(i, index_queue_send, index_queue_recv, x, y, 7, 7));
        }

        for(int i=0; i<nb_node; i++){
            nodes.get(i).start();
            System.out.println(nodes.get(i));
        }

        //Connection
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String QUEUE_NAME = "ClientServer";

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        /*System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        Consumer consumerNodes = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println(" [x] Received '" + message + "'");
            }
        };

        channel.basicConsume(QUEUE_NAME, true, consumerNodes);*/


        Consumer consumerClient = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                if(message.contains("get")){
                    String m = "node "+nb_node;
                    channel.basicPublish("", QUEUE_NAME, null, m.getBytes());
                }
            }
        };

        channel.basicConsume(QUEUE_NAME, true, consumerClient);



    }
    
}

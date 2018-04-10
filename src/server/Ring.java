package server;

import com.rabbitmq.client.*;

import java.io.IOException;

import java.util.ArrayList;

import client.Point;

public class Ring {
	
    public static void main(String[] argv) throws Exception {
    	// list of all the nodes
    	ArrayList<Node> nodes = new ArrayList<>();

        int index_queue_send;
        int index_queue_recv;

        // specify the number of nodes
        int nb_node = 4;

        // limit inside the map of each node
        ArrayList<Point> map = new ArrayList<>();
        
        // list of limits
        map.add(new Point(0, 0));
        map.add(new Point(0, 8));
        map.add(new Point(8, 8));
        map.add(new Point(8, 0));
        
        int x;
        int y;
        
        // creation of nodes with for each the limit of the specified node
        for(int i=0; i<nb_node; i++){
            index_queue_send = ((i + 1) % nb_node);
            index_queue_recv = i;
            
            x = map.get(i).getX();
            y = map.get(i).getY();
            
            nodes.add(new Node(i, index_queue_send, index_queue_recv, x, y, 7, 7));
        }

        for(int i=0; i<nb_node; i++){
            nodes.get(i).start();
        }
        
        System.out.println("Server start");

        //Connection with the client to say him the number of node (allow to the client to choose his node)
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String QUEUE_NAME = "ClientServer";

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        Consumer consumerClient = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                if(message.contains("get")){
                    String m = "node " + nb_node;
                    channel.basicPublish("", QUEUE_NAME, null, m.getBytes());
                }
            }
        };

        channel.basicConsume(QUEUE_NAME, true, consumerClient);



    }
    
}

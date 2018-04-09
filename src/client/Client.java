package client;

import com.rabbitmq.client.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeoutException;

public class Client extends Application
{
    private Channel channel;
    private String  QUEUE_CLIENT_SEND;
    private BorderPane mainBorder;
    private ToggleGroup groupNodeButton;
    private ToggleGroup toggleButtonGroup;
    private Stage stage;
    private String idChannel;
    private Connection connection;
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        mainBorder = new BorderPane();
        this.QUEUE_CLIENT_SEND = "ClientServer";
        groupNodeButton = new ToggleGroup();
        Label       labelIpServ    = new Label("Ip");
        TextField   txtFieldIpServ = new TextField();
        Button      ipButton       = new Button("connect");
        HBox        hboxIp         = new HBox(labelIpServ, txtFieldIpServ, ipButton);
        mainBorder.setTop(hboxIp);
        stage.setTitle("Valka's Client");
        stage.setScene(new Scene(mainBorder,500,500));
        stage.show();
        ipButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                String ip = txtFieldIpServ.getText();
                ConnectionFactory factory = new  ConnectionFactory();
                factory.setHost(ip);
                if(!ip.equals("localhost")){
                    factory.setUsername("test");
                    factory.setPassword("test");
                }
                try {
                    connection = factory.newConnection();
                    channel = connection.createChannel();
                    channel.queueDeclare(QUEUE_CLIENT_SEND, false, false, false, null);
                    String message = "get ";
                    initCommunication();
                    channel.basicPublish("", QUEUE_CLIENT_SEND, null, message.getBytes());

                } catch (Exception e) {
                    e.printStackTrace();
                    txtFieldIpServ.setText("Can't reach "+ip);
                }

            }
        });
    }

    public void initCommunication() throws IOException {
        Consumer consumer = new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException
            {
                VBox vBox = new VBox();
                HBox  hboxNodes = new HBox();
                String message = new String(body, "UTF-8");
                String[] splittedMessage = message.split(" ");
                if(message.contains("node")){
                    int nbNode = Integer.valueOf(splittedMessage[1]);
                    for (int i = 0; i < nbNode; i++) {
                        RadioButton r = new RadioButton();
                        r.setText(String.valueOf(i));
                        r.setToggleGroup(groupNodeButton);
                        if(i==0)
                            r.setSelected(true);
                        hboxNodes.getChildren().add(r);
                    }
                    Button buttonPlay = new Button("PLAY");
                    buttonPlay.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent actionEvent) {
                            play();
                        }
                    });
                    vBox.getChildren().add(hboxNodes);

                    toggleButtonGroup = new ToggleGroup();
                    ToggleButton tg1 = new ToggleButton("kakashi");
                    tg1.setToggleGroup(toggleButtonGroup);
                    tg1.setSelected(true);
                    ToggleButton tg2 = new ToggleButton("haku");
                    tg2.setToggleGroup(toggleButtonGroup);
                    ToggleButton tg3 = new ToggleButton("fille");
                    tg3.setToggleGroup(toggleButtonGroup);
                    ToggleButton tg4 = new ToggleButton("exemple");
                    tg4.setToggleGroup(toggleButtonGroup);
                    ToggleButton tg5 = new ToggleButton("jiraya");
                    tg5.setToggleGroup(toggleButtonGroup);
                    HBox toggleButtonHbox = new HBox(tg1,tg2,tg3,tg4,tg5);
                    vBox.getChildren().add(toggleButtonHbox);
                    buildUi(buttonPlay, vBox);
                    channel.basicCancel(idChannel);
                }
            }
        };
        idChannel = channel.basicConsume(QUEUE_CLIENT_SEND, true, consumer);
    }

    public void play(){
        try {

            RadioButton r = (RadioButton) groupNodeButton.getSelectedToggle();
            ToggleButton t = (ToggleButton)  toggleButtonGroup.getSelectedToggle();
            App app = new App(Integer.valueOf(r.getText()), t.getText(), channel);
            this.stage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildUi(Button b, VBox v){
        Platform.runLater(new Runnable(){
            @Override
            public void run() {
                mainBorder.setCenter(v);
                mainBorder.setBottom(b);
            }
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}

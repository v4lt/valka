package client;

import client.Character;
import client.Direction;
import client.Map;
import client.Rafraichissement;
import com.rabbitmq.client.Channel;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.canvas.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class App extends Stage implements ICallBackForChangesOnMap
{
	ConcurrentHashMap<String, Character> listCharacters;
	Communication communication;
	Map map;
    Scene scene;

	//Correspond à NOTRE personnage
	Character character;

	//Correspond à NOTRE identifiant
	String id;
	int nodeToConnect;
	String characterUrl;
	GraphicsContext gc;

	Channel channel;

	public App(int node, String characterUrl, Channel channel){
	    this.channel = channel;
	    this.nodeToConnect = node;
	    this.characterUrl = "/frames/"+characterUrl+".png";

        StackPane root = new StackPane();

        map = new Map("map.json");


        int width = map.getWidth() * 32;
        int height = map.getHeight() * 32;

        Canvas canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();

        Rafraichissement r = new Rafraichissement(map, gc);
        r.start();

        root.getChildren().add(canvas);

        scene = new Scene(root, width, height);

        this.setTitle("valka's game");
        this.setScene(scene);
        this.show();
        this.setOnHiding(new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                Platform.runLater(new Runnable() {

                    @Override
                    public void run() {
                        communication.leave();
                        System.exit(0);
                    }
                });
            }
        });
        listCharacters = new ConcurrentHashMap<>();
        try {
            communication = new Communication(this, nodeToConnect,this.characterUrl ,this.channel);
            communication.communicate();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }


	//L'objet communication nous prévient via cette méthode lorsque quelqu'un a bougé
	@Override
	public void someoneMove(String id, Point newPosition) {
        if(!id.equals(this.id)){
            Character characterMoving = listCharacters.get(id);
            characterMoving.moveWithPoint(newPosition, map, listCharacters);
        }
	}

	//L'objet communication nous prévient via cette méthode lorsque quelqu'un a rejoint la partie
	@Override
	public void someoneJoin(String id, Point p, String url) {
	    if(!id.equals(this.id)){
            Character character = new Character(url, p.getX(), p.getY(), Direction.DOWN);
            map.addPlayer(character);
            listCharacters.put(id, character);
        }
	}

	//L'objet communication nous prévient via cette méthode lorsque quelqu'un a quitté la partie
	@Override
	public void someoneLeave(String id) {
        System.out.println("LEAVE");
        if(!id.equals((this.id))){
            map.removePlayer(listCharacters.get(id));
            listCharacters.remove(id);
        }
	}

	//Lorsque l'on recoit un message "inform" avec notre id, un point, et l'id d'un autre joueur, cela veut dire que
    //Des joueurs étaient déjà présent sur le serveur, on doit donc les afficher
	@Override
	public void printPlayersFirstConnexion(String myId, Point p, String idOtherPlayer, String url){
        if(myId.equals(this.id)){
            Character character = new Character(url, p.getX(), p.getY(), Direction.DOWN);
            map.addPlayer(character);
            listCharacters.put(idOtherPlayer, character);
        }
    }

	public void sayHello(String client, boolean say){
		if(listCharacters.containsKey(client)){
			listCharacters.get(client).setHello(say);
		}else if(client.equals(id)){
			character.setHello(say);
		}else{
			System.out.println("problème");
		}
	}
	
    //L'objet communication nous prévient via cette méthode que l'on est à présent connecté
    // @id correspond à l'id que nous a donné le serveur
    // @p  correspond à la position sur laquelle on est apparue
    @Override
    public void connected(String id, Point p) {
	    this.character = new Character(this.characterUrl, p.getX(), p.getY(), Direction.DOWN);
	    map.addPlayer(character);
        this.id = id;
        
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
        	Direction direction = null;
        	
            if (event.getCode().equals(KeyCode.Z)) {
                if(character.move(Direction.UP, map, listCharacters)){
                    direction = Direction.UP;
                }
            }else if(event.getCode().equals(KeyCode.S)){
                if(character.move(Direction.DOWN, map, listCharacters)){
                	direction = Direction.DOWN;
                }
            }else if(event.getCode().equals(KeyCode.Q)){
                if(character.move(Direction.LEFT, map, listCharacters)){
                	direction = Direction.LEFT;
                }
            }else if(event.getCode().equals(KeyCode.D)){
                if(character.move(Direction.RIGHT, map, listCharacters)){
                	direction = Direction.RIGHT;
                }
            }
            
            if(direction != null){
            	communication.move(this.id, new Point(this.character.getX(), this.character.getY()));
            }
        });
    }
}

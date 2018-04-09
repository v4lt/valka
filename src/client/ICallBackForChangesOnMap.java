package client;

public interface ICallBackForChangesOnMap {
    //Direction -> z,q,s,d
    void someoneMove(String id, Point p);
    void someoneJoin(String id, Point p, String url);
    void someoneLeave(String id);
    void connected(String id, Point p);
    void sayHello(String client, boolean say);
    void printPlayersFirstConnexion(String id, Point p, String idOtherPlayer, String url);
}

package dev.espi.protectionstones.data;

import dev.espi.protectionstones.ProtectionStones;
import dev.espi.protectionstones.database.daos.ProtectionPlayer;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Andrew R.
 */
public class ProtectionDataHandler {

    private ConcurrentHashMap<String, ProtectionPlayer> players;
    private ProtectionStones pS = ProtectionStones.getInstance();

    public ProtectionDataHandler(){
        players = new ConcurrentHashMap<>();
    }

    public void lease(){
        for(ProtectionPlayer protectionPlayer : players.values()){
            pS.getMongoDB().getDs().save(protectionPlayer);
            players.remove(protectionPlayer.getName());
        }
    }

    public ProtectionPlayer getPlayer(Player player){
        ProtectionPlayer pPlayer = pS.getMongoDB().getDs().createQuery(ProtectionPlayer.class).filter("name ==", player.getName()).first();
        if(pPlayer == null) pPlayer = new ProtectionPlayer(player.getName());
        players.put(player.getName(), pPlayer);
        return players.get(player.getName());
    }

}

package dev.espi.protectionstones.database.daos;

import dev.espi.protectionstones.ProtectionStones;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PreSave;
import lombok.Getter;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew R.
 */
public class ProtectionPlayer {

    transient ProtectionStones pS = ProtectionStones.getInstance();

    @Id
    private ObjectId id;
    @Getter
    private String name;
    private Protections protections;

    public ProtectionPlayer(String name){
        this.id = new ObjectId();
        this.name = name;
        this.protections = new Protections(id);
        pS.getMongoDB().getDs().save(this.protections);
    }

    @PreSave
    public void onSave(){
        pS.getMongoDB().getDs().save(protections);
    }

    @PostLoad
    public void onLoaded(){
        protections = pS.getMongoDB().getDs().find(Protections.class).field("_id").equal(id).first();
    }

    public void addProtection(String server, String protection){
        if(getProtectionsFromServer(server).isEmpty()){
            protections.getProtections().put(server, new ArrayList<>());
        }
        protections.getProtections().get(server).add(protection);
    }

    public void removeProtection(String server, String protection){
        if(getProtectionsFromServer(server).isEmpty()) return;
        protections.getProtections().get(server).remove(protection);
    }

    public List<String> getProtectionsFromServer(String server){
        List<String> final_protections = new ArrayList<>();
        if(protections.getProtections().get(server) != null){
            final_protections = protections.getProtections().get(server);
        }
        return final_protections;
    }

    public boolean capableOfCreation(String server){
        Player player = Bukkit.getPlayer(name);
        int protections = getProtectionsFromServer(server).size();

        return player.hasPermission("protections.create.*") ? true : player.hasPermission("protections.create."+protections);
    }

}

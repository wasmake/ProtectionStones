package dev.espi.protectionstones.database.daos;

import dev.morphia.annotations.Id;
import lombok.Getter;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.List;

/**
 * @author Andrew R.
 */
public class Protections {

    @Id
    private ObjectId id;
    @Getter
    private HashMap<String, List<String>> protections;

    public Protections(ObjectId objectId){
        this.id = objectId;
        this.protections = new HashMap<>();
    }



}

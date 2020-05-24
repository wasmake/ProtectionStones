package dev.espi.protectionstones.database;

import com.mongodb.MongoClient;
import dev.morphia.Datastore;
import dev.morphia.Morphia;

/**
 * @author Andrew R.
 */
public interface MongoDB {

    void connect(String host, int port, String username, String database, String password, String databaseLogin);

    void registerClass(Class... classes);

    MongoClient getClient();

    Morphia getMorphia();

    Datastore getDs();
}



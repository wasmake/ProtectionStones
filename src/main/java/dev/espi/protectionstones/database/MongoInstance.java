package dev.espi.protectionstones.database;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import dev.morphia.Datastore;
import dev.morphia.Morphia;

/**
 * @author Andrew R.
 */
public class MongoInstance implements MongoDB {

    private MongoClient client;
    private Morphia morphia;
    private Datastore datastore;

    @Override
    public void connect(String host, int port, String username, String database, String password, String databaseLogin) {
        MongoClientOptions mongoOptions = MongoClientOptions.builder()
                .connectionsPerHost(10)
                .maxConnectionIdleTime(600000)
                .socketTimeout(60000)
                .connectTimeout(15000).build();
        client = new MongoClient(new ServerAddress(host, port), MongoCredential.createCredential(username, databaseLogin, password.toCharArray()), mongoOptions);
        morphia = new Morphia();
        datastore = morphia.createDatastore(client, database);
        datastore.ensureIndexes();

        datastore.ensureIndexes();
    }

    @Override
    public void registerClass(Class... classes) {
        morphia.map(classes);
    }

    @Override
    public MongoClient getClient() {
        return client;
    }

    @Override
    public Morphia getMorphia() {
        return morphia;
    }

    @Override
    public Datastore getDs() {
        return datastore;
    }
}

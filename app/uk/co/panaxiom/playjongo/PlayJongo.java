package uk.co.panaxiom.playjongo;

import com.mongodb.*;
import org.jongo.Jongo;
import org.jongo.Mapper;
import org.jongo.MongoCollection;

import play.Configuration;
import play.Logger;
import play.Play;

import com.mongodb.gridfs.GridFS;

public class PlayJongo {

    private static volatile PlayJongo INSTANCE = null;

    MongoClient mongo = null;
    Jongo jongo = null;
    GridFS gridfs = null;

    PlayJongo(Configuration config, ClassLoader classLoader, boolean isTestMode) throws Exception {
        MongoClientURI uri = new MongoClientURI(
                isTestMode
                    ? config.getString("playjongo.test-uri", "mongodb://127.0.0.1:27017/test")
                    : config.getString("playjongo.uri", "mongodb://127.0.0.1:27017/play"));

        mongo = new MongoClient(uri);
        DB db = mongo.getDB(uri.getDatabase());

        // Set write concern if configured
        String defaultWriteConcern = config.getString("playjongo.defaultWriteConcern");
        if(defaultWriteConcern != null) {
            db.setWriteConcern(WriteConcern.valueOf(defaultWriteConcern));
        }

        // Authenticate the user if necessary
        if (uri.getUsername() != null) {
            db.authenticate(uri.getUsername(), uri.getPassword());
        }
        jongo = new Jongo(db, createMapper(config, classLoader));

        if (config.getBoolean("playjongo.gridfs.enabled", false)) {
            gridfs = new GridFS(jongo.getDatabase());
        }
    }

    private Mapper createMapper(Configuration config, ClassLoader classLoader) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        final String factoryClassName = config.getString("playjongo.mapperfactory",
                JongoMapperFactory.DefaultFactory.class.getName());
        JongoMapperFactory factory = (JongoMapperFactory) Class.forName(
                factoryClassName,
                true,
                classLoader).newInstance();
        return factory.create();
    }

    public static PlayJongo getInstance() {
        if (INSTANCE == null) {
            synchronized (PlayJongo.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = new PlayJongo(Play.application().configuration(), Play.application().classloader(), Play.isTest());
                    } catch (Exception e) {
                        Logger.error(e.getClass().getSimpleName(), e);
                    }
                }
            }
        }
        return INSTANCE;
    }

    public static void forceNewInstance() {
        INSTANCE = null;
        getInstance();
    }

    public static Mongo mongo() {
        return getInstance().mongo;
    }

    public static Jongo jongo() {
        return getInstance().jongo;
    }

    public static GridFS gridfs() {
        return getInstance().gridfs;
    }

    public static MongoCollection getCollection(String name) {
        return getInstance().jongo.getCollection(name);
    }

    public static DB getDatabase() {
        return getInstance().jongo.getDatabase();
    }
}
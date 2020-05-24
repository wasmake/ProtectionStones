package dev.espi.protectionstones.utils.config;

import dev.espi.protectionstones.ProtectionStones;
import lombok.Getter;

import java.io.File;

/**
 * @author Andrew R.
 */
public class PSWConfig {

    private ProtectionStones pS = ProtectionStones.getInstance();
    @Getter
    private PSConfiguration config;

    public PSWConfig(){
        File configFile = new File(pS.getDataFolder(), "database.yml");
        config = new PSConfiguration(configFile);
        config.options().header("-------------------------------- # \nProtection Stones MongoDB Adapter \nBy wasmake\n-------------------------------- #");
        config.addDefault("db.host", "localhost");
        config.addDefault("db.port", 27017);
        config.addDefault("db.root", "root");
        config.addDefault("db.database", "protections");
        config.addDefault("db.pass", "password");
        config.addDefault("db.authdb", "Admin");
        config.fullSave();
    }

}

package dev.espi.protectionstones.utils.config;

import java.io.File;

/**
 * @author Kevin (CookLoco)
 *
 */
public interface IConfiguration {

    File getFile();

    void save();

    void addDefault(String path, Object defaultValue, String... comments);

    void createSection(String path, String... comments);

    void setHeader(String... header);

    void set(String key, Object value, String... comments);

}
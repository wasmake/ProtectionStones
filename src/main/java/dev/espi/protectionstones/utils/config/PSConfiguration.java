package dev.espi.protectionstones.utils.config;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class PSConfiguration extends YamlConfiguration implements IConfiguration {

    private EConfiguration econfig;
    private File file;

    public PSConfiguration(File file) {
        super();
        this.econfig = new EConfiguration();
        this.file = file;
        try {
            this.load(file);
        } catch (FileNotFoundException ignored) {
            ;
        } catch (IOException | InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file, ex);
        }

    }

    public EConfiguration getEConfig() {
        return econfig;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void load(File file) throws IOException, InvalidConfigurationException {
        this.file = file;
        super.load(file);
        super.options().header("");
        BufferedReader configReader = null;
        List<String> configLines = new ArrayList<>();
        try {
            configReader = new BufferedReader(new FileReader(file));
            String configReadLine;
            while ((configReadLine = configReader.readLine()) != null) configLines.add(configReadLine);
        } finally {
            if (configReader != null) configReader.close();
        }

        if (configLines.isEmpty()) {
            Bukkit.getLogger().log(Level.SEVERE, file.getName() + " doesn't have nothing to load");
            return;
        }

        boolean hasHeader = !econfig.trim(configLines.get(0)).isEmpty();

        Map<String, List<String>> configComments = new LinkedHashMap<>();
        for (int lineIndex = 0; lineIndex < configLines.size(); lineIndex++) {
            String configLine = configLines.get(lineIndex);
            String trimmedLine = econfig.trimPrefixSpaces(configLine);
            if (trimmedLine.startsWith("#") && (lineIndex > 0 || !hasHeader)) {
                String configKey = econfig.getPathToComment(configLines, lineIndex, configLine);
                if (configKey != null) {
                    List<String> keyComments = configComments.get(configKey);
                    if (keyComments == null) keyComments = new ArrayList<>();
                    keyComments.add(trimmedLine.substring(trimmedLine.startsWith("# ") ? 2 : 1));
                    configComments.put(configKey, keyComments);
                }
            }
        }
    }

    public void fullSave(){
        this.options().copyDefaults(true);
        this.getEConfig().setNewLinePerKey(true);
        this.save();
    }

    @Override
    public void save() {
        try {
            this.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save(File file) throws IOException {
        super.save(file);

        List<String> configContent = new ArrayList<>();
        BufferedReader configReader = null;
        try {
            configReader = new BufferedReader(new FileReader(file));
            String configReadLine;
            while ((configReadLine = configReader.readLine()) != null) configContent.add(configReadLine);
        } finally {
            if (configReader != null) configReader.close();
        }

        BufferedWriter configWriter = null;
        try {
            configWriter = new BufferedWriter(new FileWriter(file));
            configWriter.write("");
            for (int lineIndex = 0; lineIndex < configContent.size(); lineIndex++) {
                String configLine = configContent.get(lineIndex);

                String configKey = null;
                if (!configLine.startsWith("#") && configLine.contains(":"))
                    configKey = econfig.getPathToKey(configContent, lineIndex, configLine);
                if (configKey != null && econfig.getComments().containsKey(configKey)) {
                    int numOfSpaces = econfig.getPrefixSpaceCount(configLine);
                    String spacePrefix = "";
                    for (int i = 0; i < numOfSpaces; i++) spacePrefix += " ";
                    List<String> configComments = econfig.getComments().get(configKey);

                    if (configComments != null) {
                        for (String comment : configComments) {
                            configWriter.append(spacePrefix).append("# ").append(comment);
                            configWriter.newLine();
                        }
                    }
                }

                boolean isComment = configLine.startsWith("#");

                if (configLine.startsWith("-") || configLine.startsWith("  -") || configLine.startsWith("    -") || configLine.startsWith("      -")) {
                    configWriter.append("  " + configLine);
                } else {
                    configWriter.append(configLine);
                }
                configWriter.newLine();

                if (econfig.shouldAddNewLinePerKey() && lineIndex < configContent.size() - 1 && !isComment) {
                    String nextConfigLine = configContent.get(lineIndex + 1);
                    if (nextConfigLine != null && !nextConfigLine.startsWith(" ")) {
                        if (!nextConfigLine.startsWith("'") && !nextConfigLine.startsWith("-")) configWriter.newLine();
                    }
                }
            }
        } finally {
            if (configWriter != null) configWriter.close();
        }
    }

    @Override
    public void set(String key, Object value) {
        if (value != null) {
            if (econfig.getComments(key).size() > 0)
                econfig.getComments().put(key, econfig.getComments(key));
            else
                econfig.getComments().remove(key);
        } else {
            econfig.getComments().remove(key);
        }
        super.set(key, value);
    }

    @Override
    public void addDefault(String path, Object defaultValue, String... comments) {
        if (defaultValue != null && comments != null && comments.length > 0 && !econfig.getComments().containsKey(path)) {
            List<String> commentsList = new ArrayList<>();
            for (String comment : comments) {
                if (comment != null) commentsList.add(comment);
                else commentsList.add("");
            }
            econfig.getComments().put(path, commentsList);
        }
        super.addDefault(path, defaultValue);

    }

    public void createSection(String path, String... comments) {
        if (path != null && comments != null && comments.length > 0) {
            List<String> commentsList = new ArrayList<>();
            for (String comment : comments) {
                if (comment != null) commentsList.add(comment);
                else commentsList.add("");
            }
            econfig.getComments().put(path, commentsList);
        }
        super.createSection(path);
    }

    @Override
    public void setHeader(String... header) {
        String h = "";
        for (String line : header) {
            h += line + "\n";
        }
        super.options().header(h);
    }

    @Override
    public void set(String key, Object value, String... comments) {
        if (value != null) {
            if (comments != null) {
                if (comments.length > 0) {
                    List<String> commentsList = new ArrayList<>();
                    for (String comment : comments) {
                        if (comment != null) commentsList.add(comment);
                        else commentsList.add("");
                    }
                    econfig.getComments().put(key, commentsList);
                } else {
                    econfig.getComments().remove(key);
                }
            }
        } else {
            econfig.getComments().remove(key);
        }
        super.set(key, value);
    }

}
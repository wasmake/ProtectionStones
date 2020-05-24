package dev.espi.protectionstones.utils.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kevin (CookLoco)
 *
 */
public class EConfiguration {

    protected Map<String, List<String>> comments = new LinkedHashMap<>();
    protected boolean newLinePerKey = false;

    public EConfiguration() {
    }

    public Map<String, List<String>> getComments() {
        return comments;
    }

    public List<String> getComments(String path) {
        return this.comments.containsKey(path) ? new ArrayList<>(this.comments.get(path)) : new ArrayList<String>();
    }

    public void setNewLinePerKey(boolean newLinePerKey) {
        this.newLinePerKey = newLinePerKey;
    }

    public boolean shouldAddNewLinePerKey() {
        return this.newLinePerKey;
    }

    public String getPathToComment(List<String> configContents, int lineIndex, String configLine) {
        if (configContents != null && lineIndex >= 0 && lineIndex < configContents.size() - 1 && configLine != null) {
            String trimmedConfigLine = trimPrefixSpaces(configLine);
            if (trimmedConfigLine.startsWith("#")) {
                int currentIndex = lineIndex;
                while (currentIndex < configContents.size() - 1) {
                    currentIndex++;
                    String currentLine = configContents.get(currentIndex);
                    String trimmedCurrentLine = trimPrefixSpaces(currentLine);
                    if (!trimmedCurrentLine.startsWith("#")) {
                        if (trimmedCurrentLine.contains(":")) {
                            return getPathToKey(configContents, currentIndex, currentLine);
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        return null;
    }

    public String getPathToKey(List<String> configContents, int lineIndex, String configLine) {
        if (configContents != null && lineIndex >= 0 && lineIndex < configContents.size() && configLine != null) {
            if (!configLine.startsWith("#") && configLine.contains(":")) {
                int spacesBeforeKey = getPrefixSpaceCount(configLine);
                String key = trimPrefixSpaces(configLine.substring(0, configLine.indexOf(':')));
                if (spacesBeforeKey > 0) {
                    int currentIndex = lineIndex;
                    int previousSpacesBeforeCurrentLine = -1;
                    boolean atZeroSpaces = false;

                    while (currentIndex > 0) {
                        currentIndex--;
                        String currentLine = configContents.get(currentIndex);
                        int spacesBeforeCurrentLine = getPrefixSpaceCount(currentLine);
                        if (trim(currentLine).isEmpty())
                            break;
                        if (!trim(currentLine).startsWith("#")) {
                            if (spacesBeforeCurrentLine < spacesBeforeKey) {
                                if (currentLine.contains(":")) {
                                    if (spacesBeforeCurrentLine > 0 || !atZeroSpaces) {
                                        if (spacesBeforeCurrentLine == 0)
                                            atZeroSpaces = true;
                                        if (previousSpacesBeforeCurrentLine == -1
                                                || spacesBeforeCurrentLine < previousSpacesBeforeCurrentLine) {
                                            previousSpacesBeforeCurrentLine = spacesBeforeCurrentLine;
                                            key = trimPrefixSpaces(currentLine.substring(0, currentLine.indexOf(":")))
                                                    + "." + key;
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                return key;
            }
        }
        return null;
    }

    public int getPrefixSpaceCount(String aString) {
        int spaceCount = 0;
        if (aString != null && aString.contains(" ")) {
            for (char aCharacter : aString.toCharArray()) {
                if (aCharacter == ' ')
                    spaceCount++;
                else
                    break;
            }
        }
        return spaceCount;
    }

    public String trim(String aString) {
        return aString != null ? aString.trim().replace(System.lineSeparator(), "") : "";
    }

    public String trimPrefixSpaces(String aString) {
        if (aString != null) {
            while (aString.startsWith(" "))
                aString = aString.substring(1);
        }
        return aString;
    }
}
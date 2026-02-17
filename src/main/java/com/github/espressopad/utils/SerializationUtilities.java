package com.github.espressopad.utils;

import com.github.espressopad.models.SettingsModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

public class SerializationUtilities {
    private Path classPath;
    private final Path importsFile;
    private final Path artifactFile;
    private final Path settingsFile;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public SerializationUtilities() {
        try {
            this.classPath = Path.of(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        } catch (URISyntaxException e) {
            this.logger.warn("{}. Defaulting to user.dir property.", e.getLocalizedMessage());
            this.classPath = Path.of(System.getProperty("user.dir"));
        }
        this.importsFile = this.classPath.resolve("imports");
        this.artifactFile = this.classPath.resolve("artifacts");
        this.settingsFile = this.classPath.resolve("settings");
    }

    public Path getImportsFile() {
        return this.importsFile;
    }

    public Path getArtifactFile() {
        return this.artifactFile;
    }

    public void writeArtifactXml(List<String> artifactList) {
        try (OutputStream outputStream = Files.newOutputStream(this.artifactFile);
             ObjectOutputStream objectStream = new ObjectOutputStream(outputStream)) {
            objectStream.writeObject(artifactList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> parseArtifactXml() {
        try (InputStream inputStream = Files.newInputStream(this.artifactFile);
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (List<String>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeImportXml(List<String> importList) {
        try (OutputStream outputStream = Files.newOutputStream(this.importsFile);
             ObjectOutputStream objectStream = new ObjectOutputStream(outputStream)) {
            objectStream.writeObject(importList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> parseImportXml() {
        try (InputStream inputStream = Files.newInputStream(this.importsFile);
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (List<String>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeSettingsXml(SettingsModel settings) {
        try (OutputStream outputStream = Files.newOutputStream(this.settingsFile);
             ObjectOutputStream objectStream = new ObjectOutputStream(outputStream)) {
            objectStream.writeObject(settings);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SettingsModel parseSettingsXml() {
        try (InputStream inputStream = Files.newInputStream(this.settingsFile);
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (SettingsModel) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            if (e instanceof NoSuchFileException)
                return null;
            throw new RuntimeException(e);
        }
    }
}

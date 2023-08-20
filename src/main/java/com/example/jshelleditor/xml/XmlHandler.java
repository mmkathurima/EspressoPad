package com.example.jshelleditor.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class XmlHandler {
    private final File importsFile = Path.of(System.getProperty("user.dir"), "imports.xml").toFile();
    private final File artifactFile = Path.of(System.getProperty("user.dir"), "artifacts.xml").toFile();

    public File getImportsFile() {
        return importsFile;
    }

    public File getArtifactFile() {
        return artifactFile;
    }

    public Document initDocument(File file) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document document = builder.parse(file);
        document.getDocumentElement().normalize();
        return document;
    }

    private List<String> checkDuplicateChildNodes(List<String> elements, Node node) {
        NodeList importChildren = node.getChildNodes();
        List<String> importStream = new ArrayList<>();
        for (int i = 0; i < importChildren.getLength(); i++)
            importStream.add(importChildren.item(i).getTextContent());

        List<String> filteredImports = new ArrayList<>(elements);
        filteredImports.removeAll(importStream);
        return filteredImports;
    }

    public void saveXmlChanges(File file, Document document) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        DOMSource domSource = new DOMSource(document);
        StreamResult sr = new StreamResult(file);
        transformer.transform(domSource, sr);
    }

    public void writeArtifactXml(List<String> artifactList) {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document = builder.newDocument();
            Node node = document.getElementsByTagName("artifacts").item(0);

            if (artifactFile.exists() && node != null) {
                document = builder.parse(artifactFile);

                for (String artifacts : this.checkDuplicateChildNodes(artifactList, node)) {
                    Element lib = document.createElement("artifact");
                    lib.appendChild(document.createTextNode(artifacts));
                    node.appendChild(lib);
                }
            } else {
                Element root = document.createElement("component");
                document.appendChild(root);

                Element artifact = document.createElement("artifacts");
                root.appendChild(artifact);

                for (String artifacts : artifactList) {
                    Element lib = document.createElement("artifact");
                    lib.appendChild(document.createTextNode(artifacts));
                    artifact.appendChild(lib);
                }
            }

            this.saveXmlChanges(this.artifactFile, document);
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> parseArtifactXml() {
        try {
            NodeList artifacts = this.initDocument(this.artifactFile).getElementsByTagName("artifact");
            List<String> artifactList = new ArrayList<>();
            for (int i = 0; i < artifacts.getLength(); i++)
                artifactList.add(artifacts.item(i).getTextContent());
            return artifactList;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeImportXml(List<String> importList) {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document;

            if (this.getImportsFile().exists()) {
                document = builder.parse(this.getImportsFile());
                Node node = document.getElementsByTagName("imports").item(0);

                if (node != null) {
                    List<String> distinctImports = this.checkDuplicateChildNodes(importList, node);
                    for (String importText : distinctImports) {
                        Element anImport = document.createElement("import");
                        anImport.appendChild(document.createTextNode(importText));
                        node.appendChild(anImport);
                    }
                    if (!distinctImports.isEmpty())
                        this.saveXmlChanges(this.importsFile, document);
                    return;
                }
            }

            document = builder.newDocument();
            Element root = document.createElement("component");
            document.appendChild(root);

            Element imports = document.createElement("imports");
            root.appendChild(imports);

            for (String importText : importList) {
                Element anImport = document.createElement("import");
                anImport.appendChild(document.createTextNode(importText));
                imports.appendChild(anImport);
            }

            this.saveXmlChanges(this.importsFile, document);
        } catch (ParserConfigurationException | SAXException | TransformerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> parseImportXml() {
        try {
            NodeList imports = this.initDocument(this.importsFile).getElementsByTagName("import");
            List<String> importList = new ArrayList<>();
            for (int i = 0; i < imports.getLength(); i++)
                importList.add(imports.item(i).getTextContent());
            return importList;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}

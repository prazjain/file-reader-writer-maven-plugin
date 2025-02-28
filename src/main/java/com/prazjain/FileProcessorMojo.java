package com.prazjain;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Goal which reads XML and YML files, processes them and writes the contents back to the same file unless output file path is provided.
 */
@Mojo( name = "process-file", defaultPhase = LifecyclePhase.NONE )
public class FileProcessorMojo
    extends AbstractMojo
{
    /**
     * The path to the file to be processed.
     */
    @Parameter(property = "input", required = true)
    private String input;

    @Parameter(property = "output")
    private String output;

    @Parameter(property = "overwrite", defaultValue = "false")
    private String overwriteString;

    public void execute() throws MojoExecutionException {
        if (input == null || input.trim().isEmpty()) {
            throw new MojoExecutionException("Input file path is required.");
        }

        File inputFile = new File(input);
        if (! inputFile.exists()) {
            throw new MojoExecutionException("Input file is not a file: " + inputFile.getAbsolutePath());
        }
        String fileExtension = input.substring(input.lastIndexOf(".") + 1).toLowerCase();

        if (!fileExtension.equals("xml") && !fileExtension.equals("yml") && !fileExtension.equals("yaml")) {
            throw new MojoExecutionException("Invalid file extension. Only .xml, .yml, or .yaml files are allowed.");
        }
        boolean overwrite = Boolean.parseBoolean(overwriteString);
        if (overwrite) {
            output = input;
        } else if (output ==null || output.trim().isEmpty()) {
            output = input.replace("." + fileExtension, "") + "-output." + fileExtension;
        }
        if (fileExtension.equals("xml")) {
            checkXmlCorrectness(inputFile, output);
        } else { // this must be yaml / yml file
            checkYamlCorrectness(inputFile);
        }
        try {
            // Read the file contents
            String content = new String(Files.readAllBytes(Paths.get(inputFile.getAbsolutePath())));
            // Log the file contents (you can modify or process the content here)
            getLog().info("File contents:");
            getLog().info(content);

            getLog().info("File processed successfully: " + inputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Error processing file " + inputFile.getAbsolutePath(), e);
        }
    }
    public static String prettyPrintByTransformer(String xmlString, int indent, boolean ignoreDeclaration) {

        try {
            InputSource src = new InputSource(new StringReader(xmlString));
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, ignoreDeclaration ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            Writer out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
        }
    }
    public static String prettyPrintByDom4j(String xmlString, int indent, boolean skipDeclaration) {
        try {
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setIndentSize(indent);
            format.setSuppressDeclaration(skipDeclaration);
            format.setEncoding("UTF-8");

            org.dom4j.Document document = DocumentHelper.parseText(xmlString);
            StringWriter sw = new StringWriter();
            XMLWriter writer = new XMLWriter(sw, format);
            writer.write(document);
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
        }
    }

    private void checkXmlCorrectness(File inputFile, String output) throws MojoExecutionException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);

            Document document = factory.newDocumentBuilder().parse(inputFile);

            // Validate XML syntax
            if (document.getDocumentElement() == null) {
                throw new MojoExecutionException("Invalid XML syntax in input file");
            }

            // Update XML file
            NodeList nodeList = document.getElementsByTagName("greeting");
            if (nodeList.getLength() > 0) {
                Element element = (Element) nodeList.item(0);
                element.setTextContent("Hej");
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            Writer out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            String str = prettyPrintByDom4j(out.toString(), 4, true);
            Files.write(Path.of(output), str.trim().getBytes() );
            System.out.println(str);
        } catch (ParserConfigurationException e) {
            throw new MojoExecutionException("Error parsing input file", e);
        } catch (SAXException e) {
            throw new MojoExecutionException("Invalid XML syntax in input file", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading input file", e);
        } catch (TransformerException e) {
            throw new MojoExecutionException("Error updating input file", e);
        }
    }

    private void checkYamlCorrectness(File inputFile) throws MojoExecutionException {
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            Yaml yaml = new Yaml();
            try {
                yaml.load(fis);
            } catch (YAMLException e) {
                throw new MojoExecutionException("Invalid YAML syntax in input file", e);
            }
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Error reading input file", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error parsing input file", e);
        }
    }
}

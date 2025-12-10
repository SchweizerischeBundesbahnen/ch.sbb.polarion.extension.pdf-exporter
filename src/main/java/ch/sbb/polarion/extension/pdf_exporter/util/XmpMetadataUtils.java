package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.UUID;

/**
 * Utility class for working with XMP metadata in PDF documents.
 * Provides common functionality for parsing, modifying, and serializing XMP metadata.
 */
@UtilityClass
public class XmpMetadataUtils {

    /**
     * Namespace URI for PDF/A identification.
     */
    public static final String NS_PDFAID = "http://www.aiim.org/pdfa/ns/id/";

    /**
     * Namespace URI for PDF/UA identification.
     */
    public static final String NS_PDFUAID = "http://www.aiim.org/pdfua/ns/id/";

    /**
     * Namespace URI for RDF.
     */
    public static final String NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /**
     * Namespace URI for Dublin Core.
     */
    public static final String NS_DC = "http://purl.org/dc/elements/1.1/";

    /**
     * Namespace URI for XMP.
     */
    public static final String NS_XMP = "http://ns.adobe.com/xap/1.0/";

    /**
     * Namespace URI for PDF.
     */
    public static final String NS_PDF = "http://ns.adobe.com/pdf/1.3/";

    /**
     * Creates a secure DocumentBuilderFactory with XXE protection enabled.
     * Disables external entity processing to prevent XXE (XML External Entity) attacks.
     *
     * @return a secure DocumentBuilderFactory instance
     * @throws ParserConfigurationException if configuration fails
     */
    public DocumentBuilderFactory createSecureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        // Disable external entity processing to prevent XXE attacks
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        return factory;
    }

    /**
     * Creates a secure TransformerFactory with XXE protection enabled.
     * Disables external entity processing to prevent XXE (XML External Entity) attacks.
     *
     * @return a secure TransformerFactory instance
     * @throws TransformerException if configuration fails
     */
    public TransformerFactory createSecureTransformerFactory() throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();

        // Disable external entity processing to prevent XXE attacks
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        return factory;
    }

    /**
     * Extracts RDF content from XMP metadata (removes xpacket processing instructions).
     *
     * @param xmpMetadata the full XMP metadata with xpacket tags
     * @return the RDF content without xpacket tags
     * @throws ParserConfigurationException if XML parser cannot be configured
     * @throws IOException if XML parsing fails
     * @throws SAXException if XML is malformed
     * @throws TransformerException if XML transformation fails
     */
    public String extractRdfContent(@NotNull String xmpMetadata) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(new InputSource(new StringReader(xmpMetadata)));

        TransformerFactory transformerFactory = createSecureTransformerFactory();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document.getDocumentElement()), new StreamResult(writer));

        return writer.toString().trim();
    }

    /**
     * Wraps RDF content with XMP packet processing instructions.
     * Generates a unique packet ID using UUID.
     * <p>
     * According to XMP Specification (ISO 16684-1), the xpacket begin attribute must contain
     * the Unicode BOM (U+FEFF) to indicate UTF-8 encoding for PDF/A compliance.
     *
     * @param rdfContent the RDF content to wrap
     * @return the complete XMP metadata with xpacket tags
     */
    @NotNull
    public String wrapWithXPacket(@NotNull String rdfContent) {
        String packetId = UUID.randomUUID().toString().replace("-", "");
        return "<?xpacket begin=\"\uFEFF\" id=\"" + packetId + "\"?>\n" +
               rdfContent + "\n" +
               "<?xpacket end=\"r\"?>";
    }

    /**
     * Converts XML Document to string.
     *
     * @param doc the XML document
     * @return the XML as string
     * @throws TransformerException if transformation fails
     */
    public String documentToString(@NotNull Document doc) throws TransformerException {
        TransformerFactory transformerFactory = createSecureTransformerFactory();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Gets attribute or child element value from element, handling namespace prefixes.
     * <p>
     * In XMP/RDF, property values can be expressed either as attributes or as child elements.
     * This method checks both forms.
     *
     * @param element       the XML element
     * @param attributeName the attribute/element name (may include namespace prefix like "pdfaid:part")
     * @return the attribute/element value, or null if not found
     */
    @Nullable
    public String getAttributeValue(@NotNull Element element, @NotNull String attributeName) {
        // Try as attribute with namespace prefix
        if (element.hasAttribute(attributeName)) {
            return element.getAttribute(attributeName);
        }

        // Try without prefix (in case of namespace URI)
        String[] parts = attributeName.split(":");
        if (parts.length == 2) {
            String namespaceURI = getNamespaceUriForPrefix(parts[0]);
            if (namespaceURI != null) {
                if (element.hasAttributeNS(namespaceURI, parts[1])) {
                    return element.getAttributeNS(namespaceURI, parts[1]);
                }

                // Try as child element (XMP/RDF allows property values as child elements)
                NodeList children = element.getElementsByTagNameNS(namespaceURI, parts[1]);
                if (children.getLength() > 0) {
                    return children.item(0).getTextContent();
                }

                // Also try without namespace for child elements
                children = element.getElementsByTagName(attributeName);
                if (children.getLength() > 0) {
                    return children.item(0).getTextContent();
                }
            }
        }

        return null;
    }

    /**
     * Sets a property value on an element, handling both attribute and child element forms.
     * <p>
     * If the property exists as a child element, it will be updated. Otherwise, it will be set as an attribute.
     *
     * @param element       the XML element
     * @param attributeName the attribute/element name (may include namespace prefix)
     * @param value         the value to set
     */
    public void setPropertyValue(@NotNull Element element, @NotNull String attributeName, @NotNull String value) {
        String[] parts = attributeName.split(":");
        if (parts.length == 2) {
            String namespaceURI = getNamespaceUriForPrefix(parts[0]);
            if (namespaceURI != null) {
                // Check if child element exists
                NodeList children = element.getElementsByTagNameNS(namespaceURI, parts[1]);
                if (children.getLength() > 0) {
                    children.item(0).setTextContent(value);
                    return;
                }

                // Also check without namespace
                children = element.getElementsByTagName(attributeName);
                if (children.getLength() > 0) {
                    children.item(0).setTextContent(value);
                    return;
                }
            }
        }

        // Set as attribute (default behavior)
        element.setAttribute(attributeName, value);
    }

    /**
     * Removes a property from an element, handling both attribute and child element forms.
     *
     * @param element       the XML element
     * @param attributeName the attribute/element name (may include namespace prefix)
     */
    public void removeProperty(@NotNull Element element, @NotNull String attributeName) {
        // Remove attribute
        if (element.hasAttribute(attributeName)) {
            element.removeAttribute(attributeName);
        }

        // Remove child elements
        String[] parts = attributeName.split(":");
        if (parts.length == 2) {
            String namespaceURI = getNamespaceUriForPrefix(parts[0]);
            if (namespaceURI != null) {
                NodeList children = element.getElementsByTagNameNS(namespaceURI, parts[1]);
                while (children.getLength() > 0) {
                    element.removeChild(children.item(0));
                }

                children = element.getElementsByTagName(attributeName);
                while (children.getLength() > 0) {
                    element.removeChild(children.item(0));
                }
            }
        }
    }

    /**
     * Checks if a property exists on an element, either as attribute or child element.
     *
     * @param element       the XML element
     * @param attributeName the attribute/element name (may include namespace prefix)
     * @return true if the property exists
     */
    public boolean hasProperty(@NotNull Element element, @NotNull String attributeName) {
        return getAttributeValue(element, attributeName) != null;
    }

    /**
     * Returns namespace URI for common XMP prefixes.
     *
     * @param prefix the namespace prefix (e.g., "pdfaid", "pdfuaid", "pdf", "rdf")
     * @return the namespace URI, or null if unknown prefix
     */
    @Nullable
    public String getNamespaceUriForPrefix(@NotNull String prefix) {
        return switch (prefix) {
            case "pdfaid" -> NS_PDFAID;
            case "pdfuaid" -> NS_PDFUAID;
            case "pdf" -> NS_PDF;
            case "rdf" -> NS_RDF;
            case "dc" -> NS_DC;
            case "xmp" -> NS_XMP;
            default -> null;
        };
    }

    /**
     * Parses XMP metadata XML and returns the Document.
     *
     * @param rdfContent the RDF content (without xpacket tags)
     * @return the parsed XML Document
     * @throws ParserConfigurationException if XML parser cannot be configured
     * @throws IOException if XML parsing fails
     * @throws SAXException if XML is malformed
     */
    public Document parseXml(@NotNull String rdfContent) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(rdfContent)));
    }

    /**
     * Gets all rdf:Description elements from the document.
     *
     * @param document the XML document
     * @return NodeList of rdf:Description elements
     */
    public NodeList getRdfDescriptions(@NotNull Document document) {
        return document.getElementsByTagNameNS(NS_RDF, "Description");
    }
}

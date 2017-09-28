package com.shaposhnyk.unilines.map;

import com.shaposhnyk.unilines.UBiPipeline;
import com.shaposhnyk.unilines.UField;
import com.shaposhnyk.unilines.builders.UCField;
import com.shaposhnyk.unilines.builders.UCObjects;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Converting from POJO objects to a map example. This is also can be used to produce a JSON from a map
 */
public class AdToXmlTest extends ConverterBase {
    UField root = UField.Factory.of("root");
    UField item = UField.Factory.of("ldapItem");

    @Test
    public void attributeCreated() {
        UBiPipeline<SearchResult, Element> converter = UCObjects.Builder.of(item)
                .ofSourceType(SearchResult.class)
                .ofContextMapF((UField f, Element xEl) -> newChildElement(f, xEl))
                .field(xmlAttribute("myField", (SearchResult s) -> s.getName())
                        .decorateJ(s -> s.toUpperCase()))
                .build();

        Document xDoc = newDocument();
        Element xRoot = createRootElement(root, xDoc);

        SearchResult sr = new SearchResult("SomeName", "SomeObj", null);
        converter.consume(sr, xRoot);

        Element ldapItem = (Element) xRoot.getChildNodes().item(0);
        Assert.assertThat(ldapItem.getNodeName(), equalTo("ldapItem"));
        Assert.assertThat(ldapItem.getChildNodes().getLength(), equalTo(0));

        Assert.assertThat(ldapItem.getAttribute("myField"), equalTo("SOMENAME"));
    }

    @Test
    public void attributeIgnoredIfNull() {
        UBiPipeline<SearchResult, Element> converter = UCObjects.Builder.of(item)
                .ofSourceType(SearchResult.class)
                .ofContextMapF((UField f, Element xEl) -> newChildElement(f, xEl))
                .field(xmlAttribute("unexistedField", "myField")
                        .decorateJ(s -> s.toUpperCase()))
                .build();

        Document xDoc = newDocument();
        Element xRoot = createRootElement(root, xDoc);

        SearchResult sr = new SearchResult("SomeName", "SomeObj", new BasicAttributes());
        converter.consume(sr, xRoot);

        Element ldapItem = (Element) xRoot.getChildNodes().item(0);
        Assert.assertThat(ldapItem.getNodeName(), equalTo("ldapItem"));
        Assert.assertThat(ldapItem.getChildNodes().getLength(), equalTo(0));

        Assert.assertThat(ldapItem.hasAttribute("myField"), equalTo(false));
    }

    @Test
    public void errorsReportedInStackTrace() {
        UBiPipeline<SearchResult, Element> converter = UCObjects.Builder.of(item)
                .ofSourceType(SearchResult.class)
                .ofContextMapF((UField f, Element xEl) -> newChildElement(f, xEl))
                .field(xmlAttribute("unexistedField", "myField")
                        .decorateJ(s -> s.toUpperCase()))
                .build();

        Document xDoc = newDocument();
        Element xRoot = createRootElement(root, xDoc);

        SearchResult sr = new SearchResult("SomeName", "SomeObj", null);

        try {
            converter.consume(sr, xRoot);
        } catch (Exception e) {
            Assert.assertThat(e.getStackTrace()[0].getMethodName(), equalTo("generated"));
            Assert.assertThat(e.getStackTrace()[0].getFileName(), containsString("SimpleImmutableField"));
            Assert.assertThat(e.getStackTrace()[0].getFileName(), containsString("ldapItem"));
        }
    }

    @Test
    public void elementCreated() {
        UBiPipeline<SearchResult, Element> converter = UCObjects.Builder.of(item)
                .ofSourceType(SearchResult.class)
                .ofContextMapF((UField f, Element xEl) -> newChildElement(f, xEl))
                .field(xmlElement("myField", (SearchResult s) -> s.getName())
                        .decorateJ(s -> s.toUpperCase()))
                .build();

        Document xDoc = newDocument();
        Element xRoot = createRootElement(root, xDoc);

        SearchResult sr = new SearchResult("SomeName", "SomeObj", null);
        converter.consume(sr, xRoot);

        Element ldapItem = (Element) xRoot.getChildNodes().item(0);
        Assert.assertThat(ldapItem.getNodeName(), equalTo("ldapItem"));
        Assert.assertThat(ldapItem.getChildNodes().getLength(), equalTo(1));

        Assert.assertThat(ldapItem.getChildNodes().item(0).getNodeName(), equalTo("myField"));
        Assert.assertThat(ldapItem.getChildNodes().item(0).getTextContent(), equalTo("SOMENAME"));
    }

    @Test
    public void elementIgnoredIfNull() {
        UBiPipeline<SearchResult, Element> converter = UCObjects.Builder.of(item)
                .ofSourceType(SearchResult.class)
                .ofContextMapF((UField f, Element xEl) -> newChildElement(f, xEl))
                .field(xmlElement("myField", (SearchResult s) -> s.getName())
                        .decorateJ(s -> s.toUpperCase()))
                .build();

        Document xDoc = newDocument();
        Element xRoot = createRootElement(root, xDoc);

        SearchResult sr = new SearchResult(null, "SomeObj", new BasicAttributes());
        converter.consume(sr, xRoot);

        Element ldapItem = (Element) xRoot.getChildNodes().item(0);
        Assert.assertThat(ldapItem.getNodeName(), equalTo("ldapItem"));
        Assert.assertThat(ldapItem.getChildNodes().getLength(), equalTo(0));
    }

    @Test
    public void convertMultipleObjects() {
        UBiPipeline<Map<String, String>, Document> converter = adQueryConverter();
        Map<String, String> ctx = new ConcurrentHashMap<>();
        ctx.put("q", "brian|harry");

        Document doc = newDocument();
        converter.consume(ctx, doc);

        Element xRoot = doc.getDocumentElement();

        // asserts on xml are too verbose
        Assert.assertThat(xRoot.getNodeName(), equalTo("results"));

        Element brian = (Element) xRoot.getChildNodes().item(0);
        Assert.assertThat(brian.getNodeName(), equalTo("ldapItem"));
        Assert.assertThat(brian.getAttribute("id"), equalTo("CN=Brian Goetz,DC=shaposhnyk,DC=com"));

        Element harry = (Element) xRoot.getChildNodes().item(1);
        Assert.assertThat(harry.getNodeName(), equalTo("ldapItem"));
        Assert.assertThat(xRoot.getChildNodes().getLength(), equalTo(2));


        // note that there is no name tag
        Assert.assertThat(brian.getChildNodes().item(0).getNodeName(), equalTo("login"));
        Assert.assertThat(brian.getChildNodes().item(0).getTextContent(), equalTo("Goetz")); // phone

        Assert.assertThat(brian.getChildNodes().item(1).getNodeName(), equalTo("phoneNo"));
        Assert.assertThat(brian.getChildNodes().item(1).getTextContent(), equalTo("+18885551234")); // phone

        Assert.assertThat(brian.getChildNodes().item(3).getNodeName(), equalTo("email"));
        Assert.assertThat(brian.getChildNodes().item(3).getTextContent(), equalTo("brian.goetz@shaposhnyk.com")); // phone

        Assert.assertThat(brian.getChildNodes().getLength(), equalTo(4));

        Element harryNames = (Element) harry.getChildNodes().item(0);
        Assert.assertThat(harryNames.getNodeName(), equalTo("names"));
        Assert.assertThat(harryNames.getAttribute("initials"), equalTo("HP"));
        Assert.assertThat(harryNames.getChildNodes().item(0).getNodeName(), equalTo("displayName"));

        Assert.assertThat(harry.getChildNodes().getLength(), equalTo(3));

        System.out.println("multi: " + prettyString(doc));
    }

    @Test
    public void objectsCanBeIterated() {
        Map<String, String> params = new HashMap<>();

        UBiPipeline<Map<String, String>, Document> conv = UCObjects.Builder.of(root)
                .ofSourceType(params)
                .ofContextType(Document.class)
                .ofContextMapF(this::createRootElement)
                .flatMap(query -> queryLdap(query))
                .pipeTo(
                        UCObjects.Builder.of(item)
                                .ofSourceType(SearchResult.class)
                                .ofContextMapF(this::newChildElement)
                                .build()
                );

        Document doc = newDocument();
        conv.consume(params, doc);

        Element xRoot = doc.getDocumentElement();

        Assert.assertThat(xRoot.getNodeName(), equalTo("root"));
        Assert.assertThat(xRoot.getChildNodes().item(0).getNodeName(), equalTo("ldapItem"));
        Assert.assertThat(xRoot.getChildNodes().item(1).getNodeName(), equalTo("ldapItem"));
        Assert.assertThat(xRoot.getChildNodes().getLength(), equalTo(2));

        System.out.println("iter: " + prettyString(doc));
    }


    private UBiPipeline<Map<String, String>, Document> adQueryConverter() {
        UField root = UField.Factory.of("q", "results");
        UField nameObj = UField.Factory.of("name", "names");
        Map<String, String> params = new HashMap<>();

        return UCObjects.Builder.of(root)
                .ofSourceType(params)
                .ofContextType(Document.class)
                .ofContextMapF((UField f, Document xDoc) -> createRootElement(f, xDoc))
                .flatMap(query -> queryLdap(query))
                .pipeTo(
                        UCObjects.Builder.of(item)
                                .ofSourceType(SearchResult.class)
                                .ofContextMapF((UField f, Element xEl) -> newChildElement(f, xEl))
                                .field(xmlAttribute("id", (SearchResult s) -> s.getName()))
                                .field(xmlAttribute("type", (SearchResult s) -> s.getObject()))
                                .field(xmlElement("sAMAccountName", "login"))
                                .field(
                                        UCObjects.Builder.of(nameObj)
                                                .ofSourceType(SearchResult.class)
                                                .ofContextMapF(this::newChildElement)
                                                .field(xmlAttribute("initials", "initials"))
                                                .field(xmlElement("displayName", "displayName"))
                                                .field(
                                                        UCField.Builder
                                                                .contextMapperOf(
                                                                        UField.Factory.of("postProcessor"),
                                                                        this::detachIfEmpty)
                                                )
                                                .build()
                                )
                                .field(
                                        xmlElement("telephoneNumber", "phoneNo")
                                                .decorateJ(s -> s.replace(" ", ""))
                                )
                                .field(xmlElement("cn", "fullName"))
                                .field(xmlElement("mail", "email").decorateJ(String::toLowerCase))
                                .build()
                );
    }

    private void detachIfEmpty(Element xEl) {
        if (!xEl.hasChildNodes()) {
            xEl.getParentNode().removeChild(xEl);
        }
    }


    private Element createRootElement(UField f, Document xDoc) {
        Element xRoot = xDoc.createElement(f.externalName());
        xDoc.appendChild(xRoot);
        return xRoot;
    }

    private Document newDocument() {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            return null;
        }
    }

    private <T> UCField.UExtracting<SearchResult, Element, T> xmlAttribute(String intName, Function<SearchResult, T> extractor) {
        UField field = UField.Factory.of(intName, intName);
        return UCField.Builder.fUniExtractingOf(field,
                (UField f, SearchResult sr) -> extractor.apply(sr))
                .withWriterJF(this::writeAsXmlAttribute);
    }

    private UCField.UExtracting<SearchResult, Element, String> xmlAttribute(String intName, String extName) {
        UField field = UField.Factory.of(intName, extName);
        return UCField.Builder.fUniExtractingOf(field,
                (UField f, SearchResult sr) -> sr.getAttributes().get(f.internalName()))
                .withWriterJF(this::writeAsXmlAttribute)
                .mapJ((Attribute a) -> getStringOrNull(a));
    }

    private <T> UCField.UExtracting<SearchResult, Element, T> xmlElement(String intName, Function<SearchResult, T> extractor) {
        UField field = UField.Factory.of(intName, intName);
        return UCField.Builder.fUniExtractingOf(field,
                (UField f, SearchResult sr) -> extractor.apply(sr))
                .withWriterJF(this::writeAsXmlElement);
    }

    private UCField.UExtracting<SearchResult, Element, String> xmlElement(String intName, String extName) {
        UField field = UField.Factory.of(intName, extName);
        return UCField.Builder.fUniExtractingOf(field,
                (UField f, SearchResult sr) -> sr.getAttributes().get(f.internalName()))
                .mapJ((Attribute a) -> getStringOrNull(a))
                .withWriterJF(this::writeAsXmlElement);
    }

    private String getStringOrNull(Attribute a) {
        try {
            Object objValue = a.get();
            return objValue != null ? objValue.toString() : null;
        } catch (NamingException e) {
            return null;
        }
    }

    private void writeAsXmlAttribute(UField f, Object source, Element xEl) {
        xEl.setAttribute(f.externalName(), source.toString());
    }

    private void writeAsXmlElement(UField f, Object source, Element xEl) {
        newChildElement(f, xEl).setTextContent(source.toString());
    }

    private Element newChildElement(UField f, Element xEl) {
        Element newChild = xEl.getOwnerDocument().createElement(f.externalName());
        xEl.appendChild(newChild);
        return newChild;
    }

    private String prettyString(Document doc) {
        StringWriter stringWriter = new StringWriter();
        StreamResult xmlOutput = new StreamResult(stringWriter);
        TransformerFactory tf = TransformerFactory.newInstance();

        try {
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(doc), xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private Iterable<SearchResult> queryLdap(Map<String, String> queryParams) {
        String q = queryParams.get("q");
        String group = queryParams.get("group");
        String ldapFilter = "(|(q=" + q + ")(member=" + group + ")"; // should be escaped
        return queryLdap(ldapFilter);
    }

    private Iterable<SearchResult> queryLdap(String ldapFilter) {
        return Arrays.asList(ldapResult("Brian", "Goetz", 1234), ldapResult("Harry", "Potter", 5678));
    }

    private SearchResult ldapResult(String first, String last, int num) {
        BasicAttributes attrs = new BasicAttributes();
        attrs.put("cn", first + " " + last);
        attrs.put("givenName", first);
        attrs.put("telephoneNumber", "+1 888 555 " + num);
        if (first.startsWith("B")) {
            attrs.put("sAMAccountName", last);
            attrs.put("mail", first + "." + last + "@shaposhnyk.com");
        } else {
            attrs.put("initials", "HP");
            attrs.put("displayName", "wizard");
        }
        return new SearchResult("CN=" + first + " " + last + ",DC=shaposhnyk,DC=com", "person", attrs);
    }
}

package net.corda.spi.weaving.test;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class XMLFactoryTest {
    private static final Logger LOG = LoggerFactory.getLogger(XMLFactoryTest.class);

    @Test
    void testXMLInputFactory() {
        Object factory = XMLInputFactory.newFactory();
        assertInstanceOf(XMLInputFactory.class, factory);
        LOG.info("XMLInputFactory: {}", factory);
    }

    @Test
    void testXMLInputFactoryWithClassLoader() {
        Object factory = XMLInputFactory.newFactory(XMLInputFactory.class.getName(), getClass().getClassLoader());
        assertInstanceOf(XMLInputFactory.class, factory);
        LOG.info("XMLInputFactory(ClassLoader): {}", factory);
    }

    @Test
    void testXMLOutputFactory() {
        Object factory = XMLOutputFactory.newFactory();
        assertInstanceOf(XMLOutputFactory.class, factory);
        LOG.info("XMLOutputFactory: {}", factory);
    }

    @Test
    void testXMLOutputFactoryWithClassLoader() {
        Object factory = XMLOutputFactory.newFactory(XMLOutputFactory.class.getName(), getClass().getClassLoader());
        assertInstanceOf(XMLOutputFactory.class, factory);
        LOG.info("XMLOutputFactory(ClassLoader): {}", factory);
    }

    @Test
    void testXMLEventFactory() {
        Object factory = XMLEventFactory.newFactory();
        assertInstanceOf(XMLEventFactory.class, factory);
        LOG.info("XMLEventFactory: {}", factory);
    }

    @Test
    void testXMLEventFactoryWithClassLoader() {
        Object factory = XMLEventFactory.newFactory(XMLEventFactory.class.getName(), getClass().getClassLoader());
        assertInstanceOf(XMLEventFactory.class, factory);
        LOG.info("XMLEventFactory(ClassLoader): {}", factory);
    }
}

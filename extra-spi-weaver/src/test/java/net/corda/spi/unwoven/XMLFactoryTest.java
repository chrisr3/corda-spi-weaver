package net.corda.spi.unwoven;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XMLFactoryTest {
    private static final Logger LOG = LoggerFactory.getLogger(XMLFactoryTest.class);

    @Test
    void testXMLInputFactory() {
        Object factory = XMLInputFactory.newFactory();
        assertInstanceOf(XMLInputFactory.class, factory);
        assertFromBundle(null, factory);
        LOG.info("UNWOVEN XMLInputFactory: {}", factory);
    }

    @Test
    void testXMLInputFactoryWithClassLoader() {
        assertThrows(FactoryConfigurationError.class, () ->
            XMLInputFactory.newFactory(XMLInputFactory.class.getName(), getClass().getClassLoader()));
    }

    @Test
    void testXMLOutputFactory() {
        Object factory = XMLOutputFactory.newFactory();
        assertInstanceOf(XMLOutputFactory.class, factory);
        assertFromBundle(null, factory);
        LOG.info("UNWOVEN XMLOutputFactory: {}", factory);
    }

    @Test
    void testXMLOutputFactoryWithClassLoader() {
        assertThrows(FactoryConfigurationError.class, () ->
            XMLOutputFactory.newFactory(XMLOutputFactory.class.getName(), getClass().getClassLoader()));
    }

    @Test
    void testXMLEventFactory() {
        Object factory = XMLEventFactory.newFactory();
        assertInstanceOf(XMLEventFactory.class, factory);
        assertFromBundle(null, factory);
        LOG.info("UNWOVEN XMLEventFactory: {}", factory);
    }

    @Test
    void testXMLEventFactoryWithClassLoader() {
        assertThrows(FactoryConfigurationError.class, () ->
            XMLEventFactory.newFactory(XMLEventFactory.class.getName(), getClass().getClassLoader()));
    }

    @SuppressWarnings("SameParameterValue")
    private static void assertFromBundle(Bundle bundle, Object obj) {
        assertEquals(bundle, FrameworkUtil.getBundle(obj.getClass()));
    }
}

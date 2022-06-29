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

@SuppressWarnings("deprecation")
class XMLInstanceTest {
    private static final Logger LOG = LoggerFactory.getLogger(XMLInstanceTest.class);

    @Test
    void testXMLInputFactory() {
        Object instance = XMLInputFactory.newInstance();
        assertInstanceOf(XMLInputFactory.class, instance);
        assertFromBundle(null, instance);
        LOG.info("UNWOVEN XMLInputFactory#newInstance(): {}", instance);
    }

    @Test
    void testXMLInputFactoryWithClassLoader() {
        assertThrows(FactoryConfigurationError.class, () ->
            XMLInputFactory.newInstance(XMLInputFactory.class.getName(), getClass().getClassLoader()));
    }

    @Test
    void testXMLOutputFactory() {
        Object instance = XMLOutputFactory.newInstance();
        assertInstanceOf(XMLOutputFactory.class, instance);
        assertFromBundle(null, instance);
        LOG.info("UNWOVEN XMLOutputFactory#newInstance(): {}", instance);
    }

    @Test
    void testXMLOutputFactoryWithClassLoader() {
        assertThrows(FactoryConfigurationError.class, () ->
            XMLOutputFactory.newInstance(XMLOutputFactory.class.getName(), getClass().getClassLoader()));
    }

    @Test
    void testXMLEventFactory() {
        Object instance = XMLEventFactory.newInstance();
        assertInstanceOf(XMLEventFactory.class, instance);
        assertFromBundle(null, instance);
        LOG.info("UNWOVEN XMLEventFactory#newInstance(): {}", instance);
    }

    @Test
    void testXMLEventFactoryWithClassLoader() {
        assertThrows(FactoryConfigurationError.class, () ->
            XMLEventFactory.newInstance(XMLEventFactory.class.getName(), getClass().getClassLoader()));
    }

    @SuppressWarnings("SameParameterValue")
    private static void assertFromBundle(Bundle bundle, Object obj) {
        assertEquals(bundle, FrameworkUtil.getBundle(obj.getClass()));
    }
}

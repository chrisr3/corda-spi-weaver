package net.corda.spi.woven;

import org.junit.jupiter.api.Test;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SuppressWarnings("deprecation")
class XMLInstanceTest {
    private static final String WOODSTOX_BSN = "com.fasterxml.woodstox.woodstox-core";
    private static final Logger LOG = LoggerFactory.getLogger(XMLInstanceTest.class);

    @Test
    void testXMLInputFactory() {
        Object instance = XMLInputFactory.newInstance();
        assertInstanceOf(XMLInputFactory.class, instance);
        assertFromBundle(WOODSTOX_BSN, instance);
        LOG.info("WOVEN XMLInputFactory#newInstance: {}", instance);
    }

    @Test
    void testXMLInputFactoryWithClassLoader() {
        Object instance = XMLInputFactory.newInstance(XMLInputFactory.class.getName(), getClass().getClassLoader());
        assertInstanceOf(XMLInputFactory.class, instance);
        assertFromBundle(WOODSTOX_BSN, instance);
        LOG.info("WOVEN XMLInputFactory#newInstance(ClassLoader): {}", instance);
    }

    @Test
    void testXMLOutputFactory() {
        Object instance = XMLOutputFactory.newInstance();
        assertInstanceOf(XMLOutputFactory.class, instance);
        assertFromBundle(WOODSTOX_BSN, instance);
        LOG.info("WOVEN XMLOutputFactory#newInstance: {}", instance);
    }

    @Test
    void testXMLOutputFactoryWithClassLoader() {
        // This XMLOutputFactory.newInstance API is WRONG, because it actually returns XMLInputFactory!
        Object instance = XMLOutputFactory.newInstance(XMLInputFactory.class.getName(), getClass().getClassLoader());
        assertInstanceOf(XMLInputFactory.class, instance);
        assertFromBundle(WOODSTOX_BSN, instance);
        LOG.info("WOVEN XMLOutputFactory#newInstance(ClassLoader): {}", instance);
    }

    @Test
    void testXMLEventFactory() {
        Object instance = XMLEventFactory.newInstance();
        assertInstanceOf(XMLEventFactory.class, instance);
        assertFromBundle(WOODSTOX_BSN, instance);
        LOG.info("WOVEN XMLEventFactory#newInstance: {}", instance);
    }

    @Test
    void testXMLEventFactoryWithClassLoader() {
        Object instance = XMLEventFactory.newInstance(XMLEventFactory.class.getName(), getClass().getClassLoader());
        assertInstanceOf(XMLEventFactory.class, instance);
        assertFromBundle(WOODSTOX_BSN, instance);
        LOG.info("WOVEN XMLEventFactory#newInstance(ClassLoader): {}", instance);
    }

    @SuppressWarnings("SameParameterValue")
    private static void assertFromBundle(String bsn, Object obj) {
        assertEquals(bsn, FrameworkUtil.getBundle(obj.getClass()).getSymbolicName());
    }
}

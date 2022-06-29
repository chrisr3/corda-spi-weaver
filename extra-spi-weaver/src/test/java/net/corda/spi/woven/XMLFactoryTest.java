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

class XMLFactoryTest {
    private static final String WOODSTOX_BSN = "com.fasterxml.woodstox.woodstox-core";
    private static final Logger LOG = LoggerFactory.getLogger(XMLFactoryTest.class);

    @Test
    void testXMLInputFactory() {
        Object factory = XMLInputFactory.newFactory();
        assertInstanceOf(XMLInputFactory.class, factory);
        assertFromBundle(WOODSTOX_BSN, factory);
        LOG.info("WOVEN XMLInputFactory#newFactory(): {}", factory);
    }

    @Test
    void testXMLInputFactoryWithClassLoader() {
        Object factory = XMLInputFactory.newFactory(XMLInputFactory.class.getName(), getClass().getClassLoader());
        assertInstanceOf(XMLInputFactory.class, factory);
        assertFromBundle(WOODSTOX_BSN, factory);
        LOG.info("WOVEN XMLInputFactory#newFactory(ClassLoader): {}", factory);
    }

    @Test
    void testXMLOutputFactory() {
        Object factory = XMLOutputFactory.newFactory();
        assertInstanceOf(XMLOutputFactory.class, factory);
        assertFromBundle(WOODSTOX_BSN, factory);
        LOG.info("WOVEN XMLOutputFactory#newFactory(): {}", factory);
    }

    @Test
    void testXMLOutputFactoryWithClassLoader() {
        Object factory = XMLOutputFactory.newFactory(XMLOutputFactory.class.getName(), getClass().getClassLoader());
        assertInstanceOf(XMLOutputFactory.class, factory);
        assertFromBundle(WOODSTOX_BSN, factory);
        LOG.info("WOVEN XMLOutputFactory#newFactory(ClassLoader): {}", factory);
    }

    @Test
    void testXMLEventFactory() {
        Object factory = XMLEventFactory.newFactory();
        assertInstanceOf(XMLEventFactory.class, factory);
        assertFromBundle(WOODSTOX_BSN, factory);
        LOG.info("WOVEN XMLEventFactory#newFactory(): {}", factory);
    }

    @Test
    void testXMLEventFactoryWithClassLoader() {
        Object factory = XMLEventFactory.newFactory(XMLEventFactory.class.getName(), getClass().getClassLoader());
        assertInstanceOf(XMLEventFactory.class, factory);
        assertFromBundle(WOODSTOX_BSN, factory);
        LOG.info("WOVEN XMLEventFactory#newFactory(ClassLoader): {}", factory);
    }

    @SuppressWarnings("SameParameterValue")
    private static void assertFromBundle(String bsn, Object obj) {
        assertEquals(bsn, FrameworkUtil.getBundle(obj.getClass()).getSymbolicName());
    }
}

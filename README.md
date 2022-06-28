# corda-spi-extra-weaver
An OSGi framework extension that weaves for SPI consumers not supported by [Apache Aries SPI-Fly](https://github.com/apache/aries).

Apache Aries can only support SPI consumers which _directly_ invoke `java.util.ServiceLoader`.

This extension supports additional weaving for the following SPI consumers:
```
javax.xml.stream.XMLInputFactory.newFactory
javax.xml.stream.XMLInputFactory.newInstance
javax.xml.stream.XMLOutputFactory.newFactory
javax.xml.stream.XMLOutputFactory.newInstance
javax.xml.stream.XMLEventFactory.newFactory
javax.xml.stream.XMLEventFactory.newInstance
```

This bundle is written in Java to avoid needing to include the Kotlin standard
libraries, since OSGi framework extensions cannot have dependencies.

## Configuration.

Add the symbolic names for bundles that need extra weaving to the `net.corda.spi-weaver.auto.consumers` framework property.

```
net.corda.spi-weaver.auto.consumers='bsn1,bsn2,...'
```


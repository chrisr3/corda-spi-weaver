# corda-spi-extra-weaver
An OSGi framework extension that weaves for SPI consumers not supported by [Apache Aries SPI-Fly](https://github.com/apache/aries).

Apache Aries can only support SPI consumers which _directly_ invoke `java.util.ServiceLoader`, as described in the
[OSGi Service Loader Mediator](http://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.loader.html) specification.

This extension supports additional weaving for the following _indirect_ SPI consumers:
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

## Installation

This extension must be installed into an OSGi framework alongside the Apache Aries SPI-Fly framework extension,
because it still depends on SPI-Fly to register the SPI services for consumption:
```
org.apache.aries.spifly:org.apache.aries.spifly.dynamic.framework.extension:${spiFlyVersion}
```
Note that we _do_ need `org.apache.aries.spifly.dynamic.framework.extension` as opposed to
`org.apache.aries.spifly.dynamic.bundle`, so that SPI-Fly becomes part of the system bundle. This prevents any problems relating to bundle initialisation order.

## How It Works

SPI-Fly identifies which bundles contain `META-INF/services/*` entries, and then registers instances of these as
OSGi services. Each such service is assigned a `serviceloader.mediator` property whose value is the bundle ID of
owner of the Mediator extension (i.e. the system bundle ID). This allows the `corda-extra-spi-weaver` to identify
which services correspond to SPI services.

The `DynamicExtraWeavingHook` detects any class belonging to a bundle listed by the `net.corda.spi-weaver.auto.consumers`
property, and then instruments any invocations of one of the `XMLInputFactory`, `XMLOutputFactory` or `XMLEventFactory`
methods which rely on `ServiceLoader`. Specifically, it rewrites the invocation byte-code to ensure that `ServiceLoader`
uses a `ClassLoader` capable of "seeing" the required SPI services. This instrumentation invokes `static` functions
defined in `net.corda.spi.weaving.Util`, and so the `WeavingHook` will also provide the bundle with an OSGi
`DynamicImport-Package` directive for the `net.corda.spi.weaving` package.


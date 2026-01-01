Jackson Converter
=================

A `Converter` which uses [Jackson][1] for serialization.

A default `ObjectMapper` instance will be created or one can be configured and passed to the
`JacksonConverterFactory` construction to further control the serialization.

Multi-Format Support
--------------------

This converter is not limited to JSON. Jackson supports many data formats through its
[dataformat modules][4], and this converter works with any of them by supplying the appropriate
mapper and media type.

### XML

```java
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://example.com/")
    .addConverterFactory(
        JacksonConverterFactory.create(
            new XmlMapper(),
            MediaType.get("application/xml")))
    .build();
```

Requires the `jackson-dataformat-xml` dependency.

### CBOR

```java
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://example.com/")
    .addConverterFactory(
        JacksonConverterFactory.create(
            new CBORMapper(),
            MediaType.get("application/cbor")))
    .build();
```

Requires the `jackson-dataformat-cbor` dependency.

### Other Formats

The same pattern applies to other Jackson dataformat modules such as YAML, Smile, Ion, and more.
Simply use the corresponding mapper (e.g., `YAMLMapper`, `SmileMapper`) and media type.


Download
--------

Download [the latest JAR][2] or grab via [Maven][3]:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>converter-jackson</artifactId>
  <version>latest.version</version>
</dependency>
```
or [Gradle][3]:
```groovy
implementation 'com.squareup.retrofit2:converter-jackson:latest.version'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].



 [1]: https://github.com/FasterXML/jackson
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=converter-jackson&v=LATEST
 [3]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.squareup.retrofit2%22%20a%3A%22converter-jackson%22
 [4]: https://github.com/FasterXML/jackson#data-format-modules
 [snap]: https://s01.oss.sonatype.org/content/repositories/snapshots/


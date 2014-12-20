rembedis [![Cloudbees DEV@cloud](http://www.cloudbees.com/sites/default/files/Button-Powered-by-CB.png)](http://www.cloudbees.com/)
==========
[![Build Status](https://vanek.ci.cloudbees.com/buildStatus/icon?job=rembedis-snapshot)](https://vanek.ci.cloudbees.com/job/rembedis-snapshot/)
[![Coverage Status](https://coveralls.io/repos/anthavio/rembedis/badge.png?branch=master)](https://coveralls.io/r/anthavio/rembedis?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.anthavio/rembedis/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.anthavio/rembedis)

Redis embedder done right!

So what is wrong with [embedded-redis](https://github.com/kstyrc/embedded-redis) and it's forks?
Well, main issue is that they leave redis-server processes hanging alive after JVM stops if you did not stopped them carefully.
With rembedis, you don't have to care about it much as shutdown hook will take care of them and butcher them.

Usage example
==========
Pretty much same as [embedded-redis](https://github.com/kstyrc/embedded-redis)

Binaries
==========
Soon in Maven central...

<dependency>
	<groupId>net.anthavio</groupId>
	<artifactId>rembedis</artifactId>
	<version>1.0.0</version>
	<classifier>redis-2.8.5</classifier>
</dependency>

Snapshots
==========

Snapshots are hosted in [Sonatype OSS](https://oss.sonatype.org/content/repositories/snapshots/net/anthavio/rembedis) To access them, you need to add maven repository into you settings.xml/pom.xml

```xml
<repository>
  <id>sonatype-oss-public</id>
  <url>https://oss.sonatype.org/content/groups/public</url>
  <releases>
    <enabled>true</enabled>
  </releases>
  <snapshots>
    <enabled>true</enabled>
  </snapshots>
</repository>
```

With gradle...
```
repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots"
    }
}
```

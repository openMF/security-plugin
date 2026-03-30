# Self service Plugin for Apache Fineract

## For Users


1. [Download link for Self Service Plugin ](https://sourceforge.net/projects/mifos/files/mifos-plugins/SelfServicePlugin/SelfServicePlugin-1.9.0.zip/download)  and extract the files (java jar files are on it)

1a. Execute only for DOCKER - Create a directory, copy the Self Service Plugin libraries in it

```bash
    mkdir selfservice-plugin  && cd selfservice-plugin
```

1b. Execute only for TOMCAT - Copy the Self Service Plugin libraries in $TOMCAT_HOME/webapps/fineract-provider/WEB-INF/lib/

5. Restart Docker or Tomcat

6. Test the Self Service

## For Developers

Add the repository https://mifos.jfrog.io/artifactory/libs-snapshot-local/

Maven
```bash
    <dependency>
        <groupId>community.mifos</groupId>
        <artifactId>security-plugin</artifactId>
        <version>1.15.0-SNAPSHOT</version>
    </dependency>
```
Gradle
```bash
    compile(group: 'community.mifos', name: 'selfservice-plugin', version: '1.15.0-SNAPSHOT')
```

## Build & Use For Linux Users

This project is currently only tested against the very latest and greatest
bleeding edge Fineract `develop` branch on Linux Ubuntu 24.04LTS. Building and using it against
other versions may be possible, but is not tested or documented here.

1. Download and compile

```bash
    git clone https://github.com/openMF/selfservice-plugin.git
    cd selfservice-plugin && ./mvnw -Dmaven.test.skip=true clean package && cd ..
```

2. Execute Apache Fineract with the location of the Self Service Plugin library for Apache Fineract

```bash
java -Dloader.path=$APACHE_FINERACT_PLUGIN_HOME/libs/ -jar $APACHE_FINERACT_HOME/fineract-provider.jar
```

## Important

* Mifos® is not affiliated with, endorsed by, or otherwise associated with the Apache Software Foundation® (ASF) or any of its projects.
* Apache Software Foundation® is a vendor-neutral organization and it is an important part of the brand is that Apache Software Foundation® (ASF) projects are governed independently.
* Apache Fineract®, Fineract, Apache, the Apache® feather, and the Apache Fineract® project logo are either registered trademarks or trademarks of the Apache Software Foundation®.

## Contribute

If this Mifos® Self Service Plugin project is useful to you, please contribute back to it (and to Apache Fineract®) by raising Pull Requests yourself with any enhancements you make, and by helping to maintain this project by helping other users on Issues and reviewing PR from others (you will be promoted to committer on this project when you contribute).  
We recommend that you _Watch_ and _Star_ this project on GitHub® to make it easy to get notified.

## History

This is a plugin created from the Apache Fineract code and functionality for Self Service users. The original work is this one https://github.com/apache/fineract



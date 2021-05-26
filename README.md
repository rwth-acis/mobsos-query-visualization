# MobSOS Query Visualization
[![Build Status](https://travis-ci.org/rwth-acis/mobsos-query-visualization.svg?branch=master)](https://travis-ci.org/rwth-acis/mobsos-query-visualization) [![codecov](https://codecov.io/gh/rwth-acis/mobsos-query-visualization/branch/master/graph/badge.svg)](https://codecov.io/gh/rwth-acis/mobsos-query-visualization) [![Join the chat at https://gitter.im/rwth-acis/mobsos](https://badges.gitter.im/rwth-acis/mobsos.svg)](https://gitter.im/rwth-acis/mobsos?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

MobSOS Query Visualization is a Web service for the interactive exploration and 
visualization of relational data sets. The exploration basically consists 
in authoring query visualizations that can be persisted, exported to written reports
or published on Web sites. Authoring a query visualization consists in three simple steps:

1. selecting a _data set_, 
2. formulating a _query statement_ (usually in SQL) and 
3. selecting a _visualization type_ (e.g. pie or bar chart). 
 
Optionally, query statements and visualizations can be parameterized and further 
described by meta data such as title or display dimensions.

## Use

* You can try the service at https://las2peer.dbis.rwth-aachen.de/qv-service/.
* The wiki of this project features a [tutorial](https://github.com/rwth-acis/LAS2peer-Query-Visualization-Service/wiki/Tutorial) with step-by-step instructions.
* A short description of the service's browser interface is available as separate [help page](http://las2peer.dbis.rwth-aachen.de/qv-service/help.html).

## Build
Before you build the project make sure you have set up a [database](db.sql). 
```
mysql -u YOUR_USER -p -e 'CREATE DATABASE QVS;'
mysql -u YOUR_USER -p QVS < db.sql
mysql -u YOUR_USER -p -e "SET GLOBAL time_zone = '+00:00';"
```

Enter the credentials in the [property file](query_visualization/etc/i5.las2peer.services.mobsos.queryVisualization.QueryVisualizationService.properties). 
The following values are important: 

| Property      | Description                |
| ------------- | -------------------------- |
| stDbKey       | Default database key       |
| stDbHost      | Host for the connection    |
| stDbPort      | Port of the SQL Server     |
| stDbDatabase  | Name of the database       |
| stDbUser      | Login name                 |
| stDbPassword  | Password                   |


Execute the following command on your shell:

```shell
./gradlew clean build --info
```

## Deploy

The start_network.bat script uses the L2PNodeLauncher class to start the service. It does also register
the content of the "startup" directory and it starts the Web-Connector at port 8080.

Steps to take before launching:

1. Check the LASHOST variable at the queryviz.js (should be ok if you use the default startup script)
2. Change the address of the "qv_code_template" script (located at the demo.html file) according to your
setup. This is needed for exported queries to work.

## How to run using Docker

First build the image:
```bash
docker build . -t mobsos-query-visualization
```

Then you can run the image like this:

```bash
docker run -e MYSQL_USER=myuser -e MYSQL_PASSWORD=mypasswd -p 8080:8080 -p 9011:9011 mobsos-query-visualization
```

Replace *myuser* and *mypasswd* with the username and password of a MySQL user with access to a database named *QVS*.
By default the database host is *mysql* and the port is *3306*.
The REST-API will be available via *http://localhost:8080/QVS* and the las2peer node is available via port 9011.

In order to customize your setup you can set further environment variables.

### Node Launcher Variables

Set [las2peer node launcher options](https://github.com/rwth-acis/las2peer-Template-Project/wiki/L2pNodeLauncher-Commands#at-start-up) with these variables.
The las2peer port is fixed at *9011*.

| Variable | Default | Description |
|----------|---------|-------------|
| BOOTSTRAP | unset | Set the --bootstrap option to bootrap with existing nodes. The container will wait for any bootstrap node to be available before continuing. |
| SERVICE_PASSPHRASE | processing | Set the second argument in *startService('<service@version>', '<SERVICE_PASSPHRASE>')*. |
| SERVICE_EXTRA_ARGS | unset | Set additional launcher arguments. Example: ```--observer``` to enable monitoring. |

### Service Variables

Variables for service configuration.

| Variable | Default |
|----------|---------|
| MYSQL_USER | *mandatory* |
| MYSQL_PASSWORD | *mandatory* |
| MYSQL_HOST | mysql |
| MYSQL_PORT | 3306 |
| INIT_EXAMPLE_DATABASE | unset |
| MYSQL_EXAMPLE_USER | example |
| MYSQL_EXAMPLE_PASSWORD | example |
| MYSQL_EXAMPLE_HOST | mysql |
| MYSQL_EXAMPLE_PORT | 3306 |

### Web Connector Variables

Set [WebConnector properties](https://github.com/rwth-acis/las2peer-Template-Project/wiki/WebConnector-Configuration) with these variables.
*httpPort* and *httpsPort* are fixed at *8080* and *8443*.

| Variable | Default |
|----------|---------|
| START_HTTP | TRUE |
| START_HTTPS | FALSE |
| SSL_KEYSTORE | "" |
| SSL_KEY_PASSWORD | "" |
| CROSS_ORIGIN_RESOURCE_DOMAIN | * |
| CROSS_ORIGIN_RESOURCE_MAX_AGE | 60 |
| ENABLE_CROSS_ORIGIN_RESOURCE_SHARING | TRUE |
| OIDC_PROVIDERS | https://api.learning-layers.eu/o/oauth2,https://accounts.google.com |

### Other Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DEBUG  | unset | Set to any value to get verbose output in the container entrypoint script. |

### Volumes

The following places should be persisted in volumes in productive scenarios:

| Path | Description |
|------|-------------|
| /src/node-storage | Pastry P2P storage. |
| /src/etc/startup | Service agent key pair and passphrase. |
| /src/log | Log files. |

*Do not forget to persist you database data*

## Background

The original use case behind MobSOS QV was the exploration of MobSOS datasets
for metrics explaining the success (or failure) of artifacts (i.e. services or tools)
provided by a _community information system (CIS)_. Any MobSOS data set comprises 
automatically collected, cleaned and metadata-enriched _usage data_ as well as 
end-user-contributed _survey data_. The exploration of possible CIS success metrics 
is much more convenient and intuitive with the help of interactive query visualizations.
Additional persistence of such query visualizations enables analysts to build up
their own _CIS success metric catalogues_, to create dashboards showing query visualizations
on real-time data and ultimately to compile MobSOS-style hierarchical _CIS success models_.
CIS success metrics thereby serve as proxy indicators of certain _CIS success factors_. 
These factors in turn are assigned to one of six predefined and scientifically validated
_CIS success dimensions_. The result is a CIS success model to be validated and refined 
over time to reflect a community's changing understanding of CIS success for given CIS 
artifacts. However, MobSOS QV quickly turned out to be a rather generic tool for query 
visualizations on arbitrary relational data sets. It has been used for creating dashboards
on the evolution of different scientific or open-source developer communities.

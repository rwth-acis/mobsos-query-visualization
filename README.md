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
Execute the following command on your shell:

```shell
ant all 
```

## Deploy

TODO: needs update

The start_network.bat script uses the L2PNodeLauncher class to start the service. It does also register
the content of the "startup" directory and it starts the HTTP-connector at port 8080.

Steps to take before launching:

1. Add a default database by adding the service configuration file located in the "config" folder.

2. Check the LASHOST variable at the queryviz.js (should be ok if you use the default startup script)

3. Change the address of the "qv_code_template" script (located at the demo.html file) according to your
setup. This is needed for exported queries to work.

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

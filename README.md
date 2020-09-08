# NRMM

[NRMM](https://www.london.gov.uk/what-we-do/environment/pollution-and-air-quality/nrmm) is a java application built for the Greater London Authority in order to register and manage "NON-ROAD MOBILE MACHINERY" in London.

## Installation

Use [maven](https://maven.apache.org/guides/getting-started/) from the root of the source code directory build NRMM, the build WAR file within 'target' can then be deployed to tomcat.

```bash
mvn clean install
```

## Usage

NRMM was designed to run on Ubuntu 18.04 with tomcat 9 and a PostgresSQL 11 database.

The database can be configured through src/main/webapp/META-INF/context.xml 

Note: we plan to release the database schema in a future update, until then please contact us for more information/

To use your own website template/style see: src/main/webapp/WEB-INF/templates/external/

Also note: the current version of NRMM uses the [froala wysiwyg editor](https://froala.com/wysiwyg-editor/); this can be substituted for a different editor (see src/main/webapp/javascript/froala_editor/)

## License

Copyright (c) Greater London Authority, 2020.

This software was developed by Pivotal Solutions Limited for the GLA Non-Road Mobile Machinery (NRMM) project. 

This source code is licensed under the Open Government Licence 3.0.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.

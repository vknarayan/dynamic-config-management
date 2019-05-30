                                              Dynamic Configuration Management

Benefits of externalized config manageement:

Faster config changes - automatic reloading without restarting the applications is made possible
Supporting non-uniform environments become simple. The code base can remain the same, only configuration need to be changed.
Credentials stored in config files need to be private. They have to be handled separately compared to the code.

Externalization is already well understood. People have figured out how to externalize all environment specific configuration values and use them in appropriate ways. It still makes sense in most cases to use the common repository for code and the config files. But still how do you handle configuration changes for a minor release, where there are no code changes ? This is one of the strong motivations for config file management.

Going forward, we are moving to more externalized approach towards configuration so that the code base does not change with environments.

Configuration files are different from code, and it might make sense to put all config files across services in a common repository folder and their access can be handled in a way different from the code.

Going forward, there may be cases where we have to make frequent changes in configuration data without restart of the server. There could be need to propagate the changes in the configuration automatically to all the servers without server restart.

We need to provide a transition path from internalized, decentralized & static configuration files to externalized, centralized and dynamic configuration data.

Spring cloud config:

Spring Cloud Config Server is a library. It can be embedded in your application directly to talk to git. It reduces the complexity of interacting with git. The other way is to have a separate server application and you talk to it as a client. 
There can be multiple clients talking to server over http. It the clients are spring boot, there are other mechanisms such as client refresh to which will update the config data from the server. Every client is expected to be aware of the Spring cloud config server, so suitable for new development.Technically, it is possible to automate the configuration updates, by making use of message brokers. (e.g., RabbitMQ, kafka)

The starting point is the server. We have to get the server running. For this, we have to create an application with spring-cloud-config-server dependency and @EnableConfigServer ( in the application code). You need a spring.cloud.config.server.git.uri to locate config data for your needs ( by default it is the location of a git repo, and can be a local "file:.." url. This will be followed by writing a client application.

mvn package && java -jar target/configuration-service-0.0.1-SNAPSHOT.jar

mvn package && java -jar target/configuration-client-0.0.1-SNAPSHOT.jar

Here <artifactId>configuration-client</artifactId>, <version>0.0.1-SNAPSHOT</version> picked from pom.xml. Note that this is only name of the jar file. The spring application name is specified in the property spring.application.name in the local file bootstrap.yml.

To reflect the changes in the config file in the client, use the @RefreshScope annotation in the client code. Also, hit the refresh endpoint on the client.

curl localhost:8080/actuator/refresh -d {} -H "Content-Type: application/json"

But before this, you need to enable the refresh endpoint by adding the following line in application.properties of the client

management.endpoints.web.exposure.include=*

Note: The above statements on refresh are also true for server. If we are embedding config server in the tool, we need to do the above steps in server. But if config server is run as a separate microservice, you can do the refresh in the tool itself, which is a client.

How spring appln name propagated & properties are read by the server ?

In the client application, put the name in boostrap.properties 
spring.application.name=my-client

In the repository, you should have the corresponding properties file in the git address (repository) specified in application.properties, 

spring.cloud.config.server.git.uri=${HOME}/Desktop/config

Note that server side configuration is not involved here. You can add a new client without restart of the config service! as long as there is no new repository involved. message stored in the config file, and what is returned to the client ( or browser) can be instrumented in a method. The http end point is a method call, you can do what you like in the client implementation.

Inside the repository, all common configuration is placed in the file application.yml and specific configuration per microservice is placed in a file with the name of the microservice (as specified in the property spring.application.name in the local file bootstrap.yml).

Environment specific configuration

Environment specific configuration is handled in different Spring profiles. We can have multiple profiles for specific environments, e.g. profiles for test, qa and prod.

Separate property files corresponding to each profile can be prepared. Eg., application-PROD.properties ( in addition to application.properties common to all applications).

We can also have separate repositories for different profiles.

{serviceID}-{profile}.properties
How other language clients use Spring Cloud Config Server?
The standard uri's are /{name}/{profiles} and /{name}/{profiles}/{label}. These return a json format optimized for the spring cloud config client.

{name} is the application name. {profiles} is a comma separated list of profiles.  {label} is the branch name when using git or svn.

The following return the data in other formats optimized for those formats:

/{name}-{profiles}.properties
/{label}/{name}-{profiles}.properties
{name}-{profiles}.json
/{label}/{name}-{profiles}.json
/{name}-{profiles}.yml
/{name}-{profiles}.yaml
/{label}/{name}-{profiles}.yml
/{label}/{name}-{profiles}.yaml

Remember that configuration server serves property sources from /{name}/{profile}/{label} to applications, where the default bindings in the client app are the following:

"name" = ${spring.application.name}
"profile" = ${spring.profiles.active}
"label" = "master"
Dynamic reloading of application.properties
providing the external config file location is important, either in the command line or in application.properties file; the default application.properties file is packed in jar; what we give here is an override. the name can be anything, say app-external.properties.

mvn package && java -Dspring.config.location=file:target/classes/application.properties -jar target/configuration-service-0.0.1-SNAPSHOT.jar

Even with the reload, the git repository corresponding to an updated (/new) uri will not work, based on http request. This is a very special usecase and I guess there is no immediate answer. However, this is not needed because of the namespace feature. With the namespace feature, we do not have to provide the git path separately for each service

spring.cloud.config.server.git.uri=${HOME}/Desktop/{application}

How to use Spring Cloud Config in an orchestration tool

Option 1: Tool as the config server
We can have the common approach / same meta data collection and operations for all clients. ( clients who need config file updates, modern clients who need dynamic data update - spring boot or non-boot )

Option 2: Tool as client to config server with tool doing the config file processing
There is a standalone Spring cloud config server. Tool can do all the config file handling,  but for new clients ( with dynamic data handling), the spring cloud server need to handle the metadata. We cannot have two different services handling metadata for 2 different use cases. Theoretically, this may be possible, but no benefits.

Option 3: Tool as the client to config server, with config server doing all the processing
There is a standalone Spring cloud config server. Tool gets all metadata and provides the data as config file to the server. The server takes care of the deployment to the target. Once the server gets the metadata, there is no dependency with the tool. There are multiple ways in which the tool can communicate with the server. 
  Op1- Tool being a spring boot can use refresh mechanism ( but in principle, refresh mechanism is for updating the client properties from the server and not the other way round). 
  Op2- communicate through properties / config file to pass the meta data from the user to the server
  Op3- The third option is to use http calls to the server. 
The optimal approach would be to use option 2 & 3 ( option 2 to pass the meta data to the server; option 3 to trigger update config files / data to the clients.

Config File Management UI:
1. Input service name
2. location & name of the config files in the repository ( multiple config files for each service is possible)
3. branch name / tag name
4. Environment (?) or list of servers where it is to be deployed
5. Path for deployment
6. Repeat above steps for each service

How to handle new client services ?
New clients can be of 2 types. Those which continue using config files and those which want to update config data. Those which use config files can be treated like any other old clients ( In this case, we don't differentiate between old & new clients). Those which want update of config data, need to follow steps somewhat similar to what the tool does. But in this case, the config server need not transfer config files to clients.

Those which want update of config data also can be of two types - spring boot and non-spring boot. The recommendation is to use the http calls to cofig server so that the approach is common.

It is important to write the config server as state less. Basically it has 2 main jobs.
- fetch config from git - either as files for config file transfers or as data for new / modern clients
- distribute files or update / refresh config data
It should not be holding any config data per se.

Approach for properties file for config server:
The approach could be that we use a separate config file which can be loaded through command line for conventional config file transfer applications. The command line approach is good to separate out the environment from the code. It can also be a database file, but this results in tight coupling between the tool and config server.

For modern clients, only base properties file is good enough. They have to adher to the defined format and the config data will get updated.

cfg4j
No server - client concept
Everytime configuration changes, your object will get updated
on demand and periodic load available, and other strategies can be implemented
can be specified where configuration is located inside git
can support different repositories through plugins ( git, consul,...)
Multi-tenant support (a.k.a. environment selection) - store multiple configuration sets in a single configuration store (e.g. dev, test, prod) in your app or you want to maintain one store for multiple tenants). To support this use case cfg4j introduces Environments. Environment is simply a way to tell the provider where in the store is your configuration set located

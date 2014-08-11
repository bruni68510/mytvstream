mytvstream
==========

Java Attempt to stream backend (TVHeadend, EyeTV…) over the internet using RTMP/HTML5.

Main scheenshot
===============

![alt tag](https://raw.github.com/bruni68510/mytvstream/master/images/screenshot.png)

Configuration
==============

You need to copy the included configuration_sample.xml to configuration.xml.

You may also need to adapt the configuration:

```xml
<configuration>
  <!-- client configuration may be either flash/rtmp or html/empty -->
  <client type="flash" producerserver="rtmp" audiocodec="mp3" audiobitrate="128000" videocodec="flv" videobitrate="1024000"/>            
  
  <!--  backend configurations multiple -->
  <backends>
    <backend type="htsp">
      <server>192.168.0.34</server>
      <port>9982</port>
      <httpport>9981</httpport>
      <usernmame>hts</usernmame>
      <password>hts</password>
    </backend>    
    <!-- backend type="eyetv">
      <server>192.168.0.7</server>
      <port>2170</port>      
    </backend!-->
  </backends>
</configuration>

```

The client part defines the rendering part, it may be either flash/rtmp or html. 
The flash client can produce the video either using the flv codec or the h264 codec, you may adjust the videocodec parameter.

An HTML5 client is available but the real time encoding of the video to theora or webm need huge resource.

The backends section let you define the backend that are present on your local network, you can see an sample of a htsp/tv headend backend and an eye tv backend.

Compile, Execute and Play
==========================

These tasks are done invoking maven on the top of the project.

Compilation:
-------------

```shell
  mvn compile
```
  
Execution:
----------
````shell
  mvn exec:java -Dexec.args="configuration.xml"
```

Playing
--------

From the machine running the server:
http://localhost:8085/

From everywhere on your local network:
http://<ip_of_the_server>:8085

From the internet:
Your need to forward the port 8085 from your router to the server.
http://<ip_for_your_home>:8085

Background
==========

This project aggregates following components
* flazr (modded version) : provides rtmp streaming
* xuggler 5.4 : provides audio/video transcoding
* jetty(embedded): provides the web server running jsp and servlet.
* bootstrap : provides the frontend.
* jstree : provides the treeview for the channels in the frontend


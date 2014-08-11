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

The client part of the defines the rendering client, it may be either flash/rtmp other html. 
The flash client can produce the video either with the flv codec or the h264 codec, you may adjust the videocodec parameter.

An HTML5 client is available but the real time encoding of the video to theora or webm need huge resource.

The backends section let you define the backend that are present on your local network, you can see an sample of a htsp/tv headend backend and an eye tv backend.

Compiling and running
=====================

Compilation:
-------------

```shell
  mvn compile
```
  
Execution
----------
````shell
  mvn exec:java -Dexec.args="configuration.xml"
```

Background
==========

This project aggregates following components
* flazr (modded version) : provides rtmp steaming
* xuggler 5.4 : provides audio/video transcoding
* jetty(in a embedded fashion): provides the web server running jsp and servlet.
* bootstrap : provides the frontend.
* jstree : provides the treeview for the channels.

  

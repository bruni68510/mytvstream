<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="org.mytvstream.main.Main" %>
<%
  pageContext.setAttribute("backends", Main.getInstance().getBackend());
  pageContext.setAttribute("server_name", request.getServerName());
%>
<!DOCTYPE html>
<html> 
  <head>
    <title>Hello World JSP Page.</title>
     <meta charset="utf-8">
     
    <link rel="stylesheet" href="/jstree/default/style.min.css" />
    <link rel="stylesheet" href="/bootstrap/css/bootstrap.min.css" />    
    <link rel="stylesheet" href="/bootstrap/css/bootstrap-theme.min.css">
    <link rel="stylesheet" href="page.css">
    
    <script src="/javascript/jquery.js"></script>
    <script src="/javascript/jquery.gracefulWebSocket.js"></script>
    <script src="/javascript/jquery.json-2.4.js"></script>
    
    <script src="/bootstrap/js/bootstrap.min.js"></script>
    <script src="/jstree/jstree.min.js"></script>    
    <script src="/javascript/page.js"></script>
    
        <script>
    
    
    $(function () {
    
      var ws = $.gracefulWebSocket('ws://<c:out value="${server_name}"/>:8085/events/');
     
      ws.onmessage = function (event) {
        var messageFromServer = event.data;
        //console.log(messageFromServer);
        
        var obj = jQuery.parseJSON( messageFromServer );
        
        if (obj.action == "CHANNELFAILED") {
          alert("Failed to tune channel" + obj.error);
        }
        
        if (obj.action == "CHANNELSTARTED") {
          
          
          $("#flash").empty();
          /*          
          $("#flash").append(            
            '<object data="/resources/jaris.swf" id="VideoPlayer" type="application/x-shockwave-flash" height="100%" width="100%"> ' + 
            '<param value="true" name="menu"> ' +             
            '<param value="true" name="allowFullscreen"> ' +
            '<param value="always" name="allowScriptAccess"> ' +
            ' <param value="#000000" name="bgcolor"> ' +
            ' <param value="high" name="quality"> ' +
            ' <param value="opaque" name="wmode"> ' +
            ' <param value="noScale" name="scale"> ' +
            ' <param value="source=mystream&amp;type=video&amp;streamtype=rtmp&amp;controltype=1&amp;autostart=true&amp;controls=true&amp;darkcolor=000000&amp;brightcolor=4c4c4c&amp;controlcolor=FFFFFF&amp;hovercolor=67A8C1&amp;seekcolor=D3D3D3&amp;jsapi=true&amplive=1&amp;server=rtmp://<c:out value="${server_name}"/>/flvplayback" name="flashvars"> ' +
            '</object> '
           );
           */
          
          $("#flash").append(            
            '<object data="/resources/myPlayer.swf" id="VideoPlayer" type="application/x-shockwave-flash" height="100%" width="100%"> ' + 
            '<param value="true" name="menu"> ' +             
            '<param value="true" name="allowFullscreen"> ' +
            '<param value="always" name="allowScriptAccess"> ' +
            ' <param value="#000000" name="bgcolor"> ' +
            ' <param value="high" name="quality"> ' +
            ' <param value="opaque" name="wmode"> ' +
            ' <param value="noScale" name="scale"> ' +
            ' <param value="skin=/resources/skin.swf&stream=rtmp://192.168.0.35/flvplayback&streamname=mystream&live=1" name="flashvars"> ' +
            '</object> '
           );
          
        }
      };
    
      $.jstree.defaults.core.themes.variant = "small"; 
      $('#tree_div').jstree();
      
      $('#tree_div').on("changed.jstree", function (e, data) {
        if (data.node.data.channel) {
        
          var myObj = {
            action: "CHANNELSTART", 
            format : "FLV",
            backend : data.node.data.backend,
            bouquet : data.node.data.bouquet,
            channel : data.node.data.channel 
          };
          
          ws.send($.toJSON(myObj)); 
        }
      });
      
      
       
    });
  
    
    </script>    
  </head>
  
  <body>
  
    
    <div class="page-header well" id="header">
      <center><h1>TVStream flash page</h1></center>
    </div>
    
    <div class="container-fluid" id="content">
      <div class="row-fluid fill" >
    
        <!--Sidebar content-->
        <div class="col-md-3 grey_background fill panel" id="channel_list">           
          <div id="tree_div">
            <ul>
              <c:forEach var="backend" items="${backends}"> 
                <li data-jstree='{"icon":"/resources/images/tree.png"}'><c:out value="${backend.getName()}"/>
                  <ul>
                    <c:forEach var="bouquet" items="${backend.getBouquets()}">
                      <li><c:out value="${bouquet.getName()}"/>
                        <ul>
                          <c:forEach var="channel" items="${bouquet.getChannels()}">
                            <li 
                              data-channel=<c:out value="${channel.getID()}"/> 
                              data-bouquet=<c:out value="${bouquet.getID()}"/> 
                              data-backend=<c:out value="${backend.getID()}"/> 
                              data-jstree='{"icon":"/resources/images/film.png"}'
                            >
                            <c:out value="${channel.getNumber()} - ${channel.getName()}"/>
                            </li>
                          </c:forEach>
                        </ul>
                       </li>
                    </c:forEach>
                  </ul> 
                 </li>
              </c:forEach>
            </ul>
          </div>
       </div>
       
       <div class="col-md-9 fill" id="flash">
          <object data="/resources/jaris.swf" 
            id="VideoPlayer" type="application/x-shockwave-flash" height="100%" width="100%">
            <param value="true" name="menu">
            <param value="noScale" name="scale">
            <param value="true" name="allowFullscreen">
            <param value="always" name="allowScriptAccess">
            <param value="#000000" name="bgcolor">
            <param value="high" name="quality">
            <param value="opaque" name="wmode">
            <param value="controls=false&amp;darkcolor=000000&amp;brightcolor=4c4c4c&amp;controlcolor=FFFFFF&amp;hovercolor=67A8C1&amp;seekcolor=D3D3D3" name="flashvars"> 
          </object>
          
       </div>    
     
    </div>
  </div>
 </div>


  </body>
</html>
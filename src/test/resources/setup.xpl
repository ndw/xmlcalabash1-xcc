<p:declare-step version='1.0' name="main"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:manage="http://marklogic.com/manage"
                exclude-inline-prefixes="c cx ml manage">
<p:option name="config" select="'config.xml'"/>
<p:output port="result"/>
<p:serialization port="result" indent="true"/>

<p:import href="http://xmlcalabash.com/extension/steps/marklogic-xcc.xpl"/>

<p:variable name="host" select="doc($config)/config/host/string()"/>
<p:variable name="root" select="doc($config)/config/root/string()"/>
<p:variable name="port" select="xs:integer(doc($config)/config/port)"/>
<p:variable name="user" select="doc($config)/config/user/string()"/>
<p:variable name="password" select="doc($config)/config/password/string()"/>

<p:add-attribute attribute-name="href" match="/*">
  <p:input port="source">
    <p:inline><c:request method="get"/></p:inline>
  </p:input>
  <p:with-option name="attribute-value"
                 select="concat('http://', $host, ':7997/')"/>
</p:add-attribute>

<p:try name="healthy">
  <p:group>
    <p:http-request cx:timeout="3000"/>
  </p:group>
  <p:catch>
    <p:identity name="healthy-fail">
      <p:input port="source">
        <p:inline><html><body>Fail</body></html></p:inline>
      </p:input>
    </p:identity>
  </p:catch>
</p:try>

<p:choose>
  <p:when test="contains(., 'Healthy')">
    <p:document-template name="config">
      <p:input port="source"><p:empty/></p:input>
      <p:input port="template">
        <p:document href="marklogic/xdbc-server.xml"/>
      </p:input>
      <p:with-param name="port" select="$port"/>
      <p:with-param name="root" select="$root"/>
    </p:document-template>

    <p:document-template name="request">
      <p:input port="source"><p:empty/></p:input>
      <p:input port="template">
        <p:inline>
          <c:request method="post" href="{$href}"
                     username="{$username}" password="{$password}"
                     auth-method="digest">
            <c:body content-type="application/xml"></c:body>
          </c:request>
        </p:inline>
      </p:input>
      <p:with-param name="username" select="$user"/>
      <p:with-param name="password" select="$password"/>
      <p:with-param name="href" select="concat('http://', $host, ':8002/manage/v2/servers?group-id=Default')"/>
    </p:document-template>

    <p:insert position="last-child" match="/*/c:body">
      <p:input port="source">
        <p:pipe step="request" port="result"/>
      </p:input>
      <p:input port="insertion">
        <p:pipe step="config" port="result"/>
      </p:input>
    </p:insert>

    <p:http-request/>

    <p:choose>
      <p:when test="/c:body">
        <p:identity>
          <p:input port="source">
            <p:inline><c:result>PASS</c:result></p:inline>
          </p:input>
        </p:identity>
      </p:when>
      <p:otherwise>
        <p:error code="ml:fail">
          <p:input port="source"><p:empty/></p:input>
        </p:error>
      </p:otherwise>
    </p:choose>
  </p:when>
  <p:otherwise>
    <p:identity>
      <p:input port="source">
        <p:inline><c:result>VACUOUS PASS</c:result></p:inline>
      </p:input>
    </p:identity>
  </p:otherwise>
</p:choose>

</p:declare-step>

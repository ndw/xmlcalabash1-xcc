<p:declare-step version='1.0' name="main"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                exclude-inline-prefixes="c cx ml">
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

<p:try>
  <p:group>
    <p:http-request cx:timeout="3000"/>
  </p:group>
  <p:catch>
    <p:identity>
      <p:input port="source">
        <p:inline><html><body>Fail</body></html></p:inline>
      </p:input>
    </p:identity>
  </p:catch>
</p:try>

<p:choose>
  <p:when test="contains(., 'Healthy')">
    <ml:insert-document name="insert" content-base="Documents" uri="/xmlcalabash-insert-test.xml">
      <p:input port="source">
        <p:inline><doc>Hello, world!</doc></p:inline>
      </p:input>
      <p:with-option name="host" select="$host"/>
      <p:with-option name="port" select="$port"/>
      <p:with-option name="user" select="$user"/>
      <p:with-option name="password" select="$password"/>
    </ml:insert-document>
    <p:choose>
      <p:xpath-context>
        <p:pipe step="insert" port="result"/>
      </p:xpath-context>
      <p:when test="/c:result[. = '/xmlcalabash-insert-test.xml']">
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

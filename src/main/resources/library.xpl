<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
           version="1.0">

<p:declare-step type="ml:adhoc-query">
   <p:input port="source"/>
   <p:input port="parameters" kind="parameter"/>
   <p:output port="result" sequence="true"/>
   <p:option name="host"/>
   <p:option name="port"/>
   <p:option name="user"/>
   <p:option name="password"/>
   <p:option name="content-base"/>
   <p:option name="wrapper"/>
   <p:option name="auth-method"/>
</p:declare-step>

<p:declare-step type="ml:insert-document">
   <p:input port="source"/>
   <p:output port="result" primary="false"/>
   <p:option name="host"/>
   <p:option name="port"/>
   <p:option name="user"/>
   <p:option name="password"/>
   <p:option name="content-base"/>
   <p:option name="uri" required="true"/>
   <p:option name="buffer-size"/>
   <p:option name="collections"/>
   <p:option name="format"/>
   <p:option name="language"/>
   <p:option name="locale"/>
   <p:option name="auth-method"/>
</p:declare-step>

<p:declare-step type="ml:invoke-module">
   <p:input port="parameters" kind="parameter"/>
   <p:output port="result" sequence="true"/>
   <p:option name="module" required="true"/>
   <p:option name="host"/>
   <p:option name="port"/>
   <p:option name="user"/>
   <p:option name="password"/>
   <p:option name="content-base"/>
   <p:option name="wrapper"/>
   <p:option name="auth-method"/>
</p:declare-step>

</p:library>

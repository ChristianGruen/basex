package org.basex.query.func;

import static org.basex.query.QueryError.*;
import static org.basex.query.func.Function.*;

import org.basex.query.*;
import org.junit.*;

/**
 * This class tests the functions of the Inspection Module.
 *
 * @author BaseX Team 2005-18, BSD License
 * @author Christian Gruen
 */
public final class InspectModuleTest extends AdvancedQueryTest {
  /** Test method. */
  @Test
  public void function() {
    String func = query(_INSPECT_FUNCTION.args(" true#0"));
    query(func + "/@name/data()", true);
    query(func + "/@uri/data()", "http://www.w3.org/2005/xpath-functions");
    query(func + "/return/@type/data()", "xs:boolean");
    query(func + "/return/@occurrence/data()", "");

    func = query(_INSPECT_FUNCTION.args(" map { }"));
    query(func + "/@name/data()", "");
    query(func + "/@uri/data()", "");
    query(func + "/argument/@type/data()", "xs:anyAtomicType");
    query(func + "/return/@type/data()", "item()");
    query(func + "/return/@occurrence/data()", "*");

    func = query(_INSPECT_FUNCTION.args(" function($a as xs:int) as xs:integer { $a + 1 }"));
    query(func + "/@name/data()", "");
    query(func + "/@uri/data()", "");
    query(func + "/argument/@name/data()", "");
    query(func + "/argument/@type/data()", "xs:int");
    query(func + "/return/@type/data()", "xs:integer");
    query(func + "/return/@occurrence/data()", "");

    func = query("declare %private function Q{U}f($v as xs:int) as xs:integer {$v};" +
        _INSPECT_FUNCTION.args(" Q{U}f#1"));
    query(func + "/@name/data()", "f");
    query(func + "/@uri/data()", "U");
    query(func + "/argument/@name/data()", "v");
    query(func + "/argument/@type/data()", "xs:int");
    query(func + "/annotation/@name/data()", "private");
    query(func + "/annotation/@uri/data()", "http://www.w3.org/2012/xquery");
    query(func + "/return/@type/data()", "xs:integer");
    query(func + "/return/@occurrence/data()", "");

    // unknown annotation
    query("declare namespace pref='uri';" +
      _INSPECT_FUNCTION.args(" %pref:x function() {()}") + "/annotation/@name/data()", "pref:x");
    query("declare namespace pref='uri';" +
        _INSPECT_FUNCTION.args(" %pref:x function() {()}") + "/annotation/@uri/data()", "uri");
  }

  /** Test method. */
  @Test
  public void module() {
    error(_INSPECT_MODULE.args("src/test/resources/non-existent.xqm"), WHICHRES_X);

    final String module = "src/test/resources/hello.xqm";
    final String result = query(_INSPECT_MODULE.args(module)).
        replace("{", "{{").replace("}", "}}");

    final String var1 = query(result + "/variable[@name = 'hello:lazy']");
    query(var1 + "/@uri/data()", "world");
    query(var1 + "/@external/data()", false);
    query(var1 + "/annotation/@name/data()", "basex:lazy");
    query(var1 + "/annotation/@uri/data()", "http://basex.org");

    final String var2 = query(result + "/variable[@name = 'hello:ext']");
    query(var2 + "/@external/data()", true);

    final String func1 = query(result + "/function[@name = 'hello:world']");
    query(func1 + "/@uri/data()", "world");
    query(func1 + "/@external/data()", false);
    query(func1 + "/annotation/@name/data()", "public");
    query(func1 + "/annotation/@uri/data()", "http://www.w3.org/2012/xquery");
    query(func1 + "/return/@type/data()", "xs:string");
    query(func1 + "/return/@occurrence/data()", "");

    final String func2 = query(result + "/function[@name = 'hello:internal']").
        replace("{", "{{").replace("}", "}}");
    query(func2 + "/@uri/data()", "world");
    query(func2 + "/annotation/@name/data()[ends-with(., 'ignored')]", "ignored");
    query(func2 + "/annotation/@uri/data()[. = 'ns']", "ns");

    final String func3 = query(result + "/function[@name = 'hello:ext']");
    query(func3 + "/@external/data()", true);
  }

  /** Test method. */
  @Test
  public void xqdoc() {
    error(_INSPECT_XQDOC.args("src/test/resources/non-existent.xqm"), WHICHRES_X);

    // validate against xqDoc schema
    final String result = query(_INSPECT_XQDOC.args("src/test/resources/hello.xqm")).
        replace("{", "{{").replace("}", "}}");
    query(_VALIDATE_XSD.args(' ' + result, "src/test/resources/xqdoc.xsd"));
  }

  /** Test method. */
  @Test
  public void context() {
    final String func = query("declare function local:x() { 1 }; " + _INSPECT_CONTEXT.args());
    query(func + "/name()", "context");
    query("count(" + func + "/function)", 1);
    query(func + "/function/@name/string()", "local:x");
  }

  /** Test method. */
  @Test
  public void functions() {
    final String url = "src/test/resources/hello.xqm";
    query("declare function local:x() { 1 }; " + COUNT.args(_INSPECT_FUNCTIONS.args()), 1);
    query("declare function local:x() { 2 }; " + _INSPECT_FUNCTIONS.args() + "()", 2);
    query("import module namespace hello='world' at '" + url + "';" +
        _INSPECT_FUNCTIONS.args() + "[last()] instance of function(*)", true);

    query("for $f in " + _INSPECT_FUNCTIONS.args(url)
        + "where local-name-from-QName(function-name($f)) = 'world' "
        + "return $f()", "hello world");

    // ensure that closures will be compiled (GH-1194)
    query(_INSPECT_FUNCTIONS.args(url)
        + "[function-name(.) = QName('world','closure')]()", 1);
    query("import module namespace hello='world' at '" + url + "';"
        + _INSPECT_FUNCTIONS.args() + "[function-name(.) = xs:QName('hello:closure')]()", 1);
  }

  /** Test method. */
  @Test
  public void functionAnnotations() {
    query(_INSPECT_FUNCTION_ANNOTATIONS.args(" true#0"), "map {\n}");
    query(_INSPECT_FUNCTION_ANNOTATIONS.args(" %local:x function() { }") +
        "=> " + _MAP_CONTAINS.args(" xs:QName('local:x')"), true);
    query(_INSPECT_FUNCTION_ANNOTATIONS.args(" %Q{uri}name('a','b') function() {}") +
        " (QName('uri','name'))", "a\nb");
    query(_MAP_SIZE.args(_INSPECT_FUNCTION_ANNOTATIONS.args(
        " %basex:inline %basex:lazy function() {}")), 2);
  }

  /** Test method. */
  @Test
  public void staticContext() {
    query(_INSPECT_STATIC_CONTEXT.args(" ()", "namespaces") + "?xml",
        "http://www.w3.org/XML/1998/namespace");
    query("starts-with(" + _INSPECT_STATIC_CONTEXT.args(" ()", "base-uri") + ", 'file:')", true);
    query(_INSPECT_STATIC_CONTEXT.args(" ()", "element-namespace"), "");
    query(_INSPECT_STATIC_CONTEXT.args(" ()", "function-namespace"),
        "http://www.w3.org/2005/xpath-functions");
    query(_INSPECT_STATIC_CONTEXT.args(" ()", "collation"),
        "http://www.w3.org/2005/xpath-functions/collation/codepoint");
    query(_INSPECT_STATIC_CONTEXT.args(" ()", "boundary-space"), "strip");
    query(_INSPECT_STATIC_CONTEXT.args(" ()", "ordering"), "ordered");
    query(_INSPECT_STATIC_CONTEXT.args(" ()", "construction"), "preserve");
    query(_INSPECT_STATIC_CONTEXT.args(" ()", "default-order-empty"), "least");
    query("declare boundary-space preserve;" +
        _INSPECT_STATIC_CONTEXT.args(" ()", "boundary-space"), "preserve");
    query(_INSPECT_STATIC_CONTEXT.args(" ()", "copy-namespaces"), "preserve\ninherit");
    query(_INSPECT_STATIC_CONTEXT.args(" ()", "decimal-formats") + "('')('percent')", "%");

    // check different function types
    query(_INSPECT_STATIC_CONTEXT.args(" true#0", "boundary-space"), "strip");
    query(_INSPECT_STATIC_CONTEXT.args(" function(){}", "boundary-space"), "strip");
    query(_INSPECT_STATIC_CONTEXT.args(" function($a){}(?)", "boundary-space"), "strip");

    // errors
    error(_INSPECT_STATIC_CONTEXT.args(" ()", "unknown"), INSPECT_UNKNOWN_X);
  }
}

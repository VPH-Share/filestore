/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Random;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.bind.BindConstants;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.*;
import org.apache.jackrabbit.webdav.property.*;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * source taken from:
 * http://svn.apache.org/repos/asf/jackrabbit/trunk/jackrabbit-webdav/
 */
public class WebDAVTest {

    private String root;
    private URI uri;
    private String username, password;
    private HttpClient client;

    @Before
    public void setUp() throws Exception {
//        String propBasePath = System.getProperty("user.home") + File.separator
//                + "workspace" + File.separator + "lobcder-tests"
//                + File.separator + "etc" + File.separator + "test.proprties";
        String propBasePath = "etc" + File.separator + "test.proprties";

        Properties prop = TestSettings.getTestProperties(propBasePath);

        String testURL = prop.getProperty("webdav.test.url");
        //Some problem with the pom.xml. The properties are set but System.getProperty gets null
        if (testURL == null) {
            testURL = "http://localhost:8080/lobcder-1.0-SNAPSHOT/";
        }
        assertTrue(testURL != null);
        if (!testURL.endsWith("/")) {
            testURL = testURL + "/";
        }

        this.uri = URI.create(testURL);
        this.root = this.uri.toASCIIString();
        if (!this.root.endsWith("/")) {
            this.root += "/";
        }

        this.username = prop.getProperty(("webdav.test.username1"), "");
        if (username == null) {
            username = "user";
        }
        assertTrue(username != null);
        this.password = prop.getProperty(("webdav.test.password1"), "");
        if (password == null) {
            password = "token0";
        }
        assertTrue(password != null);

        int port = uri.getPort();
        if (port == -1) {
            port = 443;
        }

        ProtocolSocketFactory socketFactory =
                new EasySSLProtocolSocketFactory();
        Protocol https = new Protocol("https", socketFactory, port);
        Protocol.registerProtocol("https", https);


//        List authPrefs = new ArrayList();
//        authPrefs.add(AuthPolicy.BASIC);
//        client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);

        this.client = new HttpClient();
        this.client.getState().setCredentials(
                new AuthScope(this.uri.getHost(), this.uri.getPort()),
                new UsernamePasswordCredentials(this.username, this.password));

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testConnect() throws IOException {
        HttpMethod method = new GetMethod(this.uri.toASCIIString());
        int status = client.executeMethod(method);
        //Just get something back 
        assertTrue("GetMethod status: " + status, status == HttpStatus.SC_NOT_FOUND || status == HttpStatus.SC_OK);
    }

//     http://greenbytes.de/tech/webdav/rfc5842.html#rfc.section.8.1
    @Test
    public void testOptions() throws HttpException, IOException {
        OptionsMethod options = new OptionsMethod(this.uri.toASCIIString());
        int status = this.client.executeMethod(options);
        assertEquals(HttpStatus.SC_OK, status);

//        List allow = Arrays.asList(options.getAllowedMethods());

        Enumeration allowedMethods = options.getAllowedMethods();
        ArrayList<String> allow = new ArrayList<String>();
        while (allowedMethods.hasMoreElements()) {
            String method = (String) allowedMethods.nextElement();
            System.out.println("Allowed Methods: " + method);
            allow.add(method);
        }

        /*
         * The BIND method for is creating multiple bindings to the same
         * resource. Creating a new binding to a resource causes at least one
         * new URI to be mapped to that resource. Servers are required to ensure
         * the integrity of any bindings that they allow to be created. Milton
         * dosn't support that yet
         */

//        assertTrue("DAV header should include 'bind' feature", options.hasComplianceClass("bind"));
//        assertTrue("Allow header should include BIND method", allow.contains("BIND"));
        //assertTrue("Allow header should include REBIND method", allow.contains("REBIND"));
        //assertTrue("Allow header should include UNBIND method", allow.contains("UNBIND"));

        assertTrue("Allow header should include COPY method", allow.contains("COPY"));
        assertTrue("Allow header should include DELETE method", allow.contains("DELETE"));
        assertTrue("Allow header should include MKCOL method", allow.contains("MKCOL"));
        assertTrue("Allow header should include PROPFIND method", allow.contains("PROPFIND"));
        assertTrue("Allow header should include GET method", allow.contains("GET"));
        assertTrue("Allow header should include HEAD method", allow.contains("HEAD"));
        assertTrue("Allow header should include PROPPATCH method", allow.contains("PROPPATCH"));
        assertTrue("Allow header should include OPTIONS method", allow.contains("OPTIONS"));
        assertTrue("Allow header should include MOVE method", allow.contains("MOVE"));
        assertTrue("Allow header should include PUT method", allow.contains("PUT"));
    }

    //     create test resource, make it referenceable, check resource id, move resource, check again
    @Test
    public void testResourceId() throws HttpException, IOException, DavException, URISyntaxException {
        System.out.println("testResourceId");
        String testcol = this.root + "testResourceId/";
        String testuri1 = testcol + "bindtest1";
        String testuri2 = testcol + "bindtest2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            PutMethod put = new PutMethod(testuri1);
            put.setRequestEntity(new StringRequestEntity("testResourceId-foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            GetMethod get = new GetMethod(testuri1);
            this.client.executeMethod(get);
            status = get.getStatusCode();
            assertEquals(HttpStatus.SC_OK, status);
            assertEquals("testResourceId-foo", get.getResponseBodyAsString());


            // enabling version control always makes the resource referenceable
            //No version control yet
            //VersionControlMethod versioncontrol = new VersionControlMethod(testuri1);
            //status = this.client.executeMethod(versioncontrol);
            //assertTrue("VersionControlMethod status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED);
            //URI resourceId = getResourceId(testuri1);

            MoveMethod move = new MoveMethod(testuri1, testuri2, true);
            status = this.client.executeMethod(move);
            assertEquals(HttpStatus.SC_CREATED, status);

            get = new GetMethod(testuri2);
            this.client.executeMethod(get);
            status = get.getStatusCode();
            assertEquals(HttpStatus.SC_OK, status);
            assertEquals("testResourceId-foo", get.getResponseBodyAsString());
//            System.out.println("Resp: " + get.getResponseBodyAsString());

//            URI resourceId2 = getResourceId(testuri2);
//            assertEquals(resourceId, resourceId2);
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("DeleteMethod status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        }
    }

    @Test
    public void testSimpleBind() throws Exception {
        System.out.println("testSimpleBind");
        String testcol = this.root + "testSimpleBind/";
        String subcol1 = testcol + "bindtest1/";
        String testres1 = subcol1 + "res1";
        String subcol2 = testcol + "bindtest2/";
        String testres2 = subcol2 + "res2";
        int status;
        try {
            //Create testSimpleBind/
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            //Create testSimpleBind/bindtest1
            mkcol = new MkColMethod(subcol1);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            //Create testSimpleBind/bindtest2
            mkcol = new MkColMethod(subcol2);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            //create new resource R with path testSimpleBind/bindtest1/res1
            PutMethod put = new PutMethod(testres1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            //create new binding of R with path bindtest2/res2
            //No BindMethod yet
//            DavMethodBase bind = new BindMethod(subcol2, new BindInfo(testres1, "res2"));
//            status = this.client.executeMethod(bind);
//            assertEquals(HttpStatus.SC_CREATED, status);
            //check if both bindings report the same DAV:resource-id
//            assertEquals(this.getResourceId(testres1), this.getResourceId(testres2));


            GetMethod get = new GetMethod(testres1);
            status = this.client.executeMethod(get);
            assertEquals(HttpStatus.SC_OK, status);
            assertEquals("foo", get.getResponseBodyAsString());

            //Doesn't work cause we don't have bind
//            get = new GetMethod(testres2);
//            status = this.client.executeMethod(get);
//            assertEquals(HttpStatus.SC_OK, status);
//            assertEquals("foo", get.getResponseBodyAsString());

//            //modify R using the new path
//            put = new PutMethod(testres2);
//            put.setRequestEntity(new StringRequestEntity("bar", "text/plain", "UTF-8"));
//            status = this.client.executeMethod(put);
//            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
//
////            //compare representations retrieved with both paths
//            get = new GetMethod(testres1);
//            status = this.client.executeMethod(get);
//            assertEquals(HttpStatus.SC_OK, stagetEntriesChildren();tus);
//            assertEquals("bar", get.getResponseBodyAsString());
//            get = new GetMethod(testres2);
//            status = this.client.executeMethod(get);
//            assertEquals(HttpStatus.SC_OK, status);
//            assertEquals("bar", get.getResponseBodyAsString());
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        }
    }

//    No rebind yet
    @Test
    public void testRebind() throws Exception {
        System.out.println("testRebind");
        String testcol = this.root + "testRebind/";
        String subcol1 = testcol + "bindtest1/";
        String testres1 = subcol1 + "res1";
        String subcol2 = testcol + "bindtest2/";
        String testres2 = subcol2 + "res2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);
            mkcol = new MkColMethod(subcol1);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);
            mkcol = new MkColMethod(subcol2);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            //create new resource R with path bindtest1/res1
            PutMethod put = new PutMethod(testres1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            // enabling version control always makes the resource referenceable
            VersionControlMethod versioncontrol = new VersionControlMethod(testres1);
            status = this.client.executeMethod(versioncontrol);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED);

//            URI r1 = this.getResourceId(testres1);

            GetMethod get = new GetMethod(testres1);
            status = this.client.executeMethod(get);
            assertEquals(HttpStatus.SC_OK, status);
            assertEquals("foo", get.getResponseBodyAsString());

            //rebind R with path bindtest2/res2
//            DavMethodBase rebind = new RebindMethod(subcol2, new RebindInfo(testres1, "res2"));
//            status = this.client.executeMethod(rebind);
//            assertEquals(HttpStatus.SC_CREATED, status);
//            URI r2 = this.getResourceId(testres2);
//            get = new GetMethod(testres2);
//            status = this.client.executeMethod(get);
//            assertEquals(HttpStatus.SC_OK, status);
//            assertEquals("foo", get.getResponseBodyAsString());

            //make sure that rebind did not change the resource-id
//            assertEquals(r1, r2);

            //verify that the initial binding is gone
//            HeadMethod head = new HeadMethod(testres1);
//            status = this.client.executeMethod(head);
//            assertEquals(HttpStatus.SC_NOT_FOUND, status);
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        }
    }

    @Test
    public void testBindOverwrite() throws Exception {
        System.out.println("testBindOverwrite");
        String testcol = this.root + "testSimpleBind/";
        String subcol1 = testcol + "bindtest1/";
        String testres1 = subcol1 + "res1";
        String subcol2 = testcol + "bindtest2/";
        String testres2 = subcol2 + "res2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);
            mkcol = new MkColMethod(subcol1);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);
            mkcol = new MkColMethod(subcol2);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            //create new resource R with path bindtest1/res1
            PutMethod put = new PutMethod(testres1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            //create new resource R' with path bindtest2/res2
            put = new PutMethod(testres2);
            put.setRequestEntity(new StringRequestEntity("bar", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            //try to create new binding of R with path bindtest2/res2 and Overwrite:F
//            DavMethodBase bind = new BindMethod(subcol2, new BindInfo(testres1, "res2"));
//            bind.addRequestHeader(new Header("Overwrite", "F"));
//            status = this.client.executeMethod(bind);
//            assertEquals(412, status);

            //verify that bindtest2/res2 still points to R'
//            GetMethod get = new GetMethod(testres2);
//            status = this.client.executeMethod(get);
//            assertEquals(HttpStatus.SC_OK, status);
//            assertEquals("bar", get.getResponseBodyAsString());

            //create new binding of R with path bindtest2/res2
//            bind = new BindMethod(subcol2, new BindInfo(testres1, "res2"));
//            status = this.client.executeMethod(bind);
//            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);

            //verify that bindtest2/res2 now points to R
//            get = new GetMethod(testres2);
//            status = this.client.executeMethod(get);
//            assertEquals(HttpStatus.SC_OK, status);
//            assertEquals("foo", get.getResponseBodyAsString());

            //verify that the initial binding is still there
//            HeadMethod head = new HeadMethod(testres1);
//            status = this.client.executeMethod(head);
//            assertEquals(HttpStatus.SC_OK, status);
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        }
    }

    @Test
    public void testRebindOverwrite() throws Exception {
        System.out.println("testRebindOverwrite");
        String testcol = this.root + "testSimpleBind/";
        String subcol1 = testcol + "bindtest1/";
        String testres1 = subcol1 + "res1";
        String subcol2 = testcol + "bindtest2/";
        String testres2 = subcol2 + "res2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);
            mkcol = new MkColMethod(subcol1);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);
            mkcol = new MkColMethod(subcol2);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            //create new resource R with path testSimpleBind/bindtest1/res1
            PutMethod put = new PutMethod(testres1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);


            // enabling version control always makes the resource referenceable
//            VersionControlMethod versioncontrol = new VersionControlMethod(testres1);
//            status = this.client.executeMethod(versioncontrol);
//            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED);

            //create new resource R' with path testSimpleBind/bindtest2/res2
            put = new PutMethod(testres2);
            put.setRequestEntity(new StringRequestEntity("bar", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            //try rebind R with path testSimpleBind/bindtest2/res2 and Overwrite:F
//            RebindMethod rebind = new RebindMethod(subcol2, new RebindInfo(testres1, "res2"));
//            rebind.addRequestHeader(new Header("Overwrite", "F"));
//            status = this.client.executeMethod(rebind);
//            assertEquals(412, status);
//
//            //verify that testSimpleBind/bindtest2/res2 still points to R'
//            GetMethod get = new GetMethod(testres2);
//            status = this.client.executeMethod(get);
//            assertEquals(HttpStatus.SC_OK, status);
//            assertEquals("bar", get.getResponseBodyAsString());

            //rebind R with path testSimpleBind/bindtest2/res2
//            rebind = new RebindMethod(subcol2, new RebindInfo(testres1, "res2"));
//            status = this.client.executeMethod(rebind);
//            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);

            //verify that testSimpleBind/bindtest2/res2 now points to R
//            get = new GetMethod(testres2);
//            status = this.client.executeMethod(get);
//            assertEquals(HttpStatus.SC_OK, status);
//            assertEquals("foo", get.getResponseBodyAsString());

            //verify that the initial binding is gone
//            HeadMethod head = new HeadMethod(testres1);
//            status = this.client.executeMethod(head);
//            assertEquals(HttpStatus.SC_NOT_FOUND, status);
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        }
    }
    //No bind yet

    @Test
    public void testParentSet() throws Exception {
        System.out.println("testParentSet");
        String testcol = this.root + "testParentSet/";
        String subcol1 = testcol + "bindtest1/";
        String testres1 = subcol1 + "res1";
        String subcol2 = testcol + "bindtest2/";
        String testres2 = subcol2 + "res2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);
            mkcol = new MkColMethod(subcol1);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);
            mkcol = new MkColMethod(subcol2);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            //create new resource R with path testSimpleBind/bindtest1/res1
            PutMethod put = new PutMethod(testres1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

//            create new binding of R with path testSimpleBind/bindtest2/res2
//            DavMethodBase bind = new BindMethod(subcol2, new BindInfo(testres1, "res2"));
//            status = this.client.executeMethod(bind);
//            assertEquals(HttpStatus.SC_CREATED, status);
            //check if both bindings report the same DAV:resource-id
//            assertEquals(this.getResourceId(testres1), this.getResourceId(testres2));

//            //verify values of parent-set properties
//            List hrefs1 = new ArrayList();
//            List segments1 = new ArrayList();
//            List hrefs2 = new ArrayList();
//            List segments2 = new ArrayList();
//            Object ps1 = this.getParentSet(testres1).getValue();
//            Object ps2 = this.getParentSet(testres2).getValue();
//            assertTrue(ps1 instanceof List);
//            assertTrue(ps2 instanceof List);
//            List plist1 = (List) ps1;
//            List plist2 = (List) ps2;
//            assertEquals(2, plist1.size());
//            assertEquals(2, plist2.size());
//            for (int k = 0; k < 2; k++) {
//                Object pObj1 = plist1.get(k);
//                Object pObj2 = plist2.get(k);
//                assertTrue(pObj1 instanceof Element);
//                assertTrue(pObj2 instanceof Element);
//                ParentElement p1 = ParentElement.createFromXml((Element) pObj1);
//                ParentElement p2 = ParentElement.createFromXml((Element) pObj2);
//                hrefs1.add(p1.getHref());
//                hrefs2.add(p2.getHref());
//                segments1.add(p1.getSegment());
//                segments2.add(p2.getSegment());
//            }
//            Collections.sort(hrefs1);
//            Collections.sort(hrefs2);
//            Collections.sort(segments1);
//            Collections.sort(segments2);
//            assertEquals(hrefs1, hrefs2);
//            assertEquals(segments1, segments2);
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        }
    }

    @Test
    public void testBindCollections() throws Exception {
        System.out.println("testBindCollections");
        String testcol = this.root + "testBindCollections/";
        String a1 = testcol + "a1/";
        String b1 = a1 + "b1/";
        String c1 = b1 + "c1/";
        String x1 = c1 + "x1";
        String a2 = testcol + "a2/";
        String b2 = a2 + "b2/";
        String c2 = b2 + "c2/";
        String x2 = c2 + "x2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);
            mkcol = new MkColMethod(a1);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);
            mkcol = new MkColMethod(a2);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            //create collection resource C
            mkcol = new MkColMethod(b1);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);
            mkcol = new MkColMethod(c1);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            //create plain resource R
            PutMethod put = new PutMethod(x1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);
//
//            //create new binding of C with path a2/b2
//            DavMethodBase bind = new BindMethod(a2, new BindInfo(b1, "b2"));
//            status = this.client.executeMethod(bind);
//            assertEquals(HttpStatus.SC_CREATED, status);
//            //check if both bindings report the same DAV:resource-id
//            assertEquals(this.getResourceId(b1), this.getResourceId(b2));
//
//            mkcol = new MkColMethod(c2);
//            status = this.client.executeMethod(mkcol);
//            debug("Cretaing "+c2);
//            assertEquals(HttpStatus.SC_CREATED, status);
//
//            //create new binding of R with path a2/b2/c2/r2
//            bind = new BindMethod(c2, new BindInfo(x1, "x2"));
//            status = this.client.executeMethod(bind);
//            assertEquals(HttpStatus.SC_CREATED, status);
//            //check if both bindings report the same DAV:resource-id
//            assertEquals(this.getResourceId(x1), this.getResourceId(x2));
//
//            //verify different path alternatives
//            URI rid = this.getResourceId(x1);
//            assertEquals(rid, this.getResourceId(x2));
//            assertEquals(rid, this.getResourceId(testcol + "a2/b2/c1/x1"));
//            assertEquals(rid, this.getResourceId(testcol + "a1/b1/c2/x2"));
//            Object ps = this.getParentSet(x1).getValue();
//            assertTrue(ps instanceof List);
//            assertEquals(2, ((List) ps).size());
//            ps = this.getParentSet(x2).getValue();
//            assertTrue(ps instanceof List);
//            assertEquals(2, ((List) ps).size());
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        }
    }

    //will fail until <https://issues.apache.org/jira/browse/JCR-1773> is fixed
    @Test
    public void testUnbind() throws Exception {
        System.out.println("testUnbind");
        String testcol = this.root + "testUnbind/";
        String subcol1 = testcol + "bindtest1/";
        String testres1 = subcol1 + "res1";
        String subcol2 = testcol + "bindtest2/";
        String testres2 = subcol2 + "res2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);
            mkcol = new MkColMethod(subcol1);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);
            mkcol = new MkColMethod(subcol2);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            //create new resource R with path testSimpleBind/bindtest1/res1
            PutMethod put = new PutMethod(testres1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);
//
//            //create new binding of R with path testSimpleBind/bindtest2/res2
//            DavMethodBase bind = new BindMethod(subcol2, new BindInfo(testres1, "res2"));
//            status = this.client.executeMethod(bind);
//            assertEquals(HttpStatus.SC_CREATED, status);
//            //check if both bindings report the same DAV:resource-id
//            assertEquals(this.getResourceId(testres1), this.getResourceId(testres2));
//
//            //remove new path
//            UnbindMethod unbind = new UnbindMethod(subcol2, new UnbindInfo("res2"));
//            status = this.client.executeMethod(unbind);
//            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
//
//            //verify that the new binding is gone
//            HeadMethod head = new HeadMethod(testres2);
//            status = this.client.executeMethod(head);
//            assertEquals(HttpStatus.SC_NOT_FOUND, status);
//
//            //verify that the initial binding is still there
//            head = new HeadMethod(testres1);
//            status = this.client.executeMethod(head);
//            assertEquals(HttpStatus.SC_OK, status);
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        }
    }

    @Test
    public void testMove() throws HttpException, IOException, DavException, URISyntaxException {
        System.out.println("testMove");
        String testcol = this.root + "testResourceId/";
        String testuri = testcol + "movetest";
        String destinationuri = testuri + "2";
        String destinationpath = new URI(destinationuri).getRawPath();
        // make sure the scheme is removed
        assertFalse(destinationpath.contains(":"));

        int status;
        try {
            //Make sure the testcol is deleted
            DeleteMethod del = new DeleteMethod(testcol);
            status = this.client.executeMethod(del);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT || status == HttpStatus.SC_NOT_FOUND);


            //We can't create a resource if its parent does not exist.
            //When the PUT operation creates a new non-collection resource all 
            //ancestors MUST already exist. If all ancestors do not exist, the 
            //method MUST fail with a 409 (Conflict) status code. For example, 
            //if resource /a/b/c/d.html is to be created and /a/b/c/ does not
            //exist, then the request must fail.
            //http://www.webdav.org/specs/rfc2518.html#rfc.section.8.7.2
            //In our case (milton API) we will keep going one level up till we 
            //find an existing resource, in this case root.
            //When found root will run the <code>child</code> method without 
            //before running the <code>authenticate</code> method, resulting in 
            //an SC_UNAUTHORIZED return code
            PutMethod put = new PutMethod(testuri);
            status = this.client.executeMethod(put);
            assertTrue("status: " + status, status == HttpStatus.SC_CONFLICT || status == HttpStatus.SC_UNAUTHORIZED);


            MkColMethod mkCol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkCol);
            assertTrue("status: " + status, status == HttpStatus.SC_CREATED);



            put = new PutMethod(testuri);
            status = this.client.executeMethod(put);
            assertTrue("status: " + status, status == HttpStatus.SC_CREATED);

            MoveMethod moveNormal = new MoveMethod(testuri, destinationpath, true);
            status = this.client.executeMethod(moveNormal);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED || status == HttpStatus.SC_NO_CONTENT);

            HeadMethod head = new HeadMethod(destinationuri);
            status = this.client.executeMethod(head);
            //We get back HttpStatus.SC_NO_CONTENT 
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);

            head = new HeadMethod(testuri);
            status = this.client.executeMethod(head);
            assertTrue("status: " + status, status == HttpStatus.SC_NOT_FOUND);

        } finally {
            DeleteMethod delete = new DeleteMethod(testuri);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT || status == HttpStatus.SC_NOT_FOUND);

            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT || status == HttpStatus.SC_NOT_FOUND);


            delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT || status == HttpStatus.SC_NOT_FOUND);

        }
    }

    public void testPutIfEtag() throws HttpException, IOException, DavException, URISyntaxException {
        System.out.println("testPutIfEtag");
        String testcol = this.root + "testResourceId/";
        String testuri = testcol + "iftest";
        int status;
        try {

            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            PutMethod put = new PutMethod(testuri);
            String condition = "<" + testuri + "> ([" + "\"an-etag-this-testcase-invented\"" + "])";
            put.setRequestEntity(new StringRequestEntity("1"));
            put.setRequestHeader("If", condition);
            status = this.client.executeMethod(put);
            assertEquals("status: " + status, HttpStatus.SC_PRECONDITION_FAILED, status);
        } finally {
            DeleteMethod delete = new DeleteMethod(testuri);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT || status == HttpStatus.SC_NOT_FOUND);
        }
    }

    //No Lock yet 
//    @Test
//    public void testPutIfLockToken() throws HttpException, IOException, DavException, URISyntaxException {
//
//        String testuri = this.root + "iflocktest";
//        String locktoken = null;
//
//        int status;
//        try {
//            PutMethod put = new PutMethod(testuri);
//            put.setRequestEntity(new StringRequestEntity("1"));
//            status = this.client.executeMethod(put);
//            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED || status == HttpStatus.SC_NO_CONTENT);
//
//            LockMethod lock = new LockMethod(testuri, new LockInfo(
//                    Scope.EXCLUSIVE, Type.WRITE, "testcase", 10000, true));
//            status = this.client.executeMethod(lock);
//            assertEquals("status", HttpStatus.SC_OK, status);
//            locktoken = lock.getLockToken();
//            assertNotNull(locktoken);
//
//            // try to overwrite without lock token
//            put = new PutMethod(testuri);
//            put.setRequestEntity(new StringRequestEntity("2"));
//            status = this.client.executeMethod(put);
//            assertEquals("status: " + status, 423, status);
//
//            // try to overwrite using bad lock token
//            put = new PutMethod(testuri);
//            put.setRequestEntity(new StringRequestEntity("2"));
//            put.setRequestHeader("If", "(<" + "DAV:foobar" + ">)");
//            status = this.client.executeMethod(put);
//            assertEquals("status: " + status, 412, status);
//
//            // try to overwrite using correct lock token, using  No-Tag-list format
//            put = new PutMethod(testuri);
//            put.setRequestEntity(new StringRequestEntity("2"));
//            put.setRequestHeader("If", "(<" + locktoken + ">)");
//            status = this.client.executeMethod(put);
//            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
//
//            // try to overwrite using correct lock token, using Tagged-list format
//            // and full URI
//            put = new PutMethod(testuri);
//            put.setRequestEntity(new StringRequestEntity("3"));
//            put.setRequestHeader("If", "<" + testuri + ">" + "(<" + locktoken + ">)");
//            status = this.client.executeMethod(put);
//            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
//
//            // try to overwrite using correct lock token, using Tagged-list format
//            // and absolute path only
//            put = new PutMethod(testuri);
//            put.setRequestEntity(new StringRequestEntity("4"));
//            put.setRequestHeader("If", "<" + new URI(testuri).getRawPath() + ">" + "(<" + locktoken + ">)");
//            status = this.client.executeMethod(put);
//            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
//
//            // try to overwrite using correct lock token, using Tagged-list format
//            // and bad path
//            put = new PutMethod(testuri);
//            put.setRequestEntity(new StringRequestEntity("5"));
//            put.setRequestHeader("If", "</foobar>" + "(<" + locktoken + ">)");
//            status = this.client.executeMethod(put);
//            assertTrue("status: " + status, status == HttpStatus.SC_NOT_FOUND || status == 412);
//        } finally {
//            DeleteMethod delete = new DeleteMethod(testuri);
//            if (locktoken != null) {
//                delete.setRequestHeader("If", "(<" + locktoken + ">)");
//            }
//            status = this.client.executeMethod(delete);
//            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT || status == HttpStatus.SC_NOT_FOUND);
//        }
//    }
//
    @Test
    public void testPropfindInclude() throws HttpException, IOException, DavException, URISyntaxException {
        System.out.println("testPropfindInclude");
        String testcol = this.root + "testPropfindInclude/";
        String testuri = testcol + "iftest/ ";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            PutMethod put = new PutMethod(testuri);
            put.setRequestEntity(new StringRequestEntity("1"));
            status = this.client.executeMethod(put);
            assertEquals("status: " + status, HttpStatus.SC_CREATED, status);

            DavPropertyNameSet names = new DavPropertyNameSet();
            names.add(DeltaVConstants.COMMENT);
            PropFindMethod propfind = new PropFindMethod(testuri, DavConstants.PROPFIND_ALL_PROP_INCLUDE, names, 0);
            status = client.executeMethod(propfind);
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);

            MultiStatus multistatus = propfind.getResponseBodyAsMultiStatus();
            MultiStatusResponse[] responses = multistatus.getResponses();
            assertEquals(1, responses.length);

            MultiStatusResponse response = responses[0];
            DavPropertySet found = response.getProperties(HttpStatus.SC_OK);
            DavPropertySet notfound = response.getProperties(HttpStatus.SC_NOT_FOUND);

            //No comments yet
//            assertTrue(found.contains(DeltaVConstants.COMMENT) || notfound.contains(DeltaVConstants.COMMENT));
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT || status == HttpStatus.SC_NOT_FOUND);
        }
    }

    @Test
    public void testGetDataDistribution() throws UnsupportedEncodingException, IOException, DavException {
        System.out.println("testGetDataDistribution");
        String testcol1 = this.root + "testResourceId/";
        String testuri1 = testcol1 + "file1";
        String testuri2 = testcol1 + "file2";
        String testuri3 = testcol1 + "file3";
        String testcol2 = testcol1 + "folder4/";
        String testuri4 = testcol2 + "file5";
        try {

            DeleteMethod delete = new DeleteMethod(testcol1);
            int status = client.executeMethod(delete);


            MkColMethod mkcol = new MkColMethod(testcol1);
            status = client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            PutMethod put = new PutMethod(testuri1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            put = new PutMethod(testuri2);
            put.setRequestEntity(new StringRequestEntity("dar", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);


            put = new PutMethod(testuri3);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);


            mkcol = new MkColMethod(testcol2);
            status = client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            put = new PutMethod(testuri4);
            put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            DavPropertyNameSet d = new DavPropertyNameSet();
            DavPropertyName dataDist = DavPropertyName.create("data-distribution", Namespace.getNamespace("custom:"));
            d.add(dataDist);

            PropFindMethod propFind = new PropFindMethod(testcol1, d, DavConstants.DEPTH_INFINITY);
            status = client.executeMethod(propFind);
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);


            MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
            MultiStatusResponse[] responses = multiStatus.getResponses();

            for (MultiStatusResponse r : responses) {
//                System.out.println("Responce: " + r.getHref());
                DavPropertySet allProp = getProperties(r);

                DavPropertyIterator iter = allProp.iterator();
                while (iter.hasNext()) {
                    DavProperty<?> p = iter.nextProperty();
                    System.out.println("\tName: " + p.getName() + " Values " + p.getValue());
                    System.out.println("\tName: " + dataDist);
                    System.out.println("\tName: " + dataDist.getName());
                    System.out.println("\tName: " + dataDist.getNamespace());
                    assertEquals(dataDist.getName(), p.getName().getName());
                    assertNotNull(p.getValue());
                }
            }

        } finally {
            DeleteMethod delete = new DeleteMethod(testcol1);
            int status = client.executeMethod(delete);
            assertTrue("DeleteMethod status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        }
    }

    @Test
    public void testGetSetDRISupervisedProp() throws UnsupportedEncodingException, IOException, DavException {
        System.out.println("testGetSetDRISupervisedProp");
        String testcol1 = this.root + "testResourceId/";
        String testuri1 = testcol1 + "file1";
        String testuri2 = testcol1 + "file2";
        String testuri3 = testcol1 + "file3";
        String testcol2 = testcol1 + "folder4/";
        String testuri4 = testcol2 + "file5";
        try {

            DeleteMethod delete = new DeleteMethod(testcol1);
            int status = client.executeMethod(delete);


            MkColMethod mkcol = new MkColMethod(testcol1);
            status = client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            PutMethod put = new PutMethod(testuri1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            put = new PutMethod(testuri2);
            put.setRequestEntity(new StringRequestEntity("dar", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);


            put = new PutMethod(testuri3);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);


            mkcol = new MkColMethod(testcol2);
            status = client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            put = new PutMethod(testuri4);
            put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            DavPropertyNameSet driSupervisedNameSet = new DavPropertyNameSet();
            DavPropertyName driSupervisedName = DavPropertyName.create("dri-supervised", Namespace.getNamespace("custom:"));
            driSupervisedNameSet.add(driSupervisedName);

            PropFindMethod propFind = new PropFindMethod(testcol1, driSupervisedNameSet, DavConstants.DEPTH_INFINITY);
            status = client.executeMethod(propFind);
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);


            MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
            MultiStatusResponse[] responses = multiStatus.getResponses();

            for (MultiStatusResponse r : responses) {
//                System.out.println("Responce: " + r.getHref());
                DavPropertySet allProp = getProperties(r);

                DavPropertyIterator iter = allProp.iterator();
                while (iter.hasNext()) {
                    DavProperty<?> p = iter.nextProperty();
                    assertEquals(p.getName(), driSupervisedName);
//                    System.out.println("\tName: " + p.getName() + " Values " + p.getValue());
                    assertNotNull(p.getValue());
                }
            }

            DavPropertySet driSuper = new DavPropertySet();
            DavProperty<Boolean> driProp = new DefaultDavProperty<Boolean>(driSupervisedName, Boolean.TRUE);
            driSuper.add(driProp);
            PropPatchMethod proPatch = new PropPatchMethod(testcol1, driSuper, driSupervisedNameSet);
            status = client.executeMethod(proPatch);
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);


            multiStatus = proPatch.getResponseBodyAsMultiStatus();
            responses = multiStatus.getResponses();

            for (MultiStatusResponse r : responses) {
//                System.out.println("Responce: " + r.getHref());
                DavPropertySet allProp = getProperties(r);

                DavPropertyIterator iter = allProp.iterator();
                while (iter.hasNext()) {
                    DavProperty<?> p = iter.nextProperty();
//                    System.out.println("\tName: " + p.getName() + " Values " + p.getValue());
                }
            }


            propFind = new PropFindMethod(testcol1, driSupervisedNameSet, DavConstants.DEPTH_INFINITY);
            status = client.executeMethod(propFind);
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);


            multiStatus = propFind.getResponseBodyAsMultiStatus();
            responses = multiStatus.getResponses();

            for (MultiStatusResponse r : responses) {

                DavPropertySet allProp = getProperties(r);

                DavPropertyIterator iter = allProp.iterator();
                while (iter.hasNext()) {
                    DavProperty<?> p = iter.nextProperty();
                    assertEquals(p.getName(), driSupervisedName);
//                    System.out.println("\tName: " + p.getName() + " Values " + p.getValue());
                    assertNotNull(p.getValue());
                    boolean val = Boolean.valueOf(p.getValue().toString());
                    if (new URL(testcol1).getPath().equals(r.getHref())) {
                        assertTrue(val);
                    } else {
                        assertFalse(val);
                    }
                }
            }


        } finally {
            DeleteMethod delete = new DeleteMethod(testcol1);
            int status = client.executeMethod(delete);
            assertTrue("DeleteMethod status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        }
    }

    @Test
    public void testGetSetDRICheckSumProp() throws UnsupportedEncodingException, IOException, DavException {
        System.out.println("testGetSetDRICheckSumProp");
        String testcol1 = this.root + "testResourceId/";
        String testuri1 = testcol1 + "file1";
        String testuri2 = testcol1 + "file2";
        String testuri3 = testcol1 + "file3";
        String testcol2 = testcol1 + "folder4/";
        String testuri4 = testcol2 + "file5";
        try {

            DeleteMethod delete = new DeleteMethod(testcol1);
            int status = client.executeMethod(delete);


            MkColMethod mkcol = new MkColMethod(testcol1);
            status = client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            PutMethod put = new PutMethod(testuri1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            put = new PutMethod(testuri2);
            put.setRequestEntity(new StringRequestEntity("dar", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);


            put = new PutMethod(testuri3);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);


            mkcol = new MkColMethod(testcol2);
            status = client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            put = new PutMethod(testuri4);
            put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            DavPropertyNameSet driSupervisedNameSet = new DavPropertyNameSet();
            DavPropertyName driChecksumName = DavPropertyName.create("dri-checksum-MD5", Namespace.getNamespace("custom:"));
            driSupervisedNameSet.add(driChecksumName);

            PropFindMethod propFind = new PropFindMethod(testcol1, driSupervisedNameSet, DavConstants.DEPTH_INFINITY);
            status = client.executeMethod(propFind);
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);


            MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
            MultiStatusResponse[] responses = multiStatus.getResponses();

            for (MultiStatusResponse r : responses) {
//                System.out.println("Responce: " + r.getHref());
                DavPropertySet allProp = getProperties(r);

                DavPropertyIterator iter = allProp.iterator();
                while (iter.hasNext()) {
                    DavProperty<?> p = iter.nextProperty();
                    assertEquals(p.getName(), driChecksumName);
                }
            }

            DavPropertySet driSuper = new DavPropertySet();
            Long checksum = Long.valueOf(10000);
            DavProperty<Long> driProp = new DefaultDavProperty<Long>(driChecksumName, checksum);
            driSuper.add(driProp);
            PropPatchMethod proPatch = new PropPatchMethod(testcol1, driSuper, driSupervisedNameSet);
            status = client.executeMethod(proPatch);
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);



            propFind = new PropFindMethod(testcol1, driSupervisedNameSet, DavConstants.DEPTH_INFINITY);
            status = client.executeMethod(propFind);
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);


            multiStatus = propFind.getResponseBodyAsMultiStatus();
            responses = multiStatus.getResponses();

            for (MultiStatusResponse r : responses) {
                DavPropertySet allProp = getProperties(r);

                DavPropertyIterator iter = allProp.iterator();
                while (iter.hasNext()) {
                    DavProperty<?> p = iter.nextProperty();
                    assertEquals(p.getName(), driChecksumName);
//                    assertNotNull(p.getValue());
                    if (new URL(testcol1).getPath().equals(r.getHref())) {
                        Long val = Long.valueOf(p.getValue().toString());
                        assertEquals(checksum, val);
                    }


                }
            }


        } finally {
            DeleteMethod delete = new DeleteMethod(testcol1);
            int status = client.executeMethod(delete);
            assertTrue("DeleteMethod status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        }
    }

    @Test
    public void testGetSetDriLastValidationdateProp() throws UnsupportedEncodingException, IOException, DavException {
        System.out.println("testGetSetDriLastValidationdateProp");
        String testcol1 = this.root + "testResourceId/";
        String testuri1 = testcol1 + "file1";
        String testuri2 = testcol1 + "file2";
        String testuri3 = testcol1 + "file3";
        String testcol2 = testcol1 + "folder4/";
        String testuri4 = testcol2 + "file5";
        try {

            DeleteMethod delete = new DeleteMethod(testcol1);
            int status = client.executeMethod(delete);


            MkColMethod mkcol = new MkColMethod(testcol1);
            status = client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            PutMethod put = new PutMethod(testuri1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            put = new PutMethod(testuri2);
            put.setRequestEntity(new StringRequestEntity("dar", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);


            put = new PutMethod(testuri3);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);


            mkcol = new MkColMethod(testcol2);
            status = client.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);

            put = new PutMethod(testuri4);
            put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            DavPropertyNameSet driSupervisedNameSet = new DavPropertyNameSet();
            DavPropertyName driLastValidationName = DavPropertyName.create("dri-last-validation-date-ms", Namespace.getNamespace("custom:"));
            driSupervisedNameSet.add(driLastValidationName);

            PropFindMethod propFind = new PropFindMethod(testcol1, driSupervisedNameSet, DavConstants.DEPTH_INFINITY);
            status = client.executeMethod(propFind);
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);


            MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
            MultiStatusResponse[] responses = multiStatus.getResponses();

            for (MultiStatusResponse r : responses) {
                System.out.println("Responce: " + r.getHref());
                DavPropertySet allProp = getProperties(r);

                DavPropertyIterator iter = allProp.iterator();
                while (iter.hasNext()) {
                    DavProperty<?> p = iter.nextProperty();
                    System.out.println(p.getName() + " : " + p.getValue());
                    assertEquals(p.getName(), driLastValidationName);
                }
            }

            DavPropertySet driSuper = new DavPropertySet();
            Long date = System.currentTimeMillis();
            DavProperty<Long> driProp = new DefaultDavProperty<Long>(driLastValidationName, date);
            driSuper.add(driProp);
            PropPatchMethod proPatch = new PropPatchMethod(testcol1, driSuper, driSupervisedNameSet);
            status = client.executeMethod(proPatch);
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);



            propFind = new PropFindMethod(testcol1, driSupervisedNameSet, DavConstants.DEPTH_INFINITY);
            status = client.executeMethod(propFind);
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);


            multiStatus = propFind.getResponseBodyAsMultiStatus();
            responses = multiStatus.getResponses();

            for (MultiStatusResponse r : responses) {
                DavPropertySet allProp = getProperties(r);

                DavPropertyIterator iter = allProp.iterator();
                while (iter.hasNext()) {
                    DavProperty<?> p = iter.nextProperty();
                    assertEquals(p.getName(), driLastValidationName);
//                    assertNotNull(p.getValue());
                    if (new URL(testcol1).getPath().equals(r.getHref())) {
                        Long val = Long.valueOf(p.getValue().toString());
                        assertEquals(date, val);
                    }


                }
            }
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol1);
            int status = client.executeMethod(delete);
            assertTrue("DeleteMethod status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        }
    }

    @Test
    public void testFileConsistency() throws IOException, NoSuchAlgorithmException {
        File testUploadFile = File.createTempFile("tmp", null);
        Random generator = new Random();
        byte buffer[] = new byte[1024];
        OutputStream out = new FileOutputStream(testUploadFile);
        for (int i = 0; i < 10; i++) {
            generator.nextBytes(buffer);
            out.write(buffer);
        }
        String lobcderFilePath = this.root + testUploadFile.getName();
        try {

            PutMethod method = new PutMethod(lobcderFilePath);
            RequestEntity requestEntity = new InputStreamRequestEntity(
                    new FileInputStream(testUploadFile));
            method.setRequestEntity(requestEntity);
            int status = client.executeMethod(method);
            assertEquals(HttpStatus.SC_CREATED, status);

            String localMD5 = checkChecksum(new FileInputStream(testUploadFile));
            GetMethod get = new GetMethod(lobcderFilePath);
            status = client.executeMethod(get);
            assertEquals(HttpStatus.SC_OK, status);

            InputStream in = get.getResponseBodyAsStream();
            String remoteMD5 = checkChecksum(in);
            assertEquals(localMD5, remoteMD5);
        } finally {
            DeleteMethod delete = new DeleteMethod(lobcderFilePath);
            int status = client.executeMethod(delete);
            assertTrue("DeleteMethod status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        }
    }

    private String checkChecksum(InputStream is) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");

        byte[] dataBytes = new byte[1024];

        int nread = 0;
        while ((nread = is.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        }
        byte[] mdbytes = md.digest();

        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    // utility methods
    // see http://greenbytes.de/tech/webdav/rfc5842.html#rfc.section.3.1
    private URI getResourceId(String uri) throws IOException, DavException, URISyntaxException {
        DavPropertyNameSet names = new DavPropertyNameSet();
        names.add(BindConstants.RESOURCEID);
        PropFindMethod propfind = new PropFindMethod(uri, names, 0);
        int status = this.client.executeMethod(propfind);
        assertEquals(207, status);

        MultiStatus multistatus = propfind.getResponseBodyAsMultiStatus();

        MultiStatusResponse[] responses = multistatus.getResponses();
        assertEquals(1, responses.length);

        DavProperty resourceId = responses[0].getProperties(HttpStatus.SC_OK).get(BindConstants.RESOURCEID);

        assertNotNull(resourceId);
        assertTrue(resourceId.getValue() instanceof Element);

        Element href = (Element) resourceId.getValue();

        assertEquals("href", href.getLocalName());
        String text = getUri(href);
        URI resid = new URI(text);
        return resid;
    }

    private DavProperty getParentSet(String uri) throws IOException, DavException, URISyntaxException {
        DavPropertyNameSet names = new DavPropertyNameSet();
        names.add(BindConstants.PARENTSET);
        PropFindMethod propfind = new PropFindMethod(uri, names, 0);
        int status = this.client.executeMethod(propfind);
        assertEquals(207, status);
        MultiStatus multistatus = propfind.getResponseBodyAsMultiStatus();
        MultiStatusResponse[] responses = multistatus.getResponses();
        assertEquals(1, responses.length);
        DavProperty parentset = responses[0].getProperties(HttpStatus.SC_OK).get(BindConstants.PARENTSET);
        assertNotNull(parentset);
        return parentset;
    }

    private DavPropertySet getProperties(MultiStatusResponse statusResponse) {
        Status[] status = statusResponse.getStatus();

        DavPropertySet allProp = new DavPropertySet();
        for (int i = 0; i < status.length; i++) {
            DavPropertySet pset = statusResponse.getProperties(status[i].getStatusCode());
            allProp.addAll(pset);
        }

        return allProp;
    }

    private String getUri(Element href) {
        String s = "";
        for (Node c = href.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c.getNodeType() == Node.TEXT_NODE) {
                s += c.getNodeValue();
            }
        }
        return s;
    }

    private void debug(String msg) {
        System.err.println(this.getClass().getName() + ": " + msg);
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import java.util.Map;
import com.bradmcevoy.http.Range;
import java.io.ByteArrayOutputStream;
import com.bradmcevoy.http.Resource;
import java.util.List;
import nl.uva.cs.lobcder.resources.IStorageSite;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.util.ConstantsAndSettings;
import java.io.ByteArrayInputStream;
import nl.uva.cs.lobcder.catalogue.SimpleDLCatalogue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author alogo
 */
public class WebDataResourceFactoryTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getResource method, of class WebDataResourceFactory.
     */
    @Test
    public void testGetResource() throws Exception {
        System.out.println("getResource");
        String host = "localhost:8080";
        String strPath = "/";
        WebDataResourceFactory instance = new WebDataResourceFactory();
//        Resource expResult = null;
        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, strPath);
        assertNotNull(result);

        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
        result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");

        result.delete();
    }

    /**
     * Test of getResource method, of class WebDataResourceFactory.
     */
    @Test
    public void testCreateGetAndDeleteResource() throws Exception {
        System.out.println("getResource");
        String host = "localhost:8080";

        WebDataResourceFactory instance = new WebDataResourceFactory();
        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.TEST_FOLDER_NAME);
        assertNotNull(result);

        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
        WebDataFileResource file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long("DATA".getBytes().length), "text/plain");
        Long len = file.getContentLength();
        assertEquals(len, new Long("DATA".getBytes().length));

        String acceps = "text/html,text/*;q=0.9";
        String res = file.getContentType(acceps);
        String expResult = "text/*;q=0.9";
        assertEquals(expResult, res);

        Date date = file.getCreateDate();
        assertNotNull(date);
        date = file.getModifiedDate();
        assertNotNull(date);


        String name = file.getName();
        assertEquals(ConstantsAndSettings.TEST_FILE_NAME_1, name);

        result.delete();

        instance = new WebDataResourceFactory();
        result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.TEST_FOLDER_NAME);
        assertNotNull(result);

        bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
        file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long("DATA".getBytes().length), "text/plain");
        len = file.getContentLength();
        assertEquals(len, new Long("DATA".getBytes().length));

        acceps = "text/html,text/*;q=0.9";
        res = file.getContentType(acceps);
        expResult = "text/*;q=0.9";
        assertEquals(expResult, res);

        date = file.getCreateDate();
        assertNotNull(date);
        date = file.getModifiedDate();
        assertNotNull(date);


        name = file.getName();
        assertEquals(ConstantsAndSettings.TEST_FILE_NAME_1, name);

        result.delete();

    }

    @Test
    public void testCreateGetAndDeleteResource2() throws Exception {

        System.out.println("getResource");
        String host = "localhost:8080";

        WebDataResourceFactory instance = new WebDataResourceFactory();
        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.TEST_FOLDER_NAME);
        assertNotNull(result);


        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
        WebDataFileResource file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long("DATA".getBytes().length), "text/plain");
        Long len = file.getContentLength();
        assertEquals(len, new Long("DATA".getBytes().length));

        String acceps = "text/html,text/*;q=0.9";
        String res = file.getContentType(acceps);
        String expResult = "text/*;q=0.9";
        assertEquals(expResult, res);

        Date date = file.getCreateDate();
        assertNotNull(date);
        date = file.getModifiedDate();
        assertNotNull(date);


        String name = file.getName();
        assertEquals(ConstantsAndSettings.TEST_FILE_NAME_1, name);

        file.delete();

        SimpleDLCatalogue cat = new SimpleDLCatalogue();
        ILogicalData entry = cat.getResourceEntryByLDRI(file.getPath());
        assertNull(entry);

        result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.TEST_FOLDER_NAME);
        assertNotNull(result);

        Collection<IStorageSite> sites = file.getStorageSites();

        System.out.println(">>>>>>Sites: " + sites.size());
        for (IStorageSite s : sites) {
            System.out.println(">>>>>>Sites: " + s.getEndpoint() + " " + s.getUID());
        }


        file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long("DATA".getBytes().length), "text/plain");
        len = file.getContentLength();
        assertEquals(len, new Long("DATA".getBytes().length));



        acceps = "text/html,text/*;q=0.9";
        res = file.getContentType(acceps);
        expResult = "text/*;q=0.9";
        assertEquals(expResult, res);

        date = file.getCreateDate();
        assertNotNull(date);
        date = file.getModifiedDate();
        assertNotNull(date);


        name = file.getName();
        assertEquals(ConstantsAndSettings.TEST_FILE_NAME_1, name);

        result.delete();
        cat = new SimpleDLCatalogue();
        entry = cat.getResourceEntryByLDRI(result.getPath());
        assertNull(entry);

    }

    @Test
    public void testStorageSites() throws Exception {

        System.out.println("testStorageSites");
        String host = "localhost:8080";

        WebDataResourceFactory instance = new WebDataResourceFactory();
        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.TEST_FOLDER_NAME);
        assertNotNull(result);

        Collection<IStorageSite> sites = result.getStorageSites();

        System.out.println(">>>>>>Sites: " + sites.size());
        for (IStorageSite s : sites) {
            System.out.println(">>>>>>Sites: " + s.getEndpoint());
        }



        instance = new WebDataResourceFactory();
        result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.TEST_FOLDER_NAME);
        assertNotNull(result);

        sites = result.getStorageSites();

        System.out.println(">>>>>>Sites: " + sites.size());
        for (IStorageSite s : sites) {
            System.out.println(">>>>>>Sites: " + s.getEndpoint());
        }
    }

    @Test
    public void testCreateAndGetResourceContent() throws Exception {
        System.out.println("testCreateAndGetResourceContent");
        String host = "localhost:8080";


        //1st PUT
        WebDataResourceFactory instance = new WebDataResourceFactory();
        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.TEST_FOLDER_NAME);
        assertNotNull(result);
        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
        WebDataFileResource file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long("\n".getBytes().length), "text/plain");
        Long len = file.getContentLength();
        assertEquals(len, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length));

        //1st GET
        instance = new WebDataResourceFactory();
        result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.TEST_FOLDER_NAME);
        List<? extends Resource> children = result.getChildren();
        assertNotNull(children);
        assertFalse(children.isEmpty());

        for (Resource r : children) {
            if (r.getName().equals(ConstantsAndSettings.TEST_FILE_NAME_1)) {
                file = (WebDataFileResource) r;
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Range range = null;
        Map<String, String> params = null;
        String contentType = "text/plain";
        file.sendContent(out, range, params, contentType);
        String content = new String(out.toByteArray());
        assertEquals(ConstantsAndSettings.TEST_DATA, content);
    }

    @Test
    public void testMultiThread() {
        try {
            System.out.println("testMultiThread");
            Thread userThread1 = new UserThread(1);
            userThread1.setName("T1");

            Thread userThread2 = new UserThread(1);
            userThread2.setName("T2");


            userThread1.start();
            userThread2.start();

            userThread1.join();
            userThread2.join();
        } catch (InterruptedException ex) {
            fail();
            Logger.getLogger(WebDataResourceFactoryTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static class UserThread extends Thread {

        private final int opNum;

        private UserThread(int opNmu) {
            this.opNum = opNmu;
        }

        @Override
        public void run() {
            try {
                switch (opNum) {
                    case 1:
                        op1();
                        break;
                    case 2:
                        op2();
                        break;
                    default:
                        op1();
                        break;
                }

            } catch (Exception ex) {
                fail(ex.getMessage());
                ex.printStackTrace();
            }
        }

        private void op1() throws Exception {
            String host = "localhost:8080";
            String fileName = null;
            if (this.getName().equals("T1")) {
                fileName = ConstantsAndSettings.TEST_FILE_NAME_1;
            } else if (this.getName().equals("T2")) {
                fileName = ConstantsAndSettings.TEST_FILE_NAME_2;
            }
            WebDataResourceFactory instance = new WebDataResourceFactory();
            WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.TEST_FOLDER_NAME);
            assertNotNull(result);

            ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
            WebDataFileResource file = (WebDataFileResource) result.createNew(fileName, bais, new Long("DATA".getBytes().length), "text/plain");
            Long len = file.getContentLength();
            assertEquals(len, new Long("DATA".getBytes().length));

            String acceps = "text/html,text/*;q=0.9";
            String res = file.getContentType(acceps);
            String expResult = "text/*;q=0.9";
            assertEquals(expResult, res);

            Date date = file.getCreateDate();
            assertNotNull(date);
            date = file.getModifiedDate();
            assertNotNull(date);


            String name = file.getName();
            assertEquals(fileName, name);

            result.delete();

            instance = new WebDataResourceFactory();
            result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.TEST_FOLDER_NAME);
            assertNotNull(result);

            bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
            file = (WebDataFileResource) result.createNew(fileName, bais, new Long("DATA".getBytes().length), "text/plain");
            len = file.getContentLength();
            assertEquals(len, new Long("DATA".getBytes().length));

            acceps = "text/html,text/*;q=0.9";
            res = file.getContentType(acceps);
            expResult = "text/*;q=0.9";
            assertEquals(expResult, res);

            date = file.getCreateDate();
            assertNotNull(date);
            date = file.getModifiedDate();
            assertNotNull(date);


            name = file.getName();
            assertEquals(fileName, name);

            result.delete();
        }

        private void op2() {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }
}
package nl.uva.cs.lobcder.catalogue;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import lombok.Data;
import lombok.Delegate;
import lombok.Getter;
import lombok.Setter;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;


import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.ws.http.HTTPException;
import java.io.*;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.java.Log;

/**
 * User: dvasunin Date: 13.02.2015 Time: 14:24 To change this template use File
 * | Settings | File Templates.
 */
@Log
public class WP4Sweep implements Runnable {

    private final DataSource datasource;
    private final String metadataRepository;

    public WP4Sweep(DataSource datasource) throws IOException {
        this.datasource = datasource;
        metadataRepository = PropertiesHelper.getMetadataRepositoryURL();

    }

    static enum FileType {

        File, Folder
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    static class FileWP4 {
        
        private String author;
        private String globalID;
        private String category;
        private String description;
        private String linkedTo;
        private Long localID;
        private String name;
        private Long rating;
        private String relatedResources;
        private String semanticAnnotations;
        private String status;
        private String type;
        private Integer views;
        private FileType fileType;
        private String subjectID;
    }

    @XmlRootElement(name = "resource_metadata")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class ResourceMetadata {

        @XmlElement(name = "file")
        @Delegate
        private FileWP4 file = new FileWP4();
    }

    @XmlRootElement(name = "resource_metadata_list")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class ResourceMetadataList {

        @XmlElement(name = "resource_metadata")
        @Getter
        @Setter
        private Collection<ResourceMetadata> resourceMetadataList;
    }

    private static String getGlobalIdForDelete(Collection<String> globalIdCollection) {

        StringBuilder sb = new StringBuilder();
        sb.append("<globalID_list>");
        for (String id : globalIdCollection) {
            sb.append(id).append(",");
        }
        sb.replace(sb.lastIndexOf(","), sb.length(), "");
        sb.append("</globalID_list>");

        //return "<globalID_list>d0e36173-82bd-40d4-ac7b-d7977715576a,bab924df-59f4-4dcb-8f0a-3b6675df18c9,547f6829-619f-42c8-b545-24d71bd7953f,fb41bbc2-5b7b-4771-a330-3d6527188cef, 0eccb919-bb98-4c11-bb94-f568da052169, 906f1012-c049-4ded-af57</globalID_list>";
        return sb.toString();
    }

    public static interface WP4ConnectorI {

        public ResourceMetadataList create(ResourceMetadataList resourceMetadataList) throws Exception;

        public ResourceMetadataList update(ResourceMetadataList resourceMetadataList) throws Exception;

        public ResourceMetadataList delete(Collection<String> globalIdCollection) throws Exception;
    }

    public static class WP4Connector implements WP4ConnectorI {

        private Client client;
        private final String uri;

        public WP4Connector(String uri) {
            this.uri = uri;

            client = Client.create();
            client.setReadTimeout(120000);
            client.setConnectTimeout(120000);

            /*
             HttpClient apacheClient = HttpClientBuilder.create().build();
             client = new Client(new ApacheHttpClient4Handler(apacheClient,
             new BasicCookieStore(),
             true));
             client.setReadTimeout(30000);
             client.setConnectTimeout(30000);
             */
            //WebResource webResource = client.resource("http://localhost:8080/path");
            //ClientResponse response = webResource.accept("application/json")
            //        .get(ClientResponse.class);   /home/dvasunin/.m2/repository/org/apache/httpcomponents/httpcore/4.3.3/httpcore-4.3.3.jar!/org/apache/http/impl/io/DefaultHttpRequestWriterFactory.class
        }

        /**
         *
         * @param resourceMetadataList
         * @return
         * @throws Exception
         */
        @Override
        public ResourceMetadataList create(ResourceMetadataList resourceMetadataList) throws Exception {
            if (resourceMetadataList.getResourceMetadataList().isEmpty()) {
                return null;
            }

            ClientResponse response = client.resource(uri).type(MediaType.APPLICATION_XML_TYPE).accept(MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, resourceMetadataList);
            if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
                throw new HTTPException(response.getStatus());
            }
            log.log(Level.FINE, "Send metadata to uri: {0}", new Object[]{uri});
            return response.getEntity(ResourceMetadataList.class);
        }

        /**
         *
         * @param resourceMetadataList
         * @return
         * @throws Exception
         */
        @Override
        public ResourceMetadataList update(ResourceMetadataList resourceMetadataList) throws Exception {
            if (resourceMetadataList.getResourceMetadataList().isEmpty()) {
                return null;
            }

            ClientResponse response = client.resource(uri).type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE)
                    .put(ClientResponse.class, resourceMetadataList);
            if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
                throw new HTTPException(response.getStatus());
            }
            log.log(Level.FINE, "Send metadata to uri: {0}", new Object[]{uri});
            return response.getEntity(ResourceMetadataList.class);
        }

        /*

         @Override
         public ResourceMetadataList delete(Collection<String> globalIdCollection) throws Exception {
         Logger.getLogger(WP4Connector.class.getName()).log(Level.INFO, "Start");
         WebResource webResource = client.resource(uri);
         ClientResponse response = webResource.type(MediaType.APPLICATION_XML).delete(ClientResponse.class, getGlobalIdForDelete(globalIdCollection));
         if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
         throw new Exception(uri + " responded with: " + response.getClientResponseStatus().toString() + ". Response Entity:" + response.getEntity(String.class));
         }
         Logger.getLogger(WP4Connector.class.getName()).log(Level.INFO, "Done");
         return response.getEntity(ResourceMetadataList.class);
         }
         */
        @Override
        public ResourceMetadataList delete(Collection<String> globalIdCollection) throws Exception {
            class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {

                public static final String METHOD_NAME = "DELETE";

                public String getMethod() {
                    return METHOD_NAME;
                }

                public HttpDeleteWithBody(final String uri) {
                    super();
                    setURI(URI.create(uri));
                }

                public HttpDeleteWithBody(final URI uri) {
                    super();
                    setURI(uri);
                }

                public HttpDeleteWithBody() {
                    super();
                }
            }

            /* Logger.getLogger(WP4Connector.class.getName()).log(Level.INFO, "Start");
             String jsonResponse = "";

             URL url = new URL(uri);
             HttpURLConnection connection = null;
             connection = (HttpURLConnection) url.openConnection();
             connection.setRequestMethod("POST");
             // We have to override the post method so we can send data
             connection.setRequestProperty("X-HTTP-Method-Override", "DELETE");
             connection.setRequestProperty("Content-Type", "application/xml");
             connection.setDoOutput(true);
             */

            if (globalIdCollection == null || globalIdCollection.isEmpty()) {
                return null;
            }

            HttpClient httpClient = new DefaultHttpClient();
            HttpDeleteWithBody request = new HttpDeleteWithBody(uri);
            request.setEntity(new StringEntity(getGlobalIdForDelete(globalIdCollection)));
            request.setHeader("Content-Type", "application/xml");

            HttpResponse response = httpClient.execute(request);


// Send request
            /*
             OutputStreamWriter wr = new OutputStreamWriter(
             connection.getOutputStream());
             wr.write(getGlobalIdForDelete(globalIdCollection));
             wr.flush();
             */
// Get Response

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new HTTPException(response.getStatusLine().getStatusCode());
            }
            log.log(Level.FINE, "Send metadata to uri: {0}", new Object[]{uri});
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                JAXBContext jaxbContext = JAXBContext.newInstance(ResourceMetadataList.class);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                ResourceMetadataList result = (ResourceMetadataList) jaxbUnmarshaller.unmarshal(rd);
                return result;
            } else {
                throw new Exception("ERROR in DELETE");
            }

        }
    }

    private void create(Connection connection, WP4ConnectorI wp4Connector) throws Exception {

        Collection<ResourceMetadata> resourceMetadataList;
        Map<Long, Long> resourceMetadataMap;
        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ResultSet rs = s1.executeQuery("SELECT uid, ownerId, datatype, ldName, id, views "
                    + "FROM ldata_table "
                    + "JOIN wp4_table ON uid=local_id "
                    + "WHERE need_create=TRUE LIMIT 300");
            int size = rs.getFetchSize();
            resourceMetadataList = new ArrayList<>(size);
            resourceMetadataMap = new HashMap<>(size);
            while (rs.next()) {
                ResourceMetadata rm = new ResourceMetadata();
                Long localId = rs.getLong(1);
                rm.setLocalID(localId);
                rm.setAuthor(rs.getString(2));
                rm.setFileType(rs.getString(3).equals("logical.file") ? FileType.File : FileType.Folder);
                rm.setName(rs.getString(4));
                rm.setCategory("General Metadata");
                rm.setDescription("LOBCDER");
                rm.setStatus("active");
                rm.setLinkedTo("");
                rm.setRating(0L);
                rm.setRelatedResources("");
                rm.setSemanticAnnotations("");
                rm.setViews(rs.getInt(6));
                rm.setSubjectID("");
                rm.setType("File");

                resourceMetadataMap.put(localId, rs.getLong(5));
                resourceMetadataList.add(rm);
            }
        }
        try (PreparedStatement s2 = connection.prepareStatement("UPDATE wp4_table SET need_create=FALSE, global_id=? WHERE id=?")) {
            ResourceMetadataList param = new ResourceMetadataList();
            param.setResourceMetadataList(resourceMetadataList);
            ResourceMetadataList rml = wp4Connector.create(param);
            if (rml != null && rml.getResourceMetadataList() != null) {
                for (ResourceMetadata rm : rml.getResourceMetadataList()) {
                    s2.setString(1, rm.getGlobalID());
                    s2.setLong(2, resourceMetadataMap.get(rm.getLocalID()));
                    s2.addBatch();
                }
                s2.executeBatch();
            }
        }

    }

    private void update(Connection connection, WP4ConnectorI wp4Connector) throws Exception {

        Collection<ResourceMetadata> resourceMetadataList;
        Map<Long, Long> resourceMetadataMap;
        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ResultSet rs = s1.executeQuery("SELECT ownerId, ldName, global_id, views, local_id, id FROM ldata_table JOIN wp4_table ON uid=local_id WHERE need_update=TRUE AND global_id IS NOT NULL LIMIT 2000");
            int size = rs.getFetchSize();
            resourceMetadataList = new ArrayList<>(size);
            resourceMetadataMap = new HashMap<>(size);
            while (rs.next()) {
                ResourceMetadata rm = new ResourceMetadata();
                rm.setAuthor(rs.getString(1));
                rm.setName(rs.getString(2));
                rm.setGlobalID(rs.getString(3));
                rm.setViews(rs.getInt(4));
                Long localId = rs.getLong(5);
                rm.setLocalID(localId);
                rm.setCategory("General Metadata");
                rm.setDescription("LOBCDER");
                rm.setStatus("active");
                rm.setSubjectID("");
                rm.setType("File");
                resourceMetadataMap.put(localId, rs.getLong(6));
                resourceMetadataList.add(rm);
            }
        }
        try (PreparedStatement s2 = connection.prepareStatement("UPDATE wp4_table SET need_update=FALSE WHERE id=?")) {
            ResourceMetadataList param = new ResourceMetadataList();
            param.setResourceMetadataList(resourceMetadataList);
            ResourceMetadataList rml = wp4Connector.update(param);
            if (rml != null && rml.getResourceMetadataList() != null) {
                for (ResourceMetadata rm : rml.getResourceMetadataList()) {
                    s2.setLong(1, resourceMetadataMap.get(rm.getLocalID()));
                    s2.addBatch();
                }
                s2.executeBatch();
            }
        }

    }

    private void delete(Connection connection, WP4ConnectorI wp4Connector) throws Exception {
        Map<String, Long> deleteMetadataMap = new HashMap<>();
        Set<Long> deleteMetadataSet = new HashSet<>();
        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ResultSet rs = s1.executeQuery("SELECT global_id, id FROM wp4_table WHERE local_id IS NULL LIMIT 2000");
            while (rs.next()) {
                String globalId = rs.getString(1);
                if (globalId == null) {
                    deleteMetadataSet.add(rs.getLong(2));
                } else {
                    deleteMetadataMap.put(globalId, rs.getLong(2));
                }
            }
        }
        try (PreparedStatement s2 = connection.prepareStatement("DELETE FROM wp4_table WHERE id=?")) {
            ResourceMetadataList rml = wp4Connector.delete(deleteMetadataMap.keySet());
            if (rml != null && rml.getResourceMetadataList() != null && !rml.getResourceMetadataList().isEmpty()) {
                for (ResourceMetadata rm : rml.getResourceMetadataList()) {
                    s2.setLong(1, deleteMetadataMap.get(rm.getGlobalID()));
                    s2.addBatch();
                }
            }
            for (Long id : deleteMetadataSet) {
                s2.setLong(1, id);
                s2.addBatch();
            }
            if ((rml != null && rml.getResourceMetadataList() != null && !rml.getResourceMetadataList().isEmpty()) || !deleteMetadataSet.isEmpty()) {
                s2.executeBatch();
            }
        }

    }

    @Override
    public void run() {

        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            WP4ConnectorI connector = new WP4Sweep.WP4Connector(metadataRepository);
            create(connection, connector);
            update(connection, connector);
            delete(connection, connector);
        } catch (Exception ex) {
            Logger.getLogger(WP4Sweep.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void serialize(ResourceMetadataList rml) throws JAXBException {

        JAXBContext jaxbContext = JAXBContext.newInstance(ResourceMetadataList.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        //Marshal the employees list in console
        jaxbMarshaller.marshal(rml, System.out);

    }

    public ResourceMetadataList unmarchaling() throws JAXBException {

        JAXBContext jaxbContext = JAXBContext.newInstance(ResourceMetadataList.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();


        //We had written this file in marshalling example
        return (ResourceMetadataList) jaxbUnmarshaller.unmarshal(new File("/tmp/test.xml"));


    }

    public static void main(String arg[]) {
        try {
            WP4Sweep wp4 = new WP4Sweep(null);

            Collection<ResourceMetadata> rml = new ArrayList<>();
            ResourceMetadata rm = new ResourceMetadata();
            Long localId = 123L;
            rm.setLocalID(localId);
            rm.setAuthor("dmitry");
            rm.setFileType(FileType.File);
            rm.setName("my_file");
            rm.setCategory("General Metadata");
            rm.setDescription("LOBCDER");
            rm.setStatus("active");

            rm.setLinkedTo("");
            rm.setRating(0L);
            rm.setRelatedResources("");
            rm.setSemanticAnnotations("");
            rm.setViews(0);
            rm.setSubjectID("");
            rm.setType("File");


            rml.add(rm);

            rm = new ResourceMetadata();
            localId = 456L;
            rm.setLocalID(localId);
            rm.setAuthor("Spiros");
            rm.setFileType(FileType.Folder);
            rm.setName("my_file");
            rm.setCategory("General Metadata");
            rm.setDescription("LOBCDER");
            rm.setStatus("active");

            rm.setLinkedTo("");
            rm.setRating(0L);
            rm.setRelatedResources("");
            rm.setSemanticAnnotations("");
            rm.setViews(0);
            rm.setSubjectID("");
            rm.setType("File");


            rml.add(rm);

            ResourceMetadataList param = new ResourceMetadataList();
            param.setResourceMetadataList(rml);

            wp4.serialize(param);

            WP4Connector wp4Connector = new WP4Connector("http://vphshare.atosresearch.eu/metadata-extended/rest/metadata/lobcder");

            ResourceMetadataList res = wp4Connector.create(param);

            wp4.serialize(res);
            Collection<String> globalIdCollection = new HashSet<>();
            for (ResourceMetadata rm1 : res.getResourceMetadataList()) {
                globalIdCollection.add(rm1.getGlobalID());
                System.out.println(rm1.getGlobalID());

                rm1.setAuthor("Spiros");
                //rm1.setFileType(FileType.Folder);
                rm1.setName("my_file");
                rm1.setCategory("General Metadata");
                rm1.setDescription("LOBCDER");
                rm1.setStatus("active");

                //rm1.setLinkedTo("");
                //rm1.setRating(0L);
                //rm1.setRelatedResources("");
                //rm1.setSemanticAnnotations("");
                rm1.setViews(0);
                rm1.setSubjectID("");
                rm1.setType("File");
            }

            wp4.serialize(res);
            res = wp4Connector.update(res);


            wp4.serialize(wp4Connector.delete(globalIdCollection));





        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof HTTPException) {
                System.out.println(((HTTPException) e).getStatusCode());
            }
        }
    }
}

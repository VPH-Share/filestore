package nl.uva.cs.lobcder.catalogue;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import lombok.Data;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.PropertiesHelper;

import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPathExpressionException;

/**
 * User: dvasunin Date: 25.02.13 Time: 16:31 To change this template use File |
 * Settings | File Templates.
 */
@Log
class WP4SweepOLD implements Runnable {

    private final DataSource datasource;
    private final String metadataRepository;
    private final String metadataRepositoryDev;
//    private long sleepTime = 100;

    public WP4SweepOLD(DataSource datasource) throws IOException {
        this.datasource = datasource;
        metadataRepository = PropertiesHelper.getMetadataRepositoryURL();
        metadataRepositoryDev = PropertiesHelper.getMetadataRepositoryDevURL();
    }

    @Data
    public static class ResourceMetadata {

        String author = "";
        long localId = 0;
        String globalId;
        String name = "";
        String type = "";
        int views = 0;

        public String getXml() {
            return new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                    .append("<resource_metadata>")
                    .append("<file>")
                    .append("<author>").append(author).append("</author>")
                    .append("<category>General Metadata</category>")
                    .append("<description>LOBCDER</description>")
                    .append("<localID>").append(localId).append("</localID>")
                    .append("<name>").append(name).append("</name>")
                    .append("<status>active</status>")
                    .append("<type>File</type>")
                    .append("<views>").append(views).append("</views>")
                    .append("<fileType>").append(type).append("</fileType>")
                    .append("</file>")
                    .append("</resource_metadata>").toString();
        }

        public String getXmlPost() {
            return new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                    .append("<resource_metadata>")
                    .append("<file>")
                    .append("<author>").append(author).append("</author>")
                    .append("<category>General Metadata</category>")
                    .append("<description>LOBCDER</description>")
                    .append("<linkedTo/>")
                    .append("<localID>").append(localId).append("</localID>")
                    .append("<name>").append(name).append("</name>")
                    .append("<rating>0</rating>")
                    .append("<relatedResources/>")
                    .append("<semanticAnnotations/>")
                    .append("<status>active</status>")
                    .append("<type>File</type>")
                    .append("<views>").append(views).append("</views>")
                    .append("<fileType>").append(type).append("</fileType>")
                    .append("<subjectID/>")
                    .append("</file>")
                    .append("</resource_metadata>").toString();
        }
    }

    public static interface WP4ConnectorI {

        public String create(ResourceMetadata resourceMetadata) throws Exception;

        public String create_dev(ResourceMetadata resourceMetadata) throws Exception;

        public void update(ResourceMetadata resourceMetadata) throws Exception;

        public void update_dev(ResourceMetadata resourceMetadata) throws Exception;

        public void delete(String global_id) throws Exception;

        public void delete_dev(String global_id) throws Exception;
    }

    public static class WP4Connector implements WP4ConnectorI {

        private Client client;
        private XPathExpression expression;
        private String uri;
        private String uri_dev;

        public WP4Connector(String uri, String uri_dev) {
            try {

                this.uri = uri;
                this.uri_dev = uri_dev;
                client = Client.create();
                client.setReadTimeout(30000);
                client.setConnectTimeout(30000);
                XPathFactory xpf = XPathFactory.newInstance();
                XPath xpath = xpf.newXPath();
                expression = xpath.compile("/message/data[1]/_global_id[1]");

                //                throw new RuntimeException(e);
            } catch (XPathExpressionException ex) {
                Logger.getLogger(WP4SweepOLD.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public String create(ResourceMetadata resourceMetadata) {
            WebResource webResource = client.resource(uri);
            ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class,
                    resourceMetadata.getXmlPost());
            String entity = response.getEntity(String.class);
            if (response.getClientResponseStatus() == ClientResponse.Status.OK
                    && entity.contains("<_global_id>")) {
                String result = (String) entity.subSequence(entity.lastIndexOf("<_global_id>") + "<_global_id>".length(), entity.indexOf("</_global_id>"));
//                Node uidNode = (Node) expression.evaluate(new InputSource(response.getEntityInputStream()), XPathConstants.NODE);
//                String result = uidNode.getTextContent();
                log.log(Level.FINE, "Send metadata to uri: {0} author: {1} name: {2} type: {3} global_id: {4}", new Object[]{uri, resourceMetadata.author, resourceMetadata.name, resourceMetadata.type, result});
                return result;
            } else {
                //throw new Exception(uri + " responded with: " + response.getClientResponseStatus().toString() + ". Response Entity:" + entity);
                log.log(Level.SEVERE, "{0} responded with: {1}. Response Entity:{2}", new Object[]{uri, response.getClientResponseStatus().toString(), entity});
            }
            return null;
        }

        @Override
        public String create_dev(ResourceMetadata resourceMetadata) throws Exception {
            WebResource webResource = client.resource(uri_dev);
            ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, resourceMetadata.getXmlPost());
            String entity = response.getEntity(String.class);
            if (response.getClientResponseStatus() == ClientResponse.Status.OK
                    && entity.contains("<_global_id>")) {
                String result = (String) entity.subSequence(entity.lastIndexOf("<_global_id>") + "<_global_id>".length(), entity.indexOf("</_global_id>"));

//                Node uidNode = (Node) expression.evaluate(new InputSource(response.getEntityInputStream()), XPathConstants.NODE);
//                String result = uidNode.getTextContent();
                log.log(Level.FINE, "Send metadata to uri: {0} author: {1} name: {2} type: {3} global_id: {4}", new Object[]{uri_dev, resourceMetadata.author, resourceMetadata.name, resourceMetadata.type, result});
                return result;
            } else {
//                throw new Exception(uri_dev + " responded with: " + response.getClientResponseStatus().toString() + ". Response Entity:" + entity);
                log.log(Level.SEVERE, "{0} responded with: {1}. Response Entity:{2}", new Object[]{uri_dev, response.getClientResponseStatus().toString(), entity});
            }
            return null;
        }

        @Override
        public void update(ResourceMetadata resourceMetadata) {
            try {
                WebResource webResource = client.resource(uri);
                ClientResponse response = webResource.path(resourceMetadata.getGlobalId()).type(MediaType.APPLICATION_XML).put(ClientResponse.class, resourceMetadata.getXml());
                String entity = response.getEntity(String.class);
                if (response.getClientResponseStatus() != ClientResponse.Status.OK
                        || entity.contains("Error trying to create resource metadata in the system")) {
//                throw new Exception(uri + " responded with: " + response.getClientResponseStatus().toString() + ". Response Entity:" + entity);
                    log.log(Level.SEVERE, "{0} responded with: {1}. Response Entity:{2}", new Object[]{uri, response.getClientResponseStatus().toString(), entity});
                }
                log.log(Level.FINE, "Send metadata to uri: {0} author: {1} name: {2} type: {3} global_id: {4}", new Object[]{uri, resourceMetadata.author, resourceMetadata.name, resourceMetadata.type, resourceMetadata.globalId});
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "update encountered and error.", e);
                return; // Keep working
            } catch (Throwable e) {
                log.log(Level.SEVERE, "update encountered and error.", e);
            }
        }

        @Override
        public void update_dev(ResourceMetadata resourceMetadata) throws Exception {
            WebResource webResource = client.resource(uri_dev);
            ClientResponse response = webResource.path(resourceMetadata.getGlobalId()).type(MediaType.APPLICATION_XML).put(ClientResponse.class, resourceMetadata.getXml());
            String entity = response.getEntity(String.class);
            if (response.getClientResponseStatus() != ClientResponse.Status.OK
                    || entity.contains("Error trying to create resource metadata in the system")) {
//                throw new Exception(uri_dev + " responded with: " + response.getClientResponseStatus().toString() + ". Response Entity:" + entity);
                log.log(Level.SEVERE, "{0} responded with: {1}. Response Entity:{2}", new Object[]{uri_dev, response.getClientResponseStatus().toString(), entity});
            }
            log.log(Level.FINE, "Send metadata to uri: {0} author: {1} name: {2} type: {3} global_id: {4}", new Object[]{uri_dev, resourceMetadata.author, resourceMetadata.name, resourceMetadata.type, resourceMetadata.globalId});
        }

        @Override
        public void delete(String global_id) throws Exception {
            WebResource webResource = client.resource(uri);
            webResource.path(global_id).type(MediaType.APPLICATION_XML).delete();
            log.log(Level.FINE, "Deleting metadata from: {0} global_id: {1}", new Object[]{uri, global_id});
        }

        @Override
        public void delete_dev(String global_id) throws Exception {
            WebResource webResource = client.resource(uri_dev).path(global_id);
            WebResource.Builder wr = webResource.type(MediaType.APPLICATION_XML);
            try {
                wr.delete();
                log.log(Level.FINE, "Deleting metadata from: {0} global_id: {1}", new Object[]{uri_dev, global_id});
            } catch (UniformInterfaceException | ClientHandlerException ex) {
//                if(!ex.getMessage().contains("Read timed out")){
//                    throw ex;
//                }
                log.log(Level.WARNING, "Did not delete metadata from: {0} global_id: {1}", new Object[]{uri_dev, global_id});
            }
        }
    }

    private void create(Connection connection, WP4ConnectorI wp4Connector) throws SQLException {

        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY)) {
            try (PreparedStatement s2 = connection.prepareStatement("UPDATE wp4_table SET need_create = FALSE, "
                    + "global_id = ?, global_id_dev = ? WHERE id = ?")) {
                ResultSet rs = s1.executeQuery("SELECT uid, ownerId, datatype, "
                        + "ldName, id FROM ldata_table JOIN wp4_table ON uid=local_id WHERE need_create=TRUE LIMIT 10");
                while (rs.next()) {
                    ResourceMetadata rm = new ResourceMetadata();
                    rm.setLocalId(rs.getLong(1));
                    rm.setAuthor(rs.getString(2));
                    rm.setType(rs.getString(3).equals("logical.file") ? "File" : "Folder");
                    rm.setName(rs.getString(4));
                    rm.setViews(0);

                    String gid = wp4Connector.create(rm);
                    String gid_dev = wp4Connector.create_dev(rm);
                    s2.setString(1, gid);
                    s2.setString(2, gid_dev);
                    s2.setLong(3, rs.getLong(5));
                    s2.executeUpdate();
                }
            } catch (Exception ex) {
//                connection.rollback();
//                connection.close();
                Logger.getLogger(WP4SweepOLD.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        //
    }

    private void update(Connection connection, WP4ConnectorI wp4Connector) throws SQLException {

        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY)) {
            try (PreparedStatement s2 = connection.prepareStatement("UPDATE wp4_table SET need_update = FALSE WHERE id = ?")) {
                ResultSet rs = s1.executeQuery("SELECT ownerId, ldName, global_id, "
                        + "views, id, global_id_dev, local_id FROM ldata_table "
                        + "JOIN wp4_table ON uid=local_id WHERE need_update=TRUE LIMIT 10");
                while (rs.next()) {
                    ResourceMetadata rm = new ResourceMetadata();
                    rm.setAuthor(rs.getString(1));
                    rm.setName(rs.getString(2));
                    rm.setGlobalId(rs.getString(3));
                    rm.setViews(rs.getInt(4));
                    rm.setLocalId(rs.getInt(7));

                    wp4Connector.update(rm);
                    String global_id_dev = rs.getString(6);
                    if (global_id_dev != null && global_id_dev.length() >= 1) {
                        rm.setGlobalId(global_id_dev);
                        wp4Connector.update_dev(rm);
                    }
                    s2.setLong(1, rs.getLong(5));
                    s2.executeUpdate();
                }
            } catch (Exception ex) {
//                connection.rollback();
//                connection.close();
                Logger.getLogger(WP4SweepOLD.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }

    private void delete(Connection connection, WP4ConnectorI wp4Connector) throws SQLException {
        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_UPDATABLE)) {
            ResultSet rs = s1.executeQuery("SELECT global_id, id, global_id_dev FROM wp4_table WHERE local_id IS NULL LIMIT 10");
            while (rs.next()) {

                String global_id = rs.getString(1);
                String global_id_dev = rs.getString(3);
                if (global_id != null) {
                    wp4Connector.delete(global_id);
                }
                if (global_id_dev != null) {
                    wp4Connector.delete_dev(global_id_dev);
                }
                rs.deleteRow();
            }
        } catch (Exception ex) {
//            connection.rollback();
//            connection.close();
            Logger.getLogger(WP4SweepOLD.class.getName()).log(Level.WARNING, null, ex);
        }
        //
    }

    @Override
    public void run() {
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            WP4ConnectorI connector = new WP4SweepOLD.WP4Connector(metadataRepository, metadataRepositoryDev);
            create(connection, connector);
            update(connection, connector);
            delete(connection, connector);
        } catch (SQLException ex) {
            Logger.getLogger(WP4SweepOLD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

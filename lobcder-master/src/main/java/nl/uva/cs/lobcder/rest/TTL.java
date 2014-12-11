package nl.uva.cs.lobcder.rest;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.CatalogueHelper;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.PropertiesHelper;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by dvasunin on 09.12.14.
 */

@Log
@Path("ttl/")
public class TTL extends CatalogueHelper {

    @Context
    HttpServletRequest request;

    @Path("{uid}/{ttl}")
    @PUT
    public void setTTL(@PathParam("uid") Long uid, @PathParam("ttl")  Integer ttl){
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        try (Connection cn = getCatalogue().getConnection()) {
            try (PreparedStatement ps = cn.prepareStatement("SELECT uid, ownerId, ttlSec FROM ldata_table WHERE uid=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
                ps.setLong(1, uid);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                } else {
                    String owner = rs.getString(2);
                    Permissions p = getCatalogue().getPermissions(uid, owner, cn);
                    if (!mp.canWrite(p)) {
                        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
                    }
                    rs.updateInt(3, ttl);
                    rs.updateRow();
                    cn.commit();
                }
            } catch (SQLException ex) {
                log.log(Level.SEVERE, null, ex);
                cn.rollback();
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @Path("{ttl}")
    @PUT
    public void setTTL(@QueryParam("path") String pathStr, @PathParam("ttl")  Integer ttl){
        try(Connection cn = getCatalogue().getConnection()) {
            if(pathStr == null){
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            Long uid = getCatalogue().getLogicalDataUidByPath(io.milton.common.Path.path(pathStr), cn);
            if(uid == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            setTTL(uid, ttl);
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Path("getdir/{ttl}")
    @GET
    public String getDir(@PathParam("ttl")  Integer ttl) {
        if(PropertiesHelper.getTmpDirUid() == null){
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        try(Connection cn = getCatalogue().getConnection()) {
            try {
                LogicalData newFolderEntry = new LogicalData();
                newFolderEntry.setType(Constants.LOGICAL_FOLDER);
                newFolderEntry.setParentRef(PropertiesHelper.getTmpDirUid());
                newFolderEntry.setName(UUID.randomUUID().toString());
                newFolderEntry.setCreateDate(System.currentTimeMillis());
                newFolderEntry.setModifiedDate(System.currentTimeMillis());
                newFolderEntry.setLastAccessDate(System.currentTimeMillis());
                newFolderEntry.setTtlSec(ttl);
                newFolderEntry.setOwner(mp.getUserId());
                getCatalogue().setPermissions(
                        getCatalogue().registerDirLogicalData(newFolderEntry, cn).getUid(),
                        new Permissions(mp, new Permissions()), cn);
                cn.commit();
                return newFolderEntry.getName();
            } catch (Exception ex) {
                cn.rollback();
                log.log(Level.SEVERE, null, ex);
                throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (SQLException sqle) {
            log.log(Level.SEVERE, null, sqle);
            throw new WebApplicationException(sqle, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
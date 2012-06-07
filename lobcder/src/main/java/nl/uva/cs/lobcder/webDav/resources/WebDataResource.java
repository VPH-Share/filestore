/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.MyPrincipal.Exception;
import nl.uva.cs.lobcder.auth.PrincipalCache;
import nl.uva.cs.lobcder.auth.test.MyAuth;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.frontend.WebDavServlet;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.IStorageSite;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataResource implements PropFindableResource, Resource {

    private ILogicalData logicalData;
    private final IDLCatalogue catalogue;
    private static final boolean debug = false;
    private Map<String, CustomProperty> properties;
    //Collection<Integer> roles = null;

    public WebDataResource(IDLCatalogue catalogue, ILogicalData logicalData) {
        this.logicalData = logicalData;
//        if (!logicalData.getType().equals(Constants.LOGICAL_DATA)) {
//            throw new Exception("The logical data has the wonrg type: " + logicalData.getType());
//        }
        this.catalogue = catalogue;
        properties = new HashMap<String, CustomProperty>();
    }

    @Override
    public Date getCreateDate() {
        debug("getCreateDate.");
        if (getLogicalData().getMetadata() != null && getLogicalData().getMetadata().getCreateDate() != null) {
            return new Date(getLogicalData().getMetadata().getCreateDate());
        }
        return null;
    }

    @Override
    public String getUniqueId() {
        debug("getUniqueId.");
        return String.valueOf(getLogicalData().getUID());
    }

    @Override
    public String getName() {
        debug("getName.");
        return getLogicalData().getLDRI().getName();
    }

    @Override
    public Object authenticate(String user, String password) {
        MyPrincipal principal = null;
        debug("authenticate.\n"
                + "\t user: " + user
                + "\t password: " + password);
        try {
            ArrayList<Integer> roles = new ArrayList<Integer>();
            roles.add(0);
            String token = user + password;
            principal = PrincipalCache.pcache.getPrincipal(token);
            if (principal == null) {
                principal = new MyPrincipal(token, MyAuth.getInstance().checkToken(token));
                PrincipalCache.pcache.putPrincipal(principal);
            }
            WebDavServlet.request().setAttribute("vph-user", principal);
        } catch (Exception ex) {
            Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, null, ex);

        }
        return principal;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {

        //Object permission = getPermissionForTheLogicalData();
        boolean authorized = true;
        if (authorized) {
            String absPath = null;
            String absURL = null;
            String acceptHeader = null;
            String fromAddress = null;
            String remoteAddr = null;
            String cnonce = null;
            String nc = null;
            String nonce = null;
            String password = null;
            String qop = null;
            String relm = null;
            String responseDigest = null;
            String uri = null;
            String user = null;
            Object tag = null;
            if (request != null) {
                absPath = request.getAbsolutePath();
                absURL = request.getAbsoluteUrl();
                acceptHeader = request.getAcceptHeader();
                fromAddress = request.getFromAddress();
                remoteAddr = request.getRemoteAddr();
            }
            if (auth != null) {
                cnonce = auth.getCnonce();
                nc = auth.getNc();
                nonce = auth.getNonce();
                password = auth.getPassword();
                qop = auth.getQop();
                relm = auth.getRealm();
                responseDigest = auth.getResponseDigest();
                uri = auth.getUri();
                user = auth.getUser();
                tag = auth.getTag();
            }
            debug("authorise. \n"
                    + "\t request.getAbsolutePath(): " + absPath + "\n"
                    + "\t request.getAbsoluteUrl(): " + absURL + "\n"
                    + "\t request.getAcceptHeader(): " + acceptHeader + "\n"
                    + "\t request.getFromAddress(): " + fromAddress + "\n"
                    + "\t request.getRemoteAddr(): " + remoteAddr + "\n"
                    + "\t auth.getCnonce(): " + cnonce + "\n"
                    + "\t auth.getNc(): " + nc + "\n"
                    + "\t auth.getNonce(): " + nonce + "\n"
                    + "\t auth.getPassword(): " + password + "\n"
                    + "\t auth.getQop(): " + qop + "\n"
                    + "\t auth.getRealm(): " + relm + "\n"
                    + "\t auth.getResponseDigest(): " + responseDigest + "\n"
                    + "\t auth.getUri(): " + uri + "\n"
                    + "\t auth.getUser(): " + user + "\n"
                    + "\t auth.getTag(): " + tag);

            Collection<IStorageSite> sites = getLogicalData().getStorageSites();
            if (sites == null || sites.isEmpty()) {
                try {
                    sites = (Collection<IStorageSite>) getCatalogue().getSitesByUname(user);

                    if (sites == null || sites.isEmpty()) {
                        debug("\t StorageSites for " + this.getName() + " are empty!");
                        throw new RuntimeException("User " + user + " has StorageSites for " + this.getName());
                    }
                    getLogicalData().setStorageSites(sites);
                } catch (CatalogueException ex) {
                    throw new RuntimeException(ex.getMessage());
                }
            }
        }

        //return auth.getUser() == null ? false : true;
        return authorized;
    }

    @Override
    public String getRealm() {
        debug("getRealm.");
        return "realm";
    }

    @Override
    public Date getModifiedDate() {
        debug("getModifiedDate.");
        if (getLogicalData().getMetadata() != null && getLogicalData().getMetadata().getModifiedDate() != null) {
            return new Date(getLogicalData().getMetadata().getModifiedDate());
        }
        return null;
    }

    @Override
    public String checkRedirect(Request request) {
        debug("checkRedirect.");
        switch (request.getMethod()) {
            case GET:
                if (getLogicalData().isRedirectAllowed()) {
                    //Replica selection algorithm 
                    return null;
                }
                return null;

            default:
                return null;
        }
    }

    protected void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + "." + getLogicalData().getLDRI() + ": " + msg);
        }
//        log.debug(msg);
    }
//    @Override
//    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
//        try {
//            Collection<IStorageSite> sites = logicalData.getStorageSites();
//            if (sites != null && !sites.isEmpty()) {
//                for (IStorageSite s : sites) {
//                    s.deleteVNode(logicalData.getLDRI());
//                }
//            }
//            catalogue.unregisterResourceEntry(logicalData);
//        } catch (CatalogueException ex) {
//            throw new BadRequestException(this, ex.toString());
//        } catch (VlException ex) {
//            throw new BadRequestException(this, ex.toString());
//        }
//    }

    /**
     * @return the catalogue
     */
    public IDLCatalogue getCatalogue() {
        return catalogue;
    }

    /**
     * @return the logicalData
     */
    public ILogicalData getLogicalData() {
        return logicalData;
    }

    /**
     * @return the logicalData
     */
    public void setLogicalData(ILogicalData logicalData) {
        this.logicalData = logicalData;
    }

    public Path getPath() {
        return getLogicalData().getLDRI();
    }

    Collection<IStorageSite> getStorageSites() throws CatalogueException, IOException {
        Collection<IStorageSite> sites = getLogicalData().getStorageSites();
        if (sites == null || sites.isEmpty()) {
//            String uname = String.valueOf(principal.getUid());
            String uname = "uname1";
            sites = getCatalogue().getSitesByUname(uname);
        }
        if (sites == null || sites.isEmpty()) {
            debug("\t Storage Sites for " + this.getLogicalData().getLDRI() + " are empty!");
            throw new IOException("Storage Sites for " + this.getLogicalData().getLDRI() + " are empty!");
        }
        return sites;
    }
    
    public MyPrincipal getPrincipal(){
        HttpServletRequest r = WebDavServlet.request();
        MyPrincipal pr = (MyPrincipal) (r.getAttribute("vph-user"));
        return pr;
    }
}

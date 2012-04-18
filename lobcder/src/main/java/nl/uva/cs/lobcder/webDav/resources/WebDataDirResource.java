/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author S. Koulouzis
 */
class WebDataDirResource implements FolderResource, CollectionResource {

    private ILogicalData entry;
    private final IDLCatalogue catalogue;
    private boolean debug = false;

    public WebDataDirResource(IDLCatalogue catalogue, ILogicalData entry) throws IOException, Exception {
        this.entry = entry;
        if (!entry.getType().equals(Constants.LOGICAL_FOLDER)) {
            throw new Exception("The logical data has the wonrg type: " + entry.getType());
        }
        this.catalogue = catalogue;
        debug("Init. entry: " + entry.getLDRI());
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        try {
            debug("createCollection.");

            Path newCollectionPath = Path.path(entry.getLDRI(), newName);
            debug("\t newCollectionPath: " + newCollectionPath);
            LogicalData newFolderEntry = new LogicalData(newCollectionPath, Constants.LOGICAL_FOLDER);
            newFolderEntry.getMetadata().setCreateDate(System.currentTimeMillis());

            Collection<IStorageSite> sites = entry.getStorageSites();
            if (sites == null || sites.isEmpty()) {
                debug("\t Storage Sites for " + this.entry.getLDRI() + " are empty!");
                throw new IOException("Storage Sites for " + this.entry.getLDRI() + " are empty!");
            }

            //Maybe we have a problem with shalow copy
            //copyStorageSites.addAll(entry.getStorageSites());
            ArrayList<IStorageSite> copyStorageSites = new ArrayList<IStorageSite>();
            for (IStorageSite s : sites) {
                String ep = s.getEndpoint();
                if (ep == null) {
                    throw new NullPointerException("Endpoint is null");
                }
                Credential cred = s.getCredentials();
                if (cred == null) {
                    throw new NullPointerException("Credentials is null");
                }
                StorageSite ss = new StorageSite(ep, cred);
                copyStorageSites.add(ss);
            }

            newFolderEntry.setStorageSites(copyStorageSites);
//            sites = newFolderEntry.getStorageSites();
//            if (sites == null || sites.isEmpty()) {
//                debug("\t Storage Sites for " + newFolderEntry.getLDRI() + " are empty!");
//                throw new IOException("Storage Sites for " + newFolderEntry.getLDRI() + " are empty!");
//            }
            catalogue.registerResourceEntry(newFolderEntry);

            ILogicalData reloaded = catalogue.getResourceEntryByLDRI(newFolderEntry.getLDRI());
            sites = reloaded.getStorageSites();
            if (sites == null || sites.isEmpty()) {
                debug("\t Storage Sites for (reloaded)" + reloaded.getLDRI() + " are empty!");
                //Bad bad horrible patch!
                sites = entry.getStorageSites();
                copyStorageSites = new ArrayList<IStorageSite>();
                for (IStorageSite s : sites) {
                    copyStorageSites.add(new StorageSite(s.getEndpoint(), s.getCredentials()));
                }
                newFolderEntry.setStorageSites(copyStorageSites);
                catalogue.updateResourceEntry(newFolderEntry);
                reloaded = catalogue.getResourceEntryByLDRI(newFolderEntry.getLDRI());
//                throw new IOException("Storage Sites for " + reloaded.getLDRI() + " are empty!");
            }
            WebDataDirResource resource = new WebDataDirResource(catalogue, reloaded);

            //Why do we do that ?
//            reloaded = catalogue.getResourceEntryByLDRI(this.entry.getLDRI());
//            if(reloaded==null){
//                throw new BadRequestException(this, "Logical resource queried from catalogue is null");
//            }
//            this.entry = reloaded;

            return resource;
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            if (ex.getMessage().contains("resource exists")) {
                throw new ConflictException(this, newName);
            }
        }
        return null;
    }

    @Override
    public Resource child(String childName) {
        try {
            debug("child.");
            Path childPath = Path.path(entry.getLDRI(), childName);
            ILogicalData child = catalogue.getResourceEntryByLDRI(childPath);

            if (child != null) {
                return new WebDataDirResource(catalogue, child);
            }
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public List<? extends Resource> getChildren() {
        debug("getChildren.");
        ArrayList<? extends Resource> children = null;
        try {
            if (entry.getLDRI().isRoot()) {
                children = getTopLevelChildren();
            } else {
                children = getEntriesChildren();
            }
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        debug("Returning children: " + children);
        return children;
    }

    @Override
    public String getUniqueId() {
        debug("getUniqueId.");
        return entry.getUID();
    }

    @Override
    public String getName() {
        debug("getName.");
        return entry.getLDRI().getName();
    }

    @Override
    public Object authenticate(String user, String password) {
        debug("authenticate.\n"
                + "\t user: " + user
                + "\t password: " + password);
        return user;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
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
        return true;
    }

    @Override
    public String getRealm() {
        debug("getRealm.");
        return "relam";
    }

    @Override
    public Date getModifiedDate() {
        debug("getModifiedDate.");
        if (entry.getMetadata() != null && entry.getMetadata().getModifiedDate() != null) {
            return new Date(entry.getMetadata().getModifiedDate());
        }
        return null;
    }

    @Override
    public String checkRedirect(Request request) {
        debug("checkRedirect.");
        return null;
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        Resource resource;
        try {
            debug("createNew.");
            debug("\t newName: " + newName);
            debug("\t length: " + length);
            debug("\t contentType: " + contentType);
            Path newPath = Path.path(entry.getLDRI(), newName);

            LogicalData newResource = (LogicalData) catalogue.getResourceEntryByLDRI(newPath);
            if (newResource != null) {
                resource = updateExistingFile(newResource, length, contentType, inputStream);
            } else {
                resource = createNonExistingFile(newPath, length, contentType, inputStream);
            }

            ILogicalData reloaded = catalogue.getResourceEntryByLDRI(this.entry.getLDRI());
            this.entry = reloaded;
            return resource;
        } catch (Exception ex) {
            throw new BadRequestException(this, ex.getMessage());
        } finally {
        }
    }

    @Override
    public void copyTo(CollectionResource toCollection, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        try {
            debug("copyTo.");
            debug("\t toCollection: " + toCollection.getName());
            debug("\t name: " + name);
            Path toCollectionLDRI = Path.path(toCollection.getName());
            Path newLDRI = Path.path(toCollectionLDRI, name);
            LogicalData newFolderEntry = new LogicalData(newLDRI, Constants.LOGICAL_FOLDER);
            newFolderEntry.getMetadata().setModifiedDate(System.currentTimeMillis());
            catalogue.registerResourceEntry(newFolderEntry);

        } catch (Exception ex) {
            if (ex.getMessage().contains("resource exists")) {
                throw new ConflictException(this, ex.getMessage());
            }
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        try {
            debug("delete.");
            Collection<IStorageSite> sites = entry.getStorageSites();
            if (sites != null && !sites.isEmpty()) {
                for (IStorageSite s : sites) {
                    s.deleteVNode(entry.getLDRI());
                }
            }
            List<? extends Resource> children = getChildren();
            if (children != null) {
                for (Resource r : children) {
                    if (r instanceof DeletableResource) {
                        ((DeletableResource) r).delete();
                    }
                }
            }
            catalogue.unregisterResourceEntry(entry);
        } catch (CatalogueException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (VlException ex) {
            throw new BadRequestException(this, ex.toString());
        }
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException {
        //Not sure what it does
        debug("sendContent.");
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        debug("getMaxAgeSeconds.");
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        debug("getContentType. accepts: " + accepts);
        ArrayList<String> mimeTypes;
        if (accepts != null) {
            String[] acceptsTypes = accepts.split(",");
            if (entry.getMetadata() != null) {
                mimeTypes = entry.getMetadata().getContentTypes();
                for (String accessType : acceptsTypes) {
                    for (String mimeType : mimeTypes) {
                        if (accessType.equals(mimeType)) {
                            return mimeType;
                        }
                    }
                }
                return mimeTypes.get(0);
            }
        }
        return null;
    }

    @Override
    public Long getContentLength() {
        debug("getContentLength.");
        if (entry.getMetadata() != null) {
            return entry.getMetadata().getLength();
        }
        return null;
    }

    @Override
    public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        try {
            debug("moveTo.");
            debug("\t rDestgetName: " + rDest.getName() + " name: " + name);
//            if(rDest == null || rDest.getName() == null){
//                debug("----------------Will throw forbidden ");
//                throw new com.bradmcevoy.http.exceptions.BadRequestException(this);
//            }
            catalogue.renameEntry(entry.getLDRI(), Path.path(name));
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            if (ex.getMessage().contains("resource exists")) {
                throw new ConflictException(rDest, ex.getMessage());
            }
        }
    }

    @Override
    public Date getCreateDate() {
        debug("getCreateDate.");
        debug("\t entry.getMetadata(): " + entry.getMetadata());
        debug("\t entry.getMetadata().getCreateDate(): " + entry.getMetadata().getCreateDate());
        if (entry.getMetadata() != null && entry.getMetadata().getCreateDate() != null) {
            debug("getCreateDate. returning");
            return new Date(entry.getMetadata().getCreateDate());
        }
        debug("getCreateDate. returning");
        return null;
    }

    protected void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + "." + entry.getLDRI() + ": " + msg);
        }
    }

    private ArrayList<? extends Resource> getTopLevelChildren() throws Exception {
        Collection<ILogicalData> topEntries = catalogue.getTopLevelResourceEntries();
        ArrayList<Resource> children = new ArrayList<Resource>();
        for (ILogicalData e : topEntries) {
            if (e instanceof LogicalData) {
                children.add(new WebDataDirResource(catalogue, e));
            } else if (e instanceof LogicalData) {
                children.add(new WebDataFileResource(catalogue, e));
            } else {
                children.add(new WebDataResource(catalogue, e));
            }
        }
        return children;
    }

    private ArrayList<? extends Resource> getEntriesChildren() throws Exception {
        Collection<Path> childrenPaths = entry.getChildren();
//        if(childrenPaths == null){
//             entry = catalogue.getResourceEntryByLDRI(this.entry.getLDRI());
//        }
//        childrenPaths = entry.getChildren();
        ArrayList<Resource> children = new ArrayList<Resource>();
        if (childrenPaths != null) {
            for (Path p : childrenPaths) {
                debug("Adding children: " + p);
                ILogicalData ch = catalogue.getResourceEntryByLDRI(p);
                if(ch == null ){
                    throw new NullPointerException("The Collection "+entry.getLDRI()+" has "+p+" registered as a child but the catalogue has no such entry");
                }
                if (ch.getType().equals(Constants.LOGICAL_FOLDER)) {
                    children.add(new WebDataDirResource(catalogue, ch));
                } else if (ch.getType().equals(Constants.LOGICAL_FILE)) {
                    children.add(new WebDataFileResource(catalogue, ch));
                } else {
                    children.add(new WebDataResource(catalogue, ch));
                }
            }
        }
        return children;
    }

    Path getPath() {
        return this.entry.getLDRI();
    }

    Collection<IStorageSite> getStorageSites() {
        return this.entry.getStorageSites();
    }

    private Resource createNonExistingFile(Path newPath, Long length, String contentType, InputStream inputStream) throws IOException, Exception {
        LogicalData newResource = new LogicalData(newPath, Constants.LOGICAL_FILE);
        //We have to make a copy of the member collection. The same collection 
        //can't be a member of the two different classes, the relationship is 1-N!!!
        ArrayList<IStorageSite> copyStorageSites = new ArrayList<IStorageSite>();
        Collection<IStorageSite> sites = entry.getStorageSites();
//        if (sites == null || sites.isEmpty()) {
//            ILogicalData reloaded = this.catalogue.getResourceEntryByLDRI(entry.getLDRI());
//            sites = reloaded.getStorageSites();
//        }
        if (sites == null || sites.isEmpty()) {
            debug("\t Storage Sites for " + this.entry.getLDRI() + " are empty!");
            throw new IOException("Storage Sites for " + this.entry.getLDRI() + " are empty!");
        }
        //Maybe we have a problem with shalow copy
        //copyStorageSites.addAll(entry.getStorageSites());
        for (IStorageSite s : sites) {
            copyStorageSites.add(new StorageSite(s.getEndpoint(), s.getCredentials()));
        }
        newResource.setStorageSites(copyStorageSites);
        VFSNode node;
        if (!newResource.hasPhysicalData()) {
            node = newResource.createPhysicalData();
        } else {
            node = newResource.getVFSNode();
        }
        if (node != null) {
            OutputStream out = ((VFile) node).getOutputStream();
            IOUtils.copy(inputStream, out);
            if (inputStream != null) {
                inputStream.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }

        Metadata meta = new Metadata();
        meta.setLength(length);
        meta.addContentType(contentType);
        meta.setCreateDate(System.currentTimeMillis());
        newResource.setMetadata(meta);
        catalogue.registerResourceEntry(newResource);
        LogicalData relodedResource = (LogicalData) catalogue.getResourceEntryByLDRI(newResource.getLDRI());

        return new WebDataFileResource(catalogue, relodedResource);
    }

    private Resource updateExistingFile(LogicalData newResource, Long length, String contentType, InputStream inputStream) throws VlException, IOException, Exception {
        VFSNode node;

        if (!newResource.hasPhysicalData()) {
            node = newResource.createPhysicalData();
        } else {
            node = newResource.getVFSNode();
        }

        if (node != null) {
            OutputStream out = ((VFile) node).getOutputStream();
            IOUtils.copy(inputStream, out);
            if (inputStream != null) {
                inputStream.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }

        Metadata meta = newResource.getMetadata();
        meta.setLength(length);
        meta.addContentType(contentType);
        meta.setModifiedDate(System.currentTimeMillis());
        newResource.setMetadata(meta);

        catalogue.updateResourceEntry(newResource);
        LogicalData relodedResource = (LogicalData) catalogue.getResourceEntryByLDRI(newResource.getLDRI());
        return new WebDataFileResource(catalogue, relodedResource);
    }

    void setLogicalData(ILogicalData updatedLogicalData) {
        this.entry = updatedLogicalData;
    }
}

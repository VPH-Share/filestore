/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.io.Serializable;

import java.util.ArrayList;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class DataResourceEntry implements IDataResourceEntry, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -1529997963561059214L;
    @PrimaryKey
    private String uid;
    private Path ldri;
    private Metadata metadata;
    private ArrayList<IDataResourceEntry> children;
    private ArrayList<StorageResource> storageResource;

    public DataResourceEntry(Path ldri) {
        this.ldri = ldri;
        uid = java.util.UUID.randomUUID().toString();

    }

    @Override
    public Path getLDRI() {
        return this.ldri;
    }

    @Override
    public ArrayList<IDataResourceEntry> getChildren() {        
        return children;
    }

    @Override
    public ArrayList<StorageResource> getStorageResources() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Metadata getMetadata() {
        return new Metadata();
    }

    @Override
    public void setMetadata(Metadata metadata) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public String getUID() {
        return this.uid;
    }

    @Override
    public void addChildren(ArrayList<IDataResourceEntry> children) {
        if (this.children == null) {
            this.children = children;
        } else {
            this.children.addAll(children);
        }
    }

    @Override
    public void setStorageResource(ArrayList<StorageResource> storageResource) {
        this.storageResource = storageResource;
    }

    @Override
    public void addChild(IDataResourceEntry child) {
        if (this.children == null) {
            this.children = new ArrayList<IDataResourceEntry>();
        }
        this.children.add(child);
    }

    private void debug(String msg) {
        System.err.println(this.getClass().getSimpleName() +": "+msg);
    }
}

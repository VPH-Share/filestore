/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.util.Collection;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSNode;

/**
 *
 * @author S. Koulouzis
 */
public interface IStorageSite {

    String getEndpoint();

    String getVPHUsername();

    Collection<String> getLogicalPaths();
    
    boolean LDRIHasPhysicalData(Path ldri);
    
    VFSNode getVNode(Path path) throws VlException;
    
    
    VFSNode createVFSFile(Path path) throws VlException;

    public Credential getCredentials();

    public long getUID();

    public void deleteVNode(Path lDRI)throws VlException;
}

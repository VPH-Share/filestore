package nl.uva.cs.lobcder.catalogue.beans;

import lombok.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.math.BigInteger;

/**
 * User: dvasunin Date: 01.03.13 Time: 13:57 To change this template use File |
 * Settings | File Templates.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name="pdri")
@XmlAccessorType(XmlAccessType.FIELD)
public class PdriBean {
    private Long id;
    private String name;
    private BigInteger encryptionKey;
    private StorageSiteBean storage;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof PdriBean)) return false;
        PdriBean other = (PdriBean) o;
        if (other.id.equals(id)) return true;
        else return false;
    }
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

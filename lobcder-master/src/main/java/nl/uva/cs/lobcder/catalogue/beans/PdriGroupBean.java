package nl.uva.cs.lobcder.catalogue.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

/**
 * Created by dvasunin on 26.02.15.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name="pdri_group")
@XmlAccessorType(XmlAccessType.FIELD)
public class PdriGroupBean {
    private Long id;
    private Integer refCount;
    private Boolean needCheck;
    private Boolean bound;
    private Collection<PdriBean> pdri;
    private Collection<ItemBean> item;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof PdriGroupBean)) return false;
        PdriGroupBean other = (PdriGroupBean) o;
        if (other.getId().equals(getId())) return true;
        else return false;
    }
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

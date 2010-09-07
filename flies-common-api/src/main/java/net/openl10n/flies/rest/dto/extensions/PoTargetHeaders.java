package net.openl10n.flies.rest.dto.extensions;

import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import net.openl10n.flies.rest.dto.DTOUtil;
import net.openl10n.flies.rest.dto.resource.Extension;

import org.codehaus.jackson.annotate.JsonTypeName;

@XmlType(name = "poTargetHeadersExtension", namespace = PoTargetHeaders.NAMESPACE, propOrder = { "headers" })
@XmlRootElement(name = "po-target-header", namespace = PoTargetHeaders.NAMESPACE)
@JsonTypeName(value = PoTargetHeaders.ID)
public class PoTargetHeaders extends Extension
{

   public static final String ID = "gettext-po-headers";
   public static final String VERSION = "1.0";
   public static final String NAMESPACE = PoHeader.NAMESPACE;

   private Set<PoTargetHeaderEntry> headers;

   public PoTargetHeaders()
   {
      super(ID, VERSION);
   }

   @XmlElement(name = "header", namespace = PoHeader.NAMESPACE)
   public Set<PoTargetHeaderEntry> getHeaders()
   {
      return headers;
   }

}
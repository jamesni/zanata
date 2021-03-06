package org.zanata.webtrans.shared.model;

import java.io.Serializable;

public class DocumentInfo implements HasIdentifier<DocumentId>, Serializable
{
   private static final long serialVersionUID = 1L;
   private DocumentId id;
   private String name;
   private String path;

   // for GWT
   @SuppressWarnings("unused")
   private DocumentInfo()
   {
   }

   public DocumentInfo(DocumentId id, String name, String path)
   {
      this.id = id;
      this.name = name;
      this.path = path;
   }

   public DocumentId getId()
   {
      return id;
   }

   public String getName()
   {
      return name;
   }

   public String getPath()
   {
      return path;
   }

   @Override
   public String toString()
   {
      return "DocumentInfo(name=" + name + ",path=" + path + ",id=" + id + ")";
   }

}

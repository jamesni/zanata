package net.openl10n.packaging.jpa.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import net.openl10n.adapters.LocaleId;
import net.openl10n.packaging.document.Container;
import net.openl10n.packaging.document.Content;
import net.openl10n.packaging.document.DataHook;
import net.openl10n.packaging.document.Document;
import net.openl10n.packaging.document.Reference;
import net.openl10n.packaging.document.Resource;
import net.openl10n.packaging.document.TextFlow;
import net.openl10n.packaging.jpa.AbstractEntity;
import net.openl10n.packaging.jpa.LocaleIdType;
import net.openl10n.packaging.jpa.project.HProject;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;
import org.hibernate.validator.Length;
import org.hibernate.validator.NotEmpty;
import org.hibernate.validator.NotNull;

@Entity
@TypeDef(name="localeId", typeClass=LocaleIdType.class)
public class HDocument extends AbstractEntity{

	private String docId;
	private String name;
	private String path;
	private String contentType;
	private Integer revision = 1;
	private LocaleId locale;
	
	private HProject project;
	private Integer pos;

	private Map<String, HResource> resources;
	private List<HResource> resourceTree;
	
	private List<HDocumentTarget> targets = new ArrayList<HDocumentTarget>();

	public HDocument(String fullPath, String contentType) {
		this(fullPath, contentType, LocaleId.EN_US);
	}
	
	public HDocument(String fullPath, String contentType, LocaleId locale) {
		int lastSepChar =  fullPath.lastIndexOf('/');
		switch(lastSepChar){
		case -1:
			this.path = "";
			this.docId = this.name = fullPath;
			break;
		case 0:
			this.path = "/";
			this.docId = fullPath;
			this.name = fullPath.substring(1);
			break;
		default:
			this.path = fullPath.substring(0,lastSepChar+1);
			this.docId = fullPath;
			this.name = fullPath.substring(lastSepChar+1);
		}
		this.contentType = contentType;
		this.locale = locale;
	}
	
	public HDocument(String docId, String name, String path, String contentType) {
		this(docId, name, path, contentType, LocaleId.EN_US, 1);
	}
	
	public HDocument(String docId, String name, String path, String contentType, LocaleId locale) {
		this(docId, name, path, contentType, locale, 1);
	}
	
	public HDocument(String docId, String name, String path, String contentType, LocaleId locale, int revision) {
		this.docId = docId;
		this.name = name;
		this.path = path;
		this.contentType = contentType;
		this.locale = locale;
	}
	
	public HDocument() {
	}
	
	public HDocument(Document docInfo) {
		this.docId = docInfo.getId();
		this.name = docInfo.getName();
		this.path = docInfo.getPath();
		this.contentType = docInfo.getContentType();
		this.locale = new LocaleId(docInfo.getLang());
		this.revision = docInfo.getVersion();
	}

	public static HResource create(Resource res){
		if(res instanceof TextFlow){
			TextFlow tf = (TextFlow) res;
			return new HTextFlow( tf );
		}
		else if (res instanceof Container){
			Container cont = (Container) res;
			return new HContainer(cont);
		}
		else if (res instanceof DataHook){
			DataHook hook = (DataHook) res;
			return new HDataHook(hook);
		}
		else if (res instanceof Reference){
			Reference ref = (Reference) res;
			return new HReference(ref);
		}
		throw new IllegalStateException("could not find subclass of Resource: " + res.getClass().toString());
	}
	public void copy(Content content){
		for(Resource res :content.getResources()){
			HResource hRes = create(res);
			getResourceTree().add(hRes);
		}
	}
	
	@NaturalId
	@Length(max=255)
	@NotEmpty
	public String getDocId() {
		return docId;
	}
	
	public void setDocId(String docId) {
		this.docId = docId;
	}
	
	@NotEmpty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@NotNull
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	@NotNull
	@Type(type="localeId")
	public LocaleId getLocale() {
		return locale;
	}
	
	public void setLocale(LocaleId locale) {
		this.locale = locale;
	}
	
	@ManyToOne
	@JoinColumn(name="project_id",insertable=false, updatable=false, nullable=false)
	@NaturalId
	public HProject getProject() {
		return project;
	}
	
	public void setProject(HProject project) {
		this.project = project;
	}

	@Column(insertable=false, updatable=false, nullable=false)
	public Integer getPos() {
		return pos;
	}
	
	public void setPos(Integer pos) {
		this.pos = pos;
	}
	@NotNull
	public Integer getRevision() {
		return revision;
	}

	public void setRevision(Integer revision) {
		this.revision = revision;
	}

	@Transient
	public void incrementRevision() {
		revision++;
	}

	@Length(max = 255)
	@NotEmpty
	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@OneToMany(mappedBy="document")
	@MapKey(name="resId")
	public Map<String,HResource> getResources() {
		if(resources == null)
			resources = new HashMap<String, HResource>();
		return resources;
	}
	
	public void setResources(Map<String, HResource> resources) {
		this.resources = resources;
	}
	
	@OneToMany(mappedBy = "template")
	@OnDelete(action=OnDeleteAction.CASCADE)
	public List<HDocumentTarget> getTargets() {
		return targets;
	}

	public void setTargets(List<HDocumentTarget> targets) {
		this.targets = targets;
	}

	@OneToMany(cascade=CascadeType.ALL)
	@Where(clause="parent_id=NULL")
	@IndexColumn(name="pos",base=0,nullable=false)
	@JoinColumn(name="document_id",nullable=false)
	public List<HResource> getResourceTree() {
		if(resourceTree == null)
			resourceTree = new ArrayList<HResource>();
		return resourceTree;
	}
	
	public void setResourceTree(List<HResource> resourceTree) {
		this.resourceTree = resourceTree;
	}
}
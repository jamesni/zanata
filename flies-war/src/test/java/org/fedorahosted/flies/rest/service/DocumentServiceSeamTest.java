package org.fedorahosted.flies.rest.service;

import java.net.URI;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.dbunit.operation.DatabaseOperation;
import org.fedorahosted.flies.ContentType;
import org.fedorahosted.flies.LocaleId;
import org.fedorahosted.flies.rest.ApiKeyHeaderDecorator;
import org.fedorahosted.flies.rest.MediaTypes;
import org.fedorahosted.flies.rest.client.ContentQualifier;
import org.fedorahosted.flies.rest.client.IDocumentResource;
import org.fedorahosted.flies.rest.dto.Document;
import org.fedorahosted.flies.rest.dto.Link;
import org.fedorahosted.flies.rest.dto.Relationships;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.seam.mock.DBUnitSeamTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;


@Test(groups={"seam-tests"},suiteName="DocumentService")
public class DocumentServiceSeamTest extends DBUnitSeamTest{
	
	ClientRequestFactory clientRequestFactory;
	URI baseUri = URI.create("/restv1/");
	IDocumentResource documentResource;
	
	private static final String url = "projects/p/sample-project/iterations/i/1.0/documents/d/my,path,document.txt";
	
	@BeforeClass
	public void prepareRestEasyClientFramework() throws Exception {

		ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
		RegisterBuiltin.register(instance);

		clientRequestFactory = 
			new ClientRequestFactory(
					new SeamMockClientExecutor(this), baseUri);
		
		clientRequestFactory.getPrefixInterceptors().registerInterceptor(new ApiKeyHeaderDecorator("admin", "12345678901234567890123456789012"));

		documentResource = clientRequestFactory.createProxy(IDocumentResource.class, baseUri.resolve(url));
		
	}
	
	@Override
    protected void prepareDBUnitOperations() {
        beforeTestOperations.add(
        		new DataSetOperation("META-INF/testdata/AccountData.dbunit.xml", DatabaseOperation.CLEAN_INSERT)
        );
        beforeTestOperations.add(
        		new DataSetOperation("META-INF/testdata/ProjectsData.dbunit.xml", DatabaseOperation.CLEAN_INSERT)
        );
        beforeTestOperations.add(
                new DataSetOperation("org/fedorahosted/flies/rest/service/DocumentTestData.dbunit.xml", DatabaseOperation.CLEAN_INSERT)
        );
        
    }


	public void getDocumentThatDoesntExist(){
		assertThat ( documentResource.get(null).getResponseStatus(), is(Status.NOT_FOUND) ); 
	}

	public void getDocument() throws URIException {
		ClientResponse<Document> response = documentResource.get(null);
		assertThat( response.getResponseStatus(), is(Status.OK) ) ;
		Document doc = response.getEntity();
		assertThat( doc.getId(), is("/my/path/document.txt") );
		assertThat( doc.getName(), is("document.txt") );
		assertThat( doc.getContentType(), is(ContentType.TextPlain) );
		assertThat( doc.getLang(), is(LocaleId.EN_US) );
		assertThat( doc.getVersion(), is(1) );
		assertThat( doc.getResources().isEmpty(), is(true) );

		Link link = doc.findLinkByRel(Relationships.SELF);
		assertThat( link, notNullValue() );
		assertThat( URIUtil.decode(link.getHref().toString()), endsWith(url) );
		
		link = doc.findLinkByRel(Relationships.DOCUMENT_CONTAINER);
		assertThat( link, notNullValue() );
		assertThat( link.getHref().toString(), endsWith("iterations/i/1.0") );
	}
	
	public void getDocumentWithResources() throws URIException {
		ClientResponse<Document> response = documentResource.get( ContentQualifier.ALL );
		assertThat( response.getResponseStatus(), is(Status.OK) ) ;
		Document doc = response.getEntity();
		assertThat( doc.getResources().size(), is(1) );

		response = documentResource.get( ContentQualifier.SOURCE );
		assertThat( response.getResponseStatus(), is(Status.OK) ) ;
		doc = response.getEntity();
		assertThat( doc.getResources().size(), is(1) );

		response = documentResource.get( ContentQualifier.fromLocales(new LocaleId("nb-NO")));
		assertThat( response.getResponseStatus(), is(Status.OK) ) ;
		doc = response.getEntity();
		assertThat( doc.getResources().size(), is(1) );

	}
	
	public void putNewDocument() {
		Document doc = new Document("my/fancy/document.txt", ContentType.TextPlain);
		Response response = documentResource.put(doc);

		assertThat( response.getStatus(), is(Status.CREATED.getStatusCode()) );
		
		ClientResponse<Document> documentResponse = documentResource.get(null);
		
		assertThat( documentResponse.getResponseStatus(), is(Status.OK) );
		
		doc = documentResponse.getEntity(); 
		assertThat( doc.getVersion(), is(1) );
		Link link = doc.findLinkByRel(Relationships.SELF); 
		assertThat( link, notNullValue() );
		assertThat( link.getHref().toString(), endsWith(url) );
		
		link = doc.findLinkByRel(Relationships.DOCUMENT_CONTAINER); 
		assertThat( link, notNullValue() );
		assertThat( link.getType(), is( MediaTypes.APPLICATION_FLIES_PROJECT_ITERATION_XML) );
	}
	
}
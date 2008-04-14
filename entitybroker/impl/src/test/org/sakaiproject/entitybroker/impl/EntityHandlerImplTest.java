/**
 * $Id$
 * $URL$
 * EntityHandlerImplTest.java - entity-broker - Apr 6, 2008 12:08:39 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Centre for Applied Research in Educational Technologies, University of Cambridge
 * Licensed under the Educational Community License version 1.0
 * 
 * A copy of the Educational Community License has been included in this 
 * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
 *
 * Aaron Zeckoski (azeckoski@gmail.com) (aaronz@vt.edu) (aaron@caret.cam.ac.uk)
 */

package org.sakaiproject.entitybroker.impl;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.search.Order;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.impl.EntityHandlerImpl;
import org.sakaiproject.entitybroker.impl.entityprovider.EntityProviderManagerImpl;
import org.sakaiproject.entitybroker.impl.mocks.FakeServerConfigurationService;
import org.sakaiproject.entitybroker.mocks.EntityViewAccessProviderManagerMock;
import org.sakaiproject.entitybroker.mocks.HttpServletAccessProviderManagerMock;
import org.sakaiproject.entitybroker.mocks.MockHttpServletRequest;
import org.sakaiproject.entitybroker.mocks.data.MyEntity;
import org.sakaiproject.entitybroker.mocks.data.TestData;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Testing the central logic of the entity handler
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class EntityHandlerImplTest extends TestCase {

   protected EntityHandlerImpl entityHandler;
   private TestData td;

   @Override
   protected void setUp() throws Exception {
      super.setUp();
      // setup things
      td = new TestData();

      EntityProviderManagerImpl epm = new EntityProviderManagerImplTest().makeEntityProviderManager(td);

      entityHandler = new EntityHandlerImpl();
      entityHandler.setEntityProviderManager( epm );
      entityHandler.setEntityViewAccessProviderManager( new EntityViewAccessProviderManagerMock() );
      entityHandler.setAccessProviderManager( new HttpServletAccessProviderManagerMock() );
      entityHandler.setRequestGetter( epm.getRequestGetter() );
      entityHandler.setServerConfigurationService( new FakeServerConfigurationService() );
   }

   /**
    * Test method for {@link org.sakaiproject.entitybroker.impl.EntityHandlerImpl#entityExists(java.lang.String)}.
    */
   public void testEntityExists() {
      EntityReference ref = null;
      boolean exists = false;

      ref = new EntityReference(TestData.REF1);
      exists = entityHandler.entityExists(ref);
      assertTrue(exists);

      ref = new EntityReference(TestData.REF1_1);
      exists = entityHandler.entityExists(ref);
      assertTrue(exists);

      ref = new EntityReference(TestData.REF2);
      exists = entityHandler.entityExists(ref);
      assertTrue(exists);

      // test that invalid id with valid prefix does not pass
      ref = new EntityReference(TestData.REF1_INVALID);
      exists = entityHandler.entityExists(ref);
      assertFalse(exists);

      // test that unregistered ref does not pass
      ref = new EntityReference(TestData.REF9);
      exists = entityHandler.entityExists(ref);
      assertFalse(exists);
   }

   /**
    * Test method for {@link org.sakaiproject.entitybroker.impl.EntityHandlerImpl#getEntityURL(java.lang.String)}.
    */
   public void testGetEntityURL() {
      String url = null;

      url = entityHandler.getEntityURL(TestData.REF1, null, null);
      assertEquals(TestData.URL1, url);

      url = entityHandler.getEntityURL(TestData.REF2, null, null);
      assertEquals(TestData.URL2, url);

      url = entityHandler.getEntityURL(TestData.REF1_INVALID, null, null);

      try {
         url = entityHandler.getEntityURL(TestData.INVALID_REF, null, null);
         fail("Should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e.getMessage());
      }
   }

   /**
    * Test method for {@link org.sakaiproject.entitybroker.impl.EntityHandlerImpl#parseReference(java.lang.String)}.
    */
   public void testParseReference() {
      EntityReference er = null;

      er = entityHandler.parseReference(TestData.REF1);
      assertNotNull(er);
      assertEquals(TestData.PREFIX1, er.getPrefix());
      assertEquals(TestData.IDS1[0], er.getId());

      er = entityHandler.parseReference(TestData.REF2);
      assertNotNull(er);
      assertEquals(TestData.PREFIX2, er.getPrefix());

      // test parsing a defined reference
      er = entityHandler.parseReference(TestData.REF3A);
      assertNotNull(er);
      assertEquals(TestData.PREFIX3, er.getPrefix());

      // parsing of unregistered entity references returns null
      er = entityHandler.parseReference(TestData.REF9);
      assertNull(er);

      // parsing with nonexistent prefix returns null
      er = entityHandler.parseReference("/totallyfake/notreal");
      assertNull(er);

      // TODO test handling custom ref objects

      try {
         er = entityHandler.parseReference(TestData.INVALID_REF);
         fail("Should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e.getMessage());
      }
   }

   /**
    * Test method for {@link EntityHandlerImpl#parseEntityURL(String)}
    */
   public void testParseEntityURL() {
      EntityView view = null;

      view = entityHandler.parseEntityURL(TestData.INPUT_URL1);
      assertNotNull(view);
      assertEquals(EntityView.VIEW_SHOW, view.getViewKey());
      assertEquals(TestData.PREFIX1, view.getEntityReference().getPrefix());
      assertEquals(TestData.IDS1[0], view.getEntityReference().getId());

      // TODO add more tests

      // parsing of URL related to unregistered entity references returns null
      view = entityHandler.parseEntityURL(TestData.REF9);
      assertNull(view);

      // TODO test custom parse rules

      try {
         view = entityHandler.parseEntityURL(TestData.INVALID_URL);
         fail("Should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e.getMessage());
      }
   }


   @SuppressWarnings("unchecked")
   public void testGetEntityObject() {
      Object entity = null;
      EntityReference ref = null;

      // first for resolveable
      ref = entityHandler.parseReference(TestData.REF4);
      assertNotNull(ref);
      entity = entityHandler.getEntityObject(ref);
      assertNotNull(entity);
      assertEquals(MyEntity.class, entity.getClass());
      assertEquals(TestData.entity4, entity);

      ref = entityHandler.parseReference(TestData.REF4_two);
      assertNotNull(ref);
      entity = entityHandler.getEntityObject(ref);
      assertNotNull(entity);
      assertEquals(MyEntity.class, entity.getClass());
      assertEquals(TestData.entity4_two, entity);

      // now for non-resolveable
      ref = entityHandler.parseReference(TestData.REF5);
      assertNotNull(ref);
      entity = entityHandler.getEntityObject(ref);
      assertNull(entity);
   }

   /**
    * Test method for {@link org.sakaiproject.entitybroker.impl.EntityHandlerImpl#makeSearchFromRequest(javax.servlet.http.HttpServletRequest)}.
    */
   public void testMakeSearchFromRequest() {
      Search search = null;
      MockHttpServletRequest req = null;

      req = new MockHttpServletRequest("GET", new String[] {});
      search = entityHandler.makeSearchFromRequest(req);
      assertNotNull(search);
      assertTrue( search.isEmpty() );
      assertEquals(0, search.restrictions.length);
      search.addOrder( new Order("test") );

      req = new MockHttpServletRequest("GET", "test", "stuff");
      search = entityHandler.makeSearchFromRequest(req);
      assertNotNull(search);
      assertFalse( search.isEmpty() );
      assertEquals(1, search.restrictions.length);
      assertNotNull( search.getRestrictionByProperty("test") );
      assertEquals("stuff", search.getRestrictionByProperty("test").value);

      // make sure _method is ignored
      req = new MockHttpServletRequest("GET", "test", "stuff", "_method", "PUT");
      search = entityHandler.makeSearchFromRequest(req);
      assertNotNull(search);
      assertFalse( search.isEmpty() );
      assertEquals(1, search.restrictions.length);
      assertNotNull( search.getRestrictionByProperty("test") );
      assertEquals("stuff", search.getRestrictionByProperty("test").value);

      req = new MockHttpServletRequest("GET", "test", "stuff", "other", "more");
      search = entityHandler.makeSearchFromRequest(req);
      assertNotNull(search);
      assertFalse( search.isEmpty() );
      assertEquals(2, search.restrictions.length);
      assertNotNull( search.getRestrictionByProperty("test") );
      assertEquals("stuff", search.getRestrictionByProperty("test").value);
      assertNotNull( search.getRestrictionByProperty("other") );
      assertEquals("more", search.getRestrictionByProperty("other").value);
   }


   /**
    * Test method for {@link EntityHandlerImpl#internalOutputFormatter(EntityView, javax.servlet.http.HttpServletRequest, HttpServletResponse)}
    **/
   public void testInternalOutputFormatter() {

      EntityView view = null;
      MockHttpServletRequest req = null;
      MockHttpServletResponse res = null;

      // JSON test valid resolveable entity
      req = new MockHttpServletRequest("GET", TestData.REF4 + "." + Outputable.JSON);
      res = new MockHttpServletResponse();
      view = entityHandler.parseEntityURL(req.getPathInfo());
      assertNotNull(view);
      entityHandler.internalOutputFormatter(view, req, res);
      assertNotNull(res.getOutputStream());
      try {
         String json = res.getContentAsString();
         assertNotNull(json);
         assertTrue(json.length() > 20);
         assertTrue(json.contains(TestData.PREFIX4));
      } catch (UnsupportedEncodingException e) {
         fail("failure trying to get string content");
      }
      assertEquals(58, res.getContentLength());

      // XML test valid resolveable entity
      req = new MockHttpServletRequest("GET", TestData.REF4 + "." + Outputable.XML);
      res = new MockHttpServletResponse();
      view = entityHandler.parseEntityURL(req.getPathInfo());
      assertNotNull(view);
      entityHandler.internalOutputFormatter(view, req, res);
      assertNotNull(res.getOutputStream());
      try {
         String xml = res.getContentAsString();
         assertNotNull(xml);
         assertTrue(xml.length() > 20);
         assertTrue(xml.contains(TestData.PREFIX4));
      } catch (UnsupportedEncodingException e) {
         fail("failure trying to get string content");
      }
      assertEquals(68, res.getContentLength());

      // HTML test valid resolveable entity
      req = new MockHttpServletRequest("GET", TestData.REF4 + "." + Outputable.HTML);
      res = new MockHttpServletResponse();
      view = entityHandler.parseEntityURL(req.getPathInfo());
      assertNotNull(view);
      entityHandler.internalOutputFormatter(view, req, res);
      assertNotNull(res.getOutputStream());
      try {
         String html = res.getContentAsString();
         assertNotNull(html);
         assertTrue(html.length() > 20);
         assertTrue(html.contains(TestData.PREFIX4));
      } catch (UnsupportedEncodingException e) {
         fail("failure trying to get string content");
      }
      assertEquals(43, res.getContentLength());

      // test for unresolvable entities

      // JSON test valid unresolvable entity
      req = new MockHttpServletRequest("GET", TestData.REF1 + "." + Outputable.JSON);
      res = new MockHttpServletResponse();
      view = entityHandler.parseEntityURL(req.getPathInfo());
      assertNotNull(view);
      entityHandler.internalOutputFormatter(view, req, res);
      assertNotNull(res.getOutputStream());
      try {
         String json = res.getContentAsString();
         assertNotNull(json);
         assertTrue(json.length() > 20);
         assertTrue(json.contains(TestData.PREFIX1));
      } catch (UnsupportedEncodingException e) {
         fail("failure trying to get string content");
      }
      assertEquals(167, res.getContentLength());

      // XML test valid unresolvable entity
      req = new MockHttpServletRequest("GET", TestData.REF1 + "." + Outputable.XML);
      res = new MockHttpServletResponse();
      view = entityHandler.parseEntityURL(req.getPathInfo());
      assertNotNull(view);
      entityHandler.internalOutputFormatter(view, req, res);
      assertNotNull(res.getOutputStream());
      try {
         String xml = res.getContentAsString();
         assertNotNull(xml);
         assertTrue(xml.length() > 20);
         assertTrue(xml.contains(TestData.PREFIX1));
      } catch (UnsupportedEncodingException e) {
         fail("failure trying to get string content");
      }
      assertEquals(195, res.getContentLength());

      // HTML test valid unresolvable entity
      req = new MockHttpServletRequest("GET", TestData.REF1); // blank should default to html
      res = new MockHttpServletResponse();
      view = entityHandler.parseEntityURL(req.getPathInfo());
      assertNotNull(view);
      entityHandler.internalOutputFormatter(view, req, res);
      assertNotNull(res.getOutputStream());
      try {
         String html = res.getContentAsString();
         assertNotNull(html);
         assertTrue(html.length() > 20);
         assertTrue(html.contains(TestData.PREFIX1));
      } catch (UnsupportedEncodingException e) {
         fail("failure trying to get string content");
      }
      assertEquals(100, res.getContentLength());

      // test resolveable collections
      // XML
      req = new MockHttpServletRequest("GET", TestData.SPACE4);
      res = new MockHttpServletResponse();
      view = entityHandler.parseEntityURL(req.getPathInfo());
      assertNotNull(view);
      entityHandler.internalOutputFormatter(view, req, res);
      assertNotNull(res.getOutputStream());
      try {
         String xml = res.getContentAsString();
         assertNotNull(xml);
         assertTrue(xml.length() > 20);
         assertTrue(xml.contains(TestData.PREFIX4));
         assertTrue(xml.contains(TestData.IDS4[0]));
         assertTrue(xml.contains(TestData.IDS4[1]));
         assertTrue(xml.contains(TestData.IDS4[2]));
      } catch (UnsupportedEncodingException e) {
         fail("failure trying to get string content");
      }

      // JSON
      req = new MockHttpServletRequest("GET", TestData.SPACE4);
      res = new MockHttpServletResponse();
      view = entityHandler.parseEntityURL(req.getPathInfo());
      assertNotNull(view);
      entityHandler.internalOutputFormatter(view, req, res);
      assertNotNull(res.getOutputStream());
      try {
         String json = res.getContentAsString();
         assertNotNull(json);
         assertTrue(json.length() > 20);
         //assertTrue(json.contains(TestData.PREFIX4));
         assertTrue(json.contains(TestData.IDS4[0]));
         assertTrue(json.contains(TestData.IDS4[1]));
         assertTrue(json.contains(TestData.IDS4[2]));
      } catch (UnsupportedEncodingException e) {
         fail("failure trying to get string content");
      }

      // test for invalid refs
      req = new MockHttpServletRequest("GET", "/fakey/fake");
      res = new MockHttpServletResponse();
      view = new EntityView();
      assertNotNull(view);
      try {
         entityHandler.internalOutputFormatter(view, req, res);
         fail("Should have thrown exception");
      } catch (RuntimeException e) {
         assertNotNull(e);
      }
      assertNotNull(res.getOutputStream());

   }

   /**
    * Test method for {@link org.sakaiproject.entitybroker.impl.EntityHandlerImpl#handleEntityAccess(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String)}.
    */
   public void testHandleEntityAccess() {
      MockHttpServletRequest req = null;
      MockHttpServletResponse res = null;

      // test valid entity
      req = new MockHttpServletRequest("GET", TestData.REF1);
      res = new MockHttpServletResponse();

      entityHandler.handleEntityAccess(req, res, null);
      assertEquals(HttpServletResponse.SC_OK, res.getStatus());

      // test invalid prefix
      req = new MockHttpServletRequest("GET", "/fake/thing");
      res = new MockHttpServletResponse();

      try {
         entityHandler.handleEntityAccess(req, res, null);
         fail("Should have thrown exception");
      } catch (EntityException e) {
         assertNotNull(e.getMessage());
         assertEquals(HttpServletResponse.SC_NOT_IMPLEMENTED, e.responseCode);
      }

      // test invalid id
      req = new MockHttpServletRequest("GET", TestData.REF1_INVALID);
      res = new MockHttpServletResponse();

      try {
         entityHandler.handleEntityAccess(req, res, null);
         fail("Should have thrown exception");
      } catch (EntityException e) {
         assertNotNull(e.getMessage());
         assertEquals(TestData.REF1_INVALID, e.entityReference);
         assertEquals(HttpServletResponse.SC_NOT_FOUND, e.responseCode);
      }

      // test invalid path format
      req = new MockHttpServletRequest("GET", "xxxxxxxxxxxxxxxx");
      res = new MockHttpServletResponse();

      try {
         entityHandler.handleEntityAccess(req, res, null);
         fail("Should have thrown exception");
      } catch (EntityException e) {
         assertNotNull(e.getMessage());
         assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.responseCode);
      }

      // TODO test JSON data return

      // test XML data return

      // types that cannot handle the return requested

      // test the REST and CRUD methods
   }

}

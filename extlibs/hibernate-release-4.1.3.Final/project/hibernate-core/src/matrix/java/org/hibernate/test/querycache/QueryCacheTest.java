/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.querycache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.transform.Transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class QueryCacheTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "querycache/Item.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "foo" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5426" )
	public void testInvalidationFromBulkHQL() {
		sessionFactory().getCache().evictQueryRegions();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		List list = new ArrayList();
		s.beginTransaction();
		for (int i = 0; i < 3; i++) {
			Item a = new Item();
			a.setName("a" + i);
			a.setDescription("a" + i);
			list.add(a);
			s.persist(a);
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		String queryString = "select count(*) from Item";
		// this query will hit the database and create the cache
		Long result = (Long) s.createQuery(queryString).setCacheable(true).uniqueResult();
		assertEquals(3, result.intValue());
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		String updateString = "delete from Item";
		s.createQuery(updateString).executeUpdate();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		// and this one SHOULD not be served by the cache
		Number result2 = (Number) s.createQuery(queryString).setCacheable(true).uniqueResult();
		assertEquals(0, result2.intValue());
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "JBPAPP-4224" )
	public void testHitCacheInSameSession() {
		sessionFactory().evictQueries();
		sessionFactory().getStatistics().clear();
		Session s = openSession();
		List list = new ArrayList();
		s.beginTransaction();
		for ( int i = 0; i < 3; i++ ) {
			Item a = new Item();
			a.setName( "a" + i );
			a.setDescription( "a" + i );
			list.add( a );
			s.persist( a );
		}
		s.getTransaction().commit();

//		s.close();
//		s=openSession();

		s.beginTransaction();
		String queryString = "from Item";
		// this query will hit the database and create the cache
		s.createQuery( queryString ).setCacheable( true ).list();
		s.getTransaction().commit();

		s.beginTransaction();
		//and this one SHOULD served by the cache
		s.createQuery( queryString ).setCacheable( true ).list();
		s.getTransaction().commit();
		QueryStatistics qs = s.getSessionFactory().getStatistics().getQueryStatistics( queryString );
		assertEquals( 1, qs.getCacheHitCount() );
		assertEquals( 1, qs.getCachePutCount() );
		s.close();
		s = openSession();
		s.beginTransaction();
		for(Object obj:list){
			s.delete( obj );
		}
		s.getTransaction().commit();
		s.close();

	}

	private static final String queryString = "from Item i where i.name='widget'";

	@Test
	public void testQueryCacheInvalidation() throws Exception {

		sessionFactory().evictQueries();
		sessionFactory().getStatistics().clear();

		final String queryString = "from Item i where i.name='widget'";

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery( queryString ).setCacheable(true).list();
		Item i = new Item();
		i.setName("widget");
		i.setDescription("A really top-quality, full-featured widget.");
		s.save(i);
		t.commit();
		s.close();

		QueryStatistics qs = s.getSessionFactory().getStatistics().getQueryStatistics( queryString );
		EntityStatistics es = s.getSessionFactory().getStatistics().getEntityStatistics( Item.class.getName() );

		Thread.sleep(200);

		s = openSession();
		t = s.beginTransaction();
		List result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 0 );

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 1 );
		assertEquals( s.getSessionFactory().getStatistics().getEntityFetchCount(), 0 );

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		assertTrue( Hibernate.isInitialized( result.get(0) ) );
		i = (Item) result.get(0);
		i.setName("Widget");
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 2 );
		assertEquals( qs.getCacheMissCount(), 2 );
		assertEquals( s.getSessionFactory().getStatistics().getEntityFetchCount(), 0 );

		Thread.sleep(200);

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		i = (Item) s.get( Item.class, new Long(i.getId()) );

		s.delete(i);
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 2 );
		assertEquals( qs.getCacheMissCount(), 3 );
		assertEquals( qs.getCachePutCount(), 3 );
		assertEquals( qs.getExecutionCount(), 3 );
		assertEquals( es.getFetchCount(), 0 ); //check that it was being cached

	}

	@Test
	@RequiresDialectFeature(
			value = DialectChecks.CaseSensitiveCheck.class,
			comment = "i.name='widget' should not match on case sensitive database."
	)
	public void testCaseInsensitiveComparison() {
		Session s = openSession();
		s.beginTransaction();
		Item i = new Item();
		i.setName( "Widget" );
		i.setDescription( "A really top-quality, full-featured widget." );
		s.save( i );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List result = s.createQuery( queryString ).list();
		assertEquals(1, result.size());
		i = (Item) s.get( Item.class, new Long(i.getId()) );
		assertEquals( i.getName(), "Widget" );
		s.delete(i);
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testQueryCacheFetch() throws Exception {
		sessionFactory().evictQueries();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		Item i = new Item();
		i.setName("widget");
		i.setDescription("A really top-quality, full-featured widget.");
		Item i2 = new Item();
		i2.setName("other widget");
		i2.setDescription("Another decent widget.");
		s.persist(i);
		s.persist(i2);
		t.commit();
		s.close();

		final String queryString = "from Item i where i.name like '%widget'";

		QueryStatistics qs = s.getSessionFactory().getStatistics().getQueryStatistics( queryString );

		Thread.sleep(200);

		s = openSession();
		t = s.beginTransaction();
		List result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 2 );
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 0 );
		assertEquals( s.getSessionFactory().getStatistics().getEntityFetchCount(), 0 );

		sessionFactory().evict(Item.class);

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 2 );
		assertTrue( Hibernate.isInitialized( result.get(0) ) );
		assertTrue( Hibernate.isInitialized( result.get(1) ) );
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 1 );
		assertEquals( s.getSessionFactory().getStatistics().getEntityFetchCount(), 1 );

		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete Item").executeUpdate();
		t.commit();
		s.close();

	}

	@Test
	public void testProjectionCache() throws Exception {
		sessionFactory().evictQueries();
        sessionFactory().getStatistics().clear();

		final String queryString = "select i.description as desc from Item i where i.name='widget'";

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery( queryString ).setCacheable(true).list();
		Item i = new Item();
		i.setName("widget");
		i.setDescription("A really top-quality, full-featured widget.");
		s.save(i);
		t.commit();
		s.close();

        QueryStatistics qs = s.getSessionFactory().getStatistics().getQueryStatistics( queryString );
		EntityStatistics es = s.getSessionFactory().getStatistics().getEntityStatistics( Item.class.getName() );

		assertEquals( qs.getCacheHitCount(), 0 );
		assertEquals( qs.getCacheMissCount(), 1 );
		assertEquals( qs.getCachePutCount(), 1 );

		Thread.sleep(200);

		s = openSession();
		t = s.beginTransaction();
		List result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		assertEquals( i.getDescription(), ( result.get( 0 ) ) );
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 0 );
		assertEquals( qs.getCacheMissCount(), 2 );
		assertEquals( qs.getCachePutCount(), 2 );

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		assertEquals( i.getDescription(), result.get( 0 ) );
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 1 );
		assertEquals( qs.getCacheMissCount(), 2 );
		assertEquals( qs.getCachePutCount(), 2 );

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();
		assertEquals( result.size(), 1 );
		Map m = (Map) result.get(0);
		assertEquals( 1, m.size() );
		assertEquals( i.getDescription(), m.get( "desc" ) );
		t.commit();
		s.close();

		assertEquals( "hit count should go up since data is not transformed until after it is cached", qs.getCacheHitCount(), 2 );
		assertEquals( qs.getCacheMissCount(), 2 );
		assertEquals( qs.getCachePutCount(), 2 );

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();
		assertEquals( result.size(), 1 );
		m = (Map) result.get(0);
		assertEquals(1, m.size());
		assertEquals( i.getDescription(), m.get( "desc" ) );
		t.commit();
		s.close();

		assertEquals( "hit count should go up since data is not transformed until after it is cachedr", qs.getCacheHitCount(), 3 );
		assertEquals( qs.getCacheMissCount(), 2 );
		assertEquals( qs.getCachePutCount(), 2 );

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		assertTrue( Hibernate.isInitialized( result.get(0) ) );
		i = (Item) s.get( Item.class, new Long(i.getId()) );
        i.setName("widget");
		i.setDescription("A middle-quality widget.");
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 4 );
		assertEquals( qs.getCacheMissCount(), 2 );
		assertEquals( qs.getCachePutCount(), 2 );

		Thread.sleep(200);

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		i = (Item) s.get( Item.class, new Long(i.getId()) );
		assertEquals( result.get(0), "A middle-quality widget." );

		assertEquals( qs.getCacheHitCount(), 4 );
		assertEquals( qs.getCacheMissCount(), 3 );
		assertEquals( qs.getCachePutCount(), 3 );

		s.delete(i);
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 4 );
		assertEquals( qs.getCacheMissCount(), 3 );
		assertEquals( qs.getCachePutCount(), 3 );
		assertEquals( qs.getExecutionCount(), 3 );
		assertEquals( es.getFetchCount(), 0 ); //check that it was being cached
	}

}


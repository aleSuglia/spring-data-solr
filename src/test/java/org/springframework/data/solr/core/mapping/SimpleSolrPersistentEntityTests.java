/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.mapping;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.solr.core.mapping.SimpleSolrPersistentPropertyTest.BeanWithScore;
import org.springframework.data.solr.repository.Score;
import org.springframework.data.util.TypeInformation;

/**
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SimpleSolrPersistentEntityTests {

	private static final String CORE_NAME = "core1";

	public @Rule ExpectedException expectedException = ExpectedException.none();

	@SuppressWarnings("rawtypes") //
	@Mock TypeInformation typeInfo;

	@Mock SolrPersistentProperty property;

	@SuppressWarnings("unchecked")
	@Test
	public void testPersistentEntityWithSolrDocumentAnnotation() {

		when(typeInfo.getType()).thenReturn(SearchableBeanWithSolrDocumentAnnotation.class);

		SimpleSolrPersistentEntity<SearchableBeanWithSolrDocumentAnnotation> pe = new SimpleSolrPersistentEntity<>(
				typeInfo);
		assertEquals(CORE_NAME, pe.getCollectionName());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPersistentEntityShouldReadSolrCoreNameFromParentClass() {

		when(typeInfo.getType()).thenReturn(InheritingClass.class);

		SimpleSolrPersistentEntity<InheritingClass> pe = new SimpleSolrPersistentEntity<>(typeInfo);
		assertEquals(CORE_NAME, pe.getCollectionName());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPersistentEntityWithoutSolrDocumentAnnotation() {

		when(typeInfo.getType()).thenReturn(SearchableBeanWithoutSolrDocumentAnnotation.class);

		SimpleSolrPersistentEntity<SearchableBeanWithoutSolrDocumentAnnotation> pe = new SimpleSolrPersistentEntity<>(
				typeInfo);
		assertEquals("searchablebeanwithoutsolrdocumentannotation", pe.getCollectionName());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPersistentEntityWithEmptySolrDocumentAnnotation() {

		when(typeInfo.getType()).thenReturn(SearchableBeanWithEmptySolrDocumentAnnotation.class);

		SimpleSolrPersistentEntity<SearchableBeanWithEmptySolrDocumentAnnotation> pe = new SimpleSolrPersistentEntity<>(
				typeInfo);
		assertEquals("searchablebeanwithemptysolrdocumentannotation", pe.getCollectionName());
	}

	@SuppressWarnings("unchecked")
	@Test // DATASOLR-88
	public void testPersistentEntityShouldReadDocumentBoostFromSolrDocumentAnnotation() {

		when(typeInfo.getType()).thenReturn(DocumentWithBoost.class);

		SimpleSolrPersistentEntity<DocumentWithBoost> pe = new SimpleSolrPersistentEntity<>(typeInfo);
		assertThat(pe.isBoosted(), is(true));
		assertThat(pe.getBoost(), is(100f));
	}

	@SuppressWarnings("unchecked")
	@Test // DATASOLR-88
	public void testPersistentEntityShouldNotBeBoostenWhenSolrDocumentAnnotationHasDefaultBoostValue() {

		when(typeInfo.getType()).thenReturn(SearchableBeanWithEmptySolrDocumentAnnotation.class);

		SimpleSolrPersistentEntity<SearchableBeanWithEmptySolrDocumentAnnotation> pe = new SimpleSolrPersistentEntity<>(
				typeInfo);
		assertThat(pe.isBoosted(), is(false));
		assertThat(pe.getBoost(), nullValue());
	}

	@SuppressWarnings("unchecked")
	@Test // DATASOLR-210
	public void testPersistentEntityWithScoreProperty() {

		when(typeInfo.getType()).thenReturn(BeanWithScore.class);
		when(property.isScoreProperty()).thenReturn(true);
		when(property.isAnnotationPresent(eq(Score.class))).thenReturn(true);
		when(property.getFieldName()).thenReturn("myScoreProperty");

		SimpleSolrPersistentEntity<BeanWithScore> entity = new SimpleSolrPersistentEntity<>(typeInfo);

		entity.addPersistentProperty(property);

		assertTrue(entity.hasScoreProperty());
		assertEquals(Optional.of(property), entity.getScoreProperty());
		assertEquals("myScoreProperty", entity.getScoreProperty().get().getFieldName());
	}

	@SuppressWarnings("unchecked")
	@Test // DATASOLR-210
	public void verifyShouldThrowExceptionWhenMoreThanOneScorePropertyDefined() {

		expectedException.expect(MappingException.class);
		expectedException.expectMessage("Ambiguous score field mapping detected!");

		when(typeInfo.getType()).thenReturn(DocumentWithScore.class);
		SimpleSolrPersistentEntity<DocumentWithScore> entity = new SimpleSolrPersistentEntity<>(typeInfo);

		SolrPersistentProperty score = mock(SolrPersistentProperty.class);
		SolrPersistentProperty anotherScore = mock(SolrPersistentProperty.class);

		when(score.isScoreProperty()).thenReturn(true);
		when(score.isAnnotationPresent(eq(Score.class))).thenReturn(true);
		when(score.getFieldName()).thenReturn("score");

		when(anotherScore.isScoreProperty()).thenReturn(true);
		when(anotherScore.isAnnotationPresent(eq(Score.class))).thenReturn(true);
		when(anotherScore.getFieldName()).thenReturn("anotherScore");

		entity.addPersistentProperty(score);
		entity.addPersistentProperty(anotherScore);

		entity.verify();
	}

	@SuppressWarnings("unchecked")
	@Test // DATASOLR-202
	public void verifyShouldThrowExceptionWhenDynamicDefinedForNonMapPropety() {

		expectedException.expect(MappingException.class);
		expectedException.expectMessage("'dynFieldName' with mapped name '*_s'");
		expectedException.expectMessage("Map");

		when(typeInfo.getType()).thenReturn(SearchableBeanWithoutSolrDocumentAnnotation.class);
		SimpleSolrPersistentEntity<DocumentWithScore> entity = new SimpleSolrPersistentEntity<>(typeInfo);

		SolrPersistentProperty property = mock(SolrPersistentProperty.class);

		when(property.isDynamicProperty()).thenReturn(true);
		when(property.isAnnotationPresent(eq(Dynamic.class))).thenReturn(true);
		when(property.getName()).thenReturn("dynFieldName");
		when(property.getFieldName()).thenReturn("*_s");
		when(property.containsWildcard()).thenReturn(true);
		when(property.isMap()).thenReturn(false);

		entity.addPersistentProperty(property);

		entity.verify();
	}

	@SuppressWarnings("unchecked")
	@Test // DATASOLR-202
	public void verifyShouldThrowExceptionWhenDynamicDefinedForNonWildcardPropety() {

		expectedException.expect(MappingException.class);
		expectedException.expectMessage("'dynFieldName' with mapped name 'someRandomFieldName'");
		expectedException.expectMessage("wildcard");

		when(typeInfo.getType()).thenReturn(SearchableBeanWithoutSolrDocumentAnnotation.class);
		SimpleSolrPersistentEntity<DocumentWithScore> entity = new SimpleSolrPersistentEntity<>(typeInfo);

		SolrPersistentProperty property = mock(SolrPersistentProperty.class);

		when(property.isDynamicProperty()).thenReturn(true);
		when(property.isAnnotationPresent(eq(Dynamic.class))).thenReturn(true);
		when(property.getName()).thenReturn("dynFieldName");
		when(property.getFieldName()).thenReturn("someRandomFieldName");
		when(property.containsWildcard()).thenReturn(false);
		when(property.isMap()).thenReturn(true);

		entity.addPersistentProperty(property);

		entity.verify();
	}

	@SolrDocument(solrCoreName = CORE_NAME)
	static class SearchableBeanWithSolrDocumentAnnotation {}

	@SolrDocument
	static class SearchableBeanWithEmptySolrDocumentAnnotation {}

	static class SearchableBeanWithoutSolrDocumentAnnotation {}

	@SolrDocument(solrCoreName = CORE_NAME)
	static class ParentClassWithSolrDocumentAnnotation {}

	static class InheritingClass extends ParentClassWithSolrDocumentAnnotation {}

	@SolrDocument(boost = 100)
	static class DocumentWithBoost {}

	static class DocumentWithScore {}

}
